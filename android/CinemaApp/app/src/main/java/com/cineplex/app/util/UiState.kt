package com.cineplex.app.util

import retrofit2.Response

/**
 * Centralized sealed class for all UI states.
 * Every screen and ViewModel should use this type.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val errorCode: String? = null) : UiState<Nothing>()
    data object Offline : UiState<Nothing>()
}

/**
 * Wraps a Retrofit API call and maps it to [UiState].
 * Handles network errors, HTTP errors, and timeouts uniformly.
 */
suspend fun <T> safeApiCall(
    networkMonitor: NetworkMonitor,
    call: suspend () -> Response<T>,
): UiState<T> {
    if (!networkMonitor.isOnline()) {
        return UiState.Offline
    }

    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null) UiState.Empty
            else UiState.Success(body)
        } else {
            val errorMsg = when (response.code()) {
                400 -> "Invalid request. Please check your input."
                401 -> "Invalid email or password."
                403 -> "You don't have permission to do that."
                404 -> "The requested resource was not found."
                409 -> "An account with this email already exists."
                500 -> "Server error. Please try again later."
                else -> "Unexpected error (${response.code()})"
            }
            UiState.Error(errorMsg, response.code().toString())
        }
    } catch (e: java.net.SocketTimeoutException) {
        UiState.Error("Request timed out. Please try again.", "TIMEOUT")
    } catch (e: java.io.IOException) {
        UiState.Offline
    } catch (e: Exception) {
        UiState.Error(e.message ?: "An unexpected error occurred.", "UNKNOWN")
    }
}
