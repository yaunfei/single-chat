package com.yaunfei.singlechat.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.data.PreferencesRepository
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val PhonePattern = Regex("^1[3-9]\\d{9}$")

data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val countdown: Int = 0,
    val isLoggingIn: Boolean = false,
    val codeSent: Boolean = false,
    val error: String? = null,
) {
    val phoneValid: Boolean get() = PhonePattern.matches(phone)
    val canRequestCode: Boolean get() = phoneValid && countdown == 0 && !isLoggingIn
    val canLogin: Boolean get() = phoneValid && code.length == 6 && !isLoggingIn
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun onPhoneChange(value: String) {
        val digits = value.filter(Char::isDigit).take(11)
        _state.update { it.copy(phone = digits, error = null) }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        _state.update { it.copy(code = digits, error = null) }
    }

    fun requestCode() {
        val current = _state.value
        if (!current.phoneValid) {
            _state.update { it.copy(error = "请输入 11 位手机号") }
            return
        }
        if (current.countdown > 0) return

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _state.update { it.copy(countdown = 60, codeSent = true, error = null) }
            while (_state.value.countdown > 0) {
                delay(1_000)
                _state.update { it.copy(countdown = (it.countdown - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun login() {
        val current = _state.value
        if (!current.phoneValid) {
            _state.update { it.copy(error = "请输入 11 位手机号") }
            return
        }
        if (current.code != DEMO_CODE) {
            _state.update { it.copy(error = "验证码不正确，演示码是 123456") }
            return
        }
        if (current.isLoggingIn) return

        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, error = null) }
            delay(550)
            preferences.saveSession(
                Session(
                    userId = LocalUser.ID,
                    displayName = LocalUser.NAME,
                    phone = current.phone,
                ),
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private companion object {
        const val DEMO_CODE = "123456"
    }
}
