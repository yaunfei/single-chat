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

    /** 优先使用登录页/设置页保存的地址，方便手机调试时切换隧道。 */
    suspend fun effectiveBaseUrl(): String =
        preferences.currentServerBaseUrl().ifBlank { BuildConfig.BACKEND_BASE_URL }.trimEnd('/')

    private suspend fun request(
        path: String,
        method: String,
        token: String?,
        body: JSONObject?,
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
                throw parseError(text, status)
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (error: BackendException) {
            throw error
        } catch (error: JSONException) {
            throw BackendException("服务器返回了无法解析的数据", code = "BAD_RESPONSE")
        } catch (error: IOException) {
            throw BackendException(
                message = "无法连接服务器（$baseUrl），请在登录页「服务器设置」中检查或修改地址",
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
    }
}
