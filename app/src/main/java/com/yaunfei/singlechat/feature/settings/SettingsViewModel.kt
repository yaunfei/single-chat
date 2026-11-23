package com.yaunfei.singlechat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.BuildConfig
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.data.PreferencesRepository
import com.yaunfei.singlechat.core.model.Session
import com.yaunfei.singlechat.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val session: Session? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val serverBaseUrl: String = BuildConfig.BACKEND_BASE_URL,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val engine: ChatEngine,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.session,
        preferences.themeMode,
        preferences.serverBaseUrl,
    ) { session, themeMode, serverBaseUrl ->
        SettingsUiState(
            session = session,
            themeMode = themeMode,
            serverBaseUrl = serverBaseUrl.ifBlank { BuildConfig.BACKEND_BASE_URL },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun saveServerBaseUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        if (normalized.isNotBlank() &&
            !normalized.startsWith("http://") &&
            !normalized.startsWith("https://")
        ) {
            return
        }
        viewModelScope.launch { preferences.saveServerBaseUrl(normalized) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { engine.signOut() }
            preferences.clearSession()
        }
    }
}
