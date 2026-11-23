package com.yaunfei.singlechat.core.network

import com.yaunfei.singlechat.BuildConfig
import com.yaunfei.singlechat.core.data.PreferencesRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackendUser(
    val id: String,
    val displayName: String,
    val phone: String,
)

data class SmsChallenge(
    val sent: Boolean,
    val cooldownSeconds: Int,
    val devCode: String?,
)

data class AuthSession(
    val token: String,
    val expiresIn: Long,
    val user: BackendUser,
)

data class UserSigTicket(
    val userId: String,
    val sdkAppId: Int,
    val userSig: String,
    val expireAt: Long,
)

class BackendException(
    message: String,
    val code: String? = null,
    val httpStatus: Int = 0,
) : IOException(message)

/**
 * 极简 BFF 客户端。
 *
 * SecretKey 只存在于服务端，客户端拿到的永远是短期 UserSig。
 */
@Singleton
class BackendClient @Inject constructor(
    private val preferences: PreferencesRepository,
) {

    suspend fun requestSms(phone: String): SmsChallenge = withContext(Dispatchers.IO) {
        val body = JSONObject().put("phone", phone)
        val json = post("/api/v1/auth/sms", body, token = null)
        SmsChallenge(
            sent = json.optBoolean("sent", true),
            cooldownSeconds = json.optInt("cooldownSeconds", 60),
            devCode = json.optString("devCode").takeIf { it.isNotBlank() },
        )
    }

    suspend fun login(phone: String, code: String): AuthSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("phone", phone)
            .put("code", code)
        val json = post("/api/v1/auth/login", body, token = null)
        AuthSession(
            token = json.optString("token"),
            expiresIn = json.optLong("expiresIn", 0L),
            user = parseUser(json.getJSONObject("user")),
        )
    }

    suspend fun fetchUserSig(token: String): UserSigTicket = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            throw BackendException("登录已过期，请重新登录", code = "UNAUTHORIZED", httpStatus = 401)
        }
        val json = get("/api/v1/im/usersig", token)
        UserSigTicket(
            userId = json.getString("userId"),
            sdkAppId = json.getInt("sdkAppId"),
            userSig = json.getString("userSig"),
            expireAt = json.optLong("expireAt", 0L),
        )
    }

    private fun parseUser(json: JSONObject): BackendUser = BackendUser(
        id = json.getString("id"),
        displayName = json.optString("displayName"),
        phone = json.optString("phone"),
    )

    private suspend fun post(path: String, body: JSONObject, token: String?): JSONObject =
        request(path, method = "POST", token = token, body = body)

    private suspend fun get(path: String, token: String?): JSONObject =
        request(path, method = "GET", token = token, body = null)

    /** 优先级：用户在设置里手填的地址 > GitHub 下发的地址 > 打包时内置的地址。 */
    suspend fun effectiveBaseUrl(): String =
        preferences.currentServerBaseUrl().ifBlank {
            preferences.currentRemoteBaseUrl().ifBlank { BuildConfig.BACKEND_BASE_URL }
        }.trimEnd('/')

    /**
     * 从 GitHub 拉取最新的 BFF 地址。
     *
     * 免费隧道域名会周期性失效，把地址托管在 GitHub 上以后，换隧道只需更新仓库里的
     * JSON，客户端连接失败时会自动回源，用户无需重装或改设置。
     */
    suspend fun refreshRemoteConfig(): Boolean = withContext(Dispatchers.IO) {
        // 优先用 commit SHA 读文件：SHA 是 immutable 的，不会被 CDN 缓存成旧内容；
        // 分支名则可能被缓存十几分钟。
        val sources = buildList {
            latestCommitSha()?.let {
                add("https://raw.githubusercontent.com/yaunfei/single-chat/$it/config/backend.json")
            }
            val stamp = System.currentTimeMillis()
            add("https://raw.githubusercontent.com/yaunfei/single-chat/main/config/backend.json?t=$stamp")
            add("https://cdn.jsdelivr.net/gh/yaunfei/single-chat@main/config/backend.json?t=$stamp")
        }
        for (url in sources) {
            val baseUrl = runCatching { fetchRemoteBaseUrl(url) }.getOrNull()
            // CDN 可能缓存了旧地址，必须探活通过才采用。
            if (!baseUrl.isNullOrBlank() && isReachable(baseUrl)) {
                preferences.saveRemoteBaseUrl(baseUrl)
                return@withContext true
            }
        }
        false
    }

    private fun latestCommitSha(): String? = runCatching {
        val connection =
            (URL("https://api.github.com/repos/yaunfei/single-chat/commits?sha=main&per_page=1")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONFIG_TIMEOUT_MS
                readTimeout = CONFIG_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
            }
        try {
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONArray(text).optJSONObject(0)?.optString("sha")?.takeIf { it.length >= 7 }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun isReachable(baseUrl: String): Boolean = runCatching {
        val connection = (URL("$baseUrl/healthz").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = HEALTH_TIMEOUT_MS
            readTimeout = HEALTH_TIMEOUT_MS
        }
        try {
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)

    private fun fetchRemoteBaseUrl(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONFIG_TIMEOUT_MS
            readTimeout = CONFIG_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val baseUrl = JSONObject(text).optString("baseUrl").trim().trimEnd('/')
            return baseUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun request(
        path: String,
        method: String,
        token: String?,
        body: JSONObject?,
        retried: Boolean = false,
    ): JSONObject {
        val baseUrl = effectiveBaseUrl()
        val url = URL(baseUrl + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                // 502/503/504 多为免费隧道地址过期（边缘节点还在，转发已失效）：
                // 回源 GitHub 拿新地址后重试一次。
                if (status in 502..504 && !retried && refreshRemoteConfig()) {
                    connection.disconnect()
                    return request(path, method, token, body, retried = true)
                }
                throw parseError(text, status)
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (error: BackendException) {
            throw error
        } catch (error: JSONException) {
            throw BackendException("服务器返回了无法解析的数据", code = "BAD_RESPONSE")
        } catch (error: IOException) {
            // 连接失败多半是隧道地址失效：回源 GitHub 拿新地址后重试一次。
            connection.disconnect()
            if (!retried && refreshRemoteConfig()) {
                return request(path, method, token, body, retried = true)
            }
            throw BackendException(
                message = "无法连接服务器（$baseUrl），请检查网络后重试",
                code = "NETWORK",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseError(body: String, status: Int): BackendException {
        val message = runCatching {
            JSONObject(body).getJSONObject("error").optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "请求失败（HTTP $status）"
        val code = runCatching {
            JSONObject(body).getJSONObject("error").optString("code")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return BackendException(message, code = code, httpStatus = status)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val CONFIG_TIMEOUT_MS = 8_000
        const val HEALTH_TIMEOUT_MS = 4_000
    }
}
