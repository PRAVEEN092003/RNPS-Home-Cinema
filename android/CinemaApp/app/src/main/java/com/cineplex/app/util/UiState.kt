package com.cineplex.app.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
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

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Parses FastAPI error details from the response error body.
 * Supports string details {"detail": "..."} as well as validation error arrays {"detail": [{"msg": "..."}]}.
 */
fun parseErrorMessage(rawBody: String?, statusCode: Int): String {
    if (!rawBody.isNullOrBlank()) {
        try {
            val jsonElement = errorJson.parseToJsonElement(rawBody)
            if (jsonElement is JsonObject) {
                val detail = jsonElement["detail"]
                if (detail != null) {
                    if (detail is JsonPrimitive) {
                        val str = detail.content
                        if (str.isNotBlank()) return str
                    } else if (detail is JsonArray && detail.isNotEmpty()) {
                        val firstErr = detail[0]
                        if (firstErr is JsonObject) {
                            val msg = firstErr["msg"]?.jsonPrimitive?.content
                            if (!msg.isNullOrBlank()) return msg
                        }
                    }
                }
                val msg = jsonElement["message"]?.jsonPrimitive?.content
                if (!msg.isNullOrBlank()) return msg
            }
        } catch (_: Exception) {
            // Fall back to HTTP status code mappings below
        }
    }

    return when (statusCode) {
        400 -> "Invalid request. Please check your input."
        401 -> "Incorrect email or password."
        403 -> "You don't have permission to perform this action."
        404 -> "The requested resource was not found."
        409 -> "An account with this email already exists."
        422 -> "Validation error. Please verify your input."
        500 -> "Server error. Please try again later."
        502, 503, 504 -> "Server is currently waking up or temporarily unavailable. Please try again in a moment."
        else -> "Unexpected error ($statusCode)"
    }
}

/**
 * Wraps a Retrofit API call and maps it to [UiState].
 * Handles network errors, HTTP errors, and timeouts uniformly.
 */
suspend fun <T> safeApiCall(
    networkMonitor: NetworkChecker,
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
            val rawError = try { response.errorBody()?.string() } catch (_: Exception) { null }
            val errorMsg = parseErrorMessage(rawError, response.code())
            UiState.Error(errorMsg, response.code().toString())
        }
    } catch (e: java.net.SocketTimeoutException) {
        UiState.Error(
            "Request timed out. The server took too long to respond. If the server was sleeping, please try once more.",
            "TIMEOUT"
        )
    } catch (e: java.io.IOException) {
        if (!networkMonitor.isOnline()) {
            UiState.Offline
        } else {
            UiState.Error(
                "Unable to connect to server. Please check your connection and try again.",
                "NETWORK_ERROR"
            )
        }
    } catch (e: Exception) {
        UiState.Error(e.message ?: "An unexpected error occurred.", "UNKNOWN")
    }
}
