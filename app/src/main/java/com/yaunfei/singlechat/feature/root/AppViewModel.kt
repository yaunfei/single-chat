package com.yaunfei.singlechat.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class AppUiState(
    val loading: Boolean = true,
    val session: Session? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        preferences.session,
        preferences.themeMode,
    ) { session, themeMode ->
        AppUiState(loading = false, session = session, themeMode = themeMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch { preferences.clearSession() }
    }
}
