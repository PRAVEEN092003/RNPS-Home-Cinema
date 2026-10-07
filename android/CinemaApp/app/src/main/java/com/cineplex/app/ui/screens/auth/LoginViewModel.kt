package com.cineplex.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.TokenResponse
import com.cineplex.app.data.repository.AuthRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun login() {
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

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            _loginState.value = authRepository.login(email, password)
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
