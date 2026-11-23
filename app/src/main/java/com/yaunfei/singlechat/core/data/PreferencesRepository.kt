package com.yaunfei.singlechat.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Session
import com.yaunfei.singlechat.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
        val ThemeMode = stringPreferencesKey("theme_mode")
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
        )
    }

    val themeMode: Flow<ThemeMode> = preferences.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.ThemeMode] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun saveSession(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.UserId] = session.userId
            prefs[Keys.DisplayName] = session.displayName
            prefs[Keys.Phone] = session.phone
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.UserId)
            prefs.remove(Keys.DisplayName)
            prefs.remove(Keys.Phone)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.ThemeMode] = mode.name }
    }
}
