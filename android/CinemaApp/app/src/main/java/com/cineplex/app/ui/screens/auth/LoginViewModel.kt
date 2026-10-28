package com.cineplex.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.TokenResponse
import com.cineplex.app.data.repository.AuthRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var emailState = MutableStateFlow("")
    var passwordState = MutableStateFlow("")

    private val _loginState = MutableStateFlow<UiState<TokenResponse>?>(null)
    val loginState: StateFlow<UiState<TokenResponse>?> = _loginState.asStateFlow()

    private val _isServerWakingUp = MutableStateFlow(false)
    val isServerWakingUp: StateFlow<Boolean> = _isServerWakingUp.asStateFlow()

    val languageState: StateFlow<String> = authRepository.languageFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "en"
        )

    init {
        authRepository.warmUpServer()
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            authRepository.setLanguage(lang)
        }
    }

    fun login() {
        // Prevent duplicate login submissions while a request is in progress
        if (_loginState.value is UiState.Loading) return

        val email = emailState.value.trim()
        val password = passwordState.value.trim()

        if (email.isBlank()) {
            _loginState.value = UiState.Error("Please enter your email address.")
            return
        }
        if (password.isBlank()) {
            _loginState.value = UiState.Error("Please enter your password.")
            return
        }
        _loginState.value = UiState.Loading
        _isServerWakingUp.value = false

        viewModelScope.launch {
            // Start timer to detect cold start delay (> 4 seconds)
            val wakeUpJob: Job = launch {
                delay(4000)
                if (_loginState.value is UiState.Loading) {
                    _isServerWakingUp.value = true
                }
            }

            try {
                _loginState.value = authRepository.login(email, password)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "An unexpected error occurred.")
            } finally {
                wakeUpJob.cancel()
                _isServerWakingUp.value = false
            }
        }
    }

    fun resetState() {
        _loginState.value = null
        _isServerWakingUp.value = false
    }
}
