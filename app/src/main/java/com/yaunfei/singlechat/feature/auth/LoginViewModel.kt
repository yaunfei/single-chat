package com.yaunfei.singlechat.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.data.PreferencesRepository
import com.yaunfei.singlechat.core.model.Session
import com.yaunfei.singlechat.core.network.BackendClient
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
    val isRequestingCode: Boolean = false,
    val isLoggingIn: Boolean = false,
    val codeSent: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val phoneValid: Boolean get() = PhonePattern.matches(phone)
    val canRequestCode: Boolean
        get() = phoneValid && countdown == 0 && !isLoggingIn && !isRequestingCode
    val canLogin: Boolean get() = phoneValid && code.length == 6 && !isLoggingIn
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val backend: BackendClient,
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
        if (current.countdown > 0 || current.isRequestingCode) return

        viewModelScope.launch {
            _state.update { it.copy(isRequestingCode = true, error = null) }
            runCatching { backend.requestSms(current.phone) }
                .onSuccess { challenge ->
                    val devCode = challenge.devCode
                    _state.update {
                        it.copy(
                            isRequestingCode = false,
                            codeSent = true,
                            code = devCode ?: it.code,
                            countdown = challenge.cooldownSeconds.coerceAtLeast(1),
                            notice = if (devCode != null) {
                                "开发态验证码 $devCode 已自动填入"
                            } else {
                                "验证码已发送"
                            },
                        )
                    }
                    startCountdown(_state.value.countdown)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isRequestingCode = false,
                            error = error.message ?: "验证码发送失败，请稍后再试",
                        )
                    }
                }
        }
    }

    fun login() {
        val current = _state.value
        if (!current.phoneValid) {
            _state.update { it.copy(error = "请输入 11 位手机号") }
            return
        }
        if (current.code.length != 6) {
            _state.update { it.copy(error = "请输入 6 位验证码") }
            return
        }
        if (current.isLoggingIn) return

        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, error = null) }
            runCatching { backend.login(current.phone, current.code) }
                .onSuccess { auth ->
                    preferences.saveSession(
                        Session(
                            userId = auth.user.id,
                            displayName = auth.user.displayName.ifBlank { "用户" + current.phone.takeLast(4) },
                            phone = auth.user.phone.ifBlank { current.phone },
                            token = auth.token,
                        ),
                    )
                    // 保留 isLoggingIn，让 MainActivity 切到主界面前一直显示加载态。
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoggingIn = false,
                            error = error.message ?: "登录失败，请稍后再试",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.countdown > 0) {
                delay(1_000)
                _state.update { it.copy(countdown = (it.countdown - 1).coerceAtLeast(0)) }
            }
        }
    }
}
