package com.cineplex.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {

    @Test
    fun parseErrorMessage_withStringDetail_returnsDetail() {
        val json = """{"detail":"Incorrect email or password"}"""
        val message = parseErrorMessage(json, 401)
        assertEquals("Incorrect email or password", message)
    }

    @Test
    fun parseErrorMessage_withDuplicateAccountDetail_returnsDetail() {
        val json = """{"detail":"An account with this email already exists"}"""
        val message = parseErrorMessage(json, 409)
        assertEquals("An account with this email already exists", message)
    }

    @Test
    fun parseErrorMessage_withValidationArray_extractsFirstErrorMessage() {
        val json = """{"detail":[{"type":"string_too_short","loc":["body","password"],"msg":"String should have at least 6 characters"}]}"""
        val message = parseErrorMessage(json, 422)
        assertEquals("String should have at least 6 characters", message)
    }

    @Test
    fun parseErrorMessage_withNullOrEmptyBody_usesStatusFallback() {
        assertEquals("Incorrect email or password.", parseErrorMessage(null, 401))
        assertEquals("An account with this email already exists.", parseErrorMessage("", 409))
        assertEquals("Validation error. Please verify your input.", parseErrorMessage(null, 422))
        assertEquals("Server error. Please try again later.", parseErrorMessage(null, 500))
    }

    @Test
    fun parseErrorMessage_withColdStartGatewayErrors_returnsServerWakingUpMessage() {
        val msg502 = parseErrorMessage(null, 502)
        val msg503 = parseErrorMessage(null, 503)
        val msg504 = parseErrorMessage(null, 504)

        assertTrue(msg502.contains("waking up", ignoreCase = true) || msg502.contains("temporarily unavailable", ignoreCase = true))
        assertTrue(msg503.contains("waking up", ignoreCase = true) || msg503.contains("temporarily unavailable", ignoreCase = true))
        assertTrue(msg504.contains("waking up", ignoreCase = true) || msg504.contains("temporarily unavailable", ignoreCase = true))
    }

    @Test
    fun uiState_typesVerify() {
        val success = UiState.Success("token_123")
        assertEquals("token_123", success.data)

        val error = UiState.Error("Timeout occurred", "TIMEOUT")
        assertEquals("Timeout occurred", error.message)
        assertEquals("TIMEOUT", error.errorCode)

        assertTrue(UiState.Loading is UiState<*>)
        assertTrue(UiState.Offline is UiState<*>)
        assertTrue(UiState.Empty is UiState<*>)
    }
}
