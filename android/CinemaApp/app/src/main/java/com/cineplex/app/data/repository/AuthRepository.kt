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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.cineplex.app.data.firebase.FirebaseDataSource
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AuthRepository protected constructor(
    private val api: CinemaApiService?,
    private val session: SessionManager?,
    private val network: NetworkMonitor?,
    private val firebase: FirebaseDataSource?,
    @Suppress("UNUSED_PARAMETER") dummy: Unit,
) {
    @Inject
    constructor(
        api: CinemaApiService,
        session: SessionManager,
        network: NetworkMonitor,
        firebase: FirebaseDataSource,
    ) : this(api, session, network, firebase, Unit)

    // Secondary constructor for testing
    constructor() : this(null, null, null, null, Unit)

    open suspend fun register(name: String, email: String, password: String, phone: String?): UiState<TokenResponse> {
        val net = network ?: return UiState.Error("Network uninitialized")
        // Integrate Firebase Authentication
        if (firebase != null) {
            try {
                firebase.register(name = name, email = email, password = password, phone = phone)
            } catch (_: Exception) {
                // Non-fatal if user exists in Firebase Auth or network timeout; FastAPI remains authoritative
            }
        }
        val state = safeApiCall(net) {
            api!!.register(RegisterRequest(name = name, email = email, password = password, phone = phone))
        }
        if (state is UiState.Success) {
            session?.saveToken(state.data.accessToken)
            session?.saveUser(state.data.user)
        }
        return state
    }

    open suspend fun login(email: String, password: String): UiState<TokenResponse> {
        val net = network ?: return UiState.Error("Network uninitialized")
        // Integrate Firebase Authentication
        if (firebase != null) {
            try {
                firebase.login(email = email, password = password)
            } catch (_: Exception) {
                // Non-fatal for existing users awaiting Firebase sync; proceed with FastAPI
            }
        }
        val state = safeApiCall(net) {
            api!!.login(LoginRequest(email = email, password = password))
        }
        if (state is UiState.Success) {
            session?.saveToken(state.data.accessToken)
            session?.saveUser(state.data.user)
        }
        return state
    }

    open suspend fun getMe(): UiState<UserDto> {
        val net = network ?: return UiState.Error("Network uninitialized")
        val state = safeApiCall(net) { api!!.getMe() }
        if (state is UiState.Success) {
            return state
        }
        // Fallback to Firebase getMe if available
        if (firebase != null) {
            try {
                val fUser = firebase.getMe()
                return UiState.Success(fUser)
            } catch (_: Exception) {}
        }
        return state
    }

    open suspend fun updateProfile(name: String? = null, phone: String? = null): UiState<UserDto> {
        val net = network ?: return UiState.Error("Network uninitialized")
        val state = safeApiCall(net) {
            api!!.updateProfile(UserUpdateRequest(name = name, phone = phone))
        }
        if (state is UiState.Success) {
            session?.saveUser(state.data)
        }
        return state
    }

    open val languageFlow: kotlinx.coroutines.flow.Flow<String> =
        session?.languageFlow ?: kotlinx.coroutines.flow.flowOf("en")

    open suspend fun getLanguage(): String = session?.getLanguage() ?: "en"

    open suspend fun setLanguage(lang: String) {
        session?.setLanguage(lang)
    }

    open suspend fun logout() {
        try {
            firebase?.logout()
        } catch (_: Exception) {}
        session?.clearSession()
    }

    open suspend fun isLoggedIn(): Boolean = session?.isLoggedIn() ?: false

    open suspend fun getCachedUser(): UserDto? = session?.getUser()

    /**
     * Sends an asynchronous background ping to wake up the server (e.g. Render cold start).
     * Non-blocking and silent.
     */
    open fun warmUpServer() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                api?.health()
            } catch (_: Exception) {
                // Background warm-up ping; errors are safely ignored
            }
        }
    }
}

