package com.cineplex.app.data.repository

import com.cineplex.app.data.api.CinemaApiService
import com.cineplex.app.data.model.LoginRequest
import com.cineplex.app.data.model.RegisterRequest
import com.cineplex.app.data.model.TokenResponse
import com.cineplex.app.data.model.UserDto
import com.cineplex.app.data.model.UserUpdateRequest
import com.cineplex.app.util.NetworkMonitor
import com.cineplex.app.util.SessionManager
import com.cineplex.app.util.UiState
import com.cineplex.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: CinemaApiService,
    private val session: SessionManager,
    private val network: NetworkMonitor,
) {
    suspend fun register(name: String, email: String, password: String, phone: String?): UiState<TokenResponse> {
        val state = safeApiCall(network) {
            api.register(RegisterRequest(name = name, email = email, password = password, phone = phone))
        }
        if (state is UiState.Success) {
            session.saveToken(state.data.accessToken)
            session.saveUser(state.data.user)
        }
        return state
    }

    suspend fun login(email: String, password: String): UiState<TokenResponse> {
        val state = safeApiCall(network) {
            api.login(LoginRequest(email = email, password = password))
        }
        if (state is UiState.Success) {
            session.saveToken(state.data.accessToken)
            session.saveUser(state.data.user)
        }
        return state
    }

    suspend fun getMe(): UiState<UserDto> =
        safeApiCall(network) { api.getMe() }

    suspend fun updateProfile(name: String? = null, phone: String? = null): UiState<UserDto> {
        val state = safeApiCall(network) {
            api.updateProfile(UserUpdateRequest(name = name, phone = phone))
        }
        if (state is UiState.Success) {
            session.saveUser(state.data)
        }
        return state
    }

    suspend fun logout() {
        session.clearSession()
    }

    suspend fun isLoggedIn(): Boolean = session.isLoggedIn()

    suspend fun getCachedUser(): UserDto? = session.getUser()
}
