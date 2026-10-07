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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var nameState = MutableStateFlow("")
    var emailState = MutableStateFlow("")
    var phoneState = MutableStateFlow("")
    var passwordState = MutableStateFlow("")

    private val _registerState = MutableStateFlow<UiState<TokenResponse>?>(null)
    val registerState: StateFlow<UiState<TokenResponse>?> = _registerState.asStateFlow()

    fun register() {
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

        viewModelScope.launch {
            _registerState.value = UiState.Loading
            _registerState.value = authRepository.register(name, email, password, phone)
        }
    }
}
