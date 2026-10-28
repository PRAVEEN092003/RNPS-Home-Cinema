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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var nameState = MutableStateFlow("")
    var emailState = MutableStateFlow("")
    var phoneState = MutableStateFlow("")
    var passwordState = MutableStateFlow("")

    private val _registerState = MutableStateFlow<UiState<TokenResponse>?>(null)
    val registerState: StateFlow<UiState<TokenResponse>?> = _registerState.asStateFlow()

    private val _isServerWakingUp = MutableStateFlow(false)
    val isServerWakingUp: StateFlow<Boolean> = _isServerWakingUp.asStateFlow()

    init {
        authRepository.warmUpServer()
    }

    fun register() {
        // Prevent duplicate registration submissions while a request is running
        if (_registerState.value is UiState.Loading) return

        val name = nameState.value.trim()
        val email = emailState.value.trim()
        val phone = phoneState.value.trim().ifEmpty { null }
        val password = passwordState.value.trim()

        if (name.isBlank()) {
            _registerState.value = UiState.Error("Please enter your name.")
            return
        }
        if (email.isBlank()) {
            _registerState.value = UiState.Error("Please enter your email.")
            return
        }
        if (password.length < 6) {
            _registerState.value = UiState.Error("Password must be at least 6 characters.")
            return
        }
        _registerState.value = UiState.Loading
        _isServerWakingUp.value = false

        viewModelScope.launch {
            // Start timer to detect cold start delay (> 4 seconds)
            val wakeUpJob: Job = launch {
                delay(4000)
                if (_registerState.value is UiState.Loading) {
                    _isServerWakingUp.value = true
                }
            }

            try {
                _registerState.value = authRepository.register(name, email, password, phone)
            } catch (e: Exception) {
                _registerState.value = UiState.Error(e.message ?: "An unexpected error occurred.")
            } finally {
                wakeUpJob.cancel()
                _isServerWakingUp.value = false
            }
        }
    }

    fun resetState() {
        _registerState.value = null
        _isServerWakingUp.value = false
    }
}
