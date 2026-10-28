package com.cineplex.app.util

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class SafeApiCallTest {

    private class FakeNetworkChecker(private val online: Boolean) : NetworkChecker {
        override fun isOnline(): Boolean = online
    }

    @Test
    fun safeApiCall_whenOffline_returnsOfflineState() = runBlocking {
        val monitor = FakeNetworkChecker(online = false)
        val result = safeApiCall(monitor) {
            Response.success("data")
        }
        assertTrue(result is UiState.Offline)
    }

    @Test
    fun safeApiCall_whenSuccessful_returnsSuccessState() = runBlocking {
        val monitor = FakeNetworkChecker(online = true)
        val result = safeApiCall(monitor) {
            Response.success("auth_token_sample")
        }
        assertTrue(result is UiState.Success)
        assertEquals("auth_token_sample", (result as UiState.Success).data)
    }

    @Test
    fun safeApiCall_whenHttp401_returnsHandledError() = runBlocking {
        val monitor = FakeNetworkChecker(online = true)
        val errorJson = """{"detail":"Incorrect email or password"}"""
        val response = Response.error<String>(
            401,
            errorJson.toResponseBody("application/json".toMediaType())
        )
        val result = safeApiCall(monitor) { response }
        assertTrue(result is UiState.Error)
        val err = result as UiState.Error
        assertEquals("Incorrect email or password", err.message)
        assertEquals("401", err.errorCode)
    }

    @Test
    fun safeApiCall_whenHttp409Duplicate_returnsHandledError() = runBlocking {
        val monitor = FakeNetworkChecker(online = true)
        val errorJson = """{"detail":"An account with this email already exists"}"""
        val response = Response.error<String>(
            409,
            errorJson.toResponseBody("application/json".toMediaType())
        )
        val result = safeApiCall(monitor) { response }
        assertTrue(result is UiState.Error)
        val err = result as UiState.Error
        assertEquals("An account with this email already exists", err.message)
        assertEquals("409", err.errorCode)
    }

    @Test
    fun safeApiCall_whenSocketTimeout_returnsTimeoutError() = runBlocking {
        val monitor = FakeNetworkChecker(online = true)
        val result = safeApiCall<String>(monitor) {
            throw SocketTimeoutException("connect timed out")
        }
        assertTrue(result is UiState.Error)
        val err = result as UiState.Error
        assertEquals("TIMEOUT", err.errorCode)
        assertTrue(err.message.contains("timed out", ignoreCase = true))
    }

    @Test
    fun safeApiCall_whenIOExceptionWhileOnline_returnsNetworkErrorNotOffline() = runBlocking {
        val monitor = FakeNetworkChecker(online = true)
        val result = safeApiCall<String>(monitor) {
            throw IOException("Connection reset by peer")
        }
        assertTrue(result is UiState.Error)
        val err = result as UiState.Error
        assertEquals("NETWORK_ERROR", err.errorCode)
    }
}
