package com.yaunfei.singlechat.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Session
import com.yaunfei.singlechat.core.model.ThemeMode
import com.yaunfei.singlechat.core.network.UserSigTicket
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.singleChatDataStore by preferencesDataStore(name = "single_chat_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private object Keys {
        val UserId = stringPreferencesKey("user_id")
        val DisplayName = stringPreferencesKey("display_name")
        val Phone = stringPreferencesKey("phone")
        val AuthToken = stringPreferencesKey("auth_token")
        val ImUserId = stringPreferencesKey("im_user_id")
        val ImUserSig = stringPreferencesKey("im_user_sig")
        val ImSigSdkAppId = intPreferencesKey("im_sig_sdk_app_id")
        val ImSigExpireAt = longPreferencesKey("im_sig_expire_at")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val ServerBaseUrl = stringPreferencesKey("server_base_url")
        val RemoteBaseUrl = stringPreferencesKey("remote_base_url")
    }

    private val dataStore = context.singleChatDataStore

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val session: Flow<Session?> = preferences.map { prefs ->
        val userId = prefs[Keys.UserId] ?: return@map null
        Session(
            userId = userId,
            displayName = prefs[Keys.DisplayName] ?: LocalUser.NAME,
            phone = prefs[Keys.Phone].orEmpty(),
            token = prefs[Keys.AuthToken].orEmpty(),
        )
    }

    val imUserId: Flow<String?> = preferences.map { it[Keys.ImUserId] }

    val themeMode: Flow<ThemeMode> = preferences.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.ThemeMode] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    /** 用户自定义的 BFF 地址；为空表示使用 BuildConfig 中的默认值。 */
    val serverBaseUrl: Flow<String> = preferences.map { it[Keys.ServerBaseUrl].orEmpty() }

    suspend fun currentSession(): Session? = session.first()

    suspend fun currentServerBaseUrl(): String = serverBaseUrl.first()

    /** 从 GitHub 拉取到的最新 BFF 地址（隧道轮换后自动更新）。 */
    suspend fun currentRemoteBaseUrl(): String =
        preferences.first()[Keys.RemoteBaseUrl].orEmpty()

    suspend fun saveRemoteBaseUrl(url: String) {
        dataStore.edit { prefs -> prefs[Keys.RemoteBaseUrl] = url.trim().trimEnd('/') }
    }

    suspend fun currentImUserId(): String? = imUserId.first()

    suspend fun saveSession(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.UserId] = session.userId
            prefs[Keys.DisplayName] = session.displayName
            prefs[Keys.Phone] = session.phone
            prefs[Keys.AuthToken] = session.token
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.UserId)
            prefs.remove(Keys.DisplayName)
            prefs.remove(Keys.Phone)
            prefs.remove(Keys.AuthToken)
        }
    }

    suspend fun saveImUserId(userId: String) {
        dataStore.edit { prefs -> prefs[Keys.ImUserId] = userId }
    }

    /**
     * 缓存最近一次下发的 UserSig（服务端签发 7 天有效期）。
     * BFF 临时不可达时，客户端可凭它直接登录 IM（IM 长连接不经过 BFF）。
     */
    suspend fun currentImTicket(): UserSigTicket? = preferences.first().let { prefs ->
        val sig = prefs[Keys.ImUserSig] ?: return null
        val userId = prefs[Keys.ImUserId] ?: return null
        UserSigTicket(
            userId = userId,
            sdkAppId = prefs[Keys.ImSigSdkAppId] ?: 0,
            userSig = sig,
            expireAt = prefs[Keys.ImSigExpireAt] ?: 0L,
        )
    }

    suspend fun saveImTicket(ticket: UserSigTicket) {
        dataStore.edit { prefs ->
            prefs[Keys.ImUserSig] = ticket.userSig
            prefs[Keys.ImSigSdkAppId] = ticket.sdkAppId
            prefs[Keys.ImSigExpireAt] = ticket.expireAt
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.ThemeMode] = mode.name }
    }

    suspend fun saveServerBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        dataStore.edit { prefs ->
            if (normalized.isBlank()) {
                prefs.remove(Keys.ServerBaseUrl)
            } else {
                prefs[Keys.ServerBaseUrl] = normalized
            }
        }
    }
}
