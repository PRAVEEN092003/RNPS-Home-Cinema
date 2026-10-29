package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.TicketVerificationDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminTicketVerifyViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    var codeInput = MutableStateFlow("")

    private val _isCameraOpen = MutableStateFlow(false)
    val isCameraOpen: StateFlow<Boolean> = _isCameraOpen.asStateFlow()

    private val _verifyState = MutableStateFlow<UiState<TicketVerificationDto>?>(null)
    val verifyState: StateFlow<UiState<TicketVerificationDto>?> = _verifyState.asStateFlow()

    private val _checkInState = MutableStateFlow<UiState<TicketVerificationDto>?>(null)
    val checkInState: StateFlow<UiState<TicketVerificationDto>?> = _checkInState.asStateFlow()

    fun openCamera() {
        _isCameraOpen.value = true
    }

    fun closeCamera() {
        _isCameraOpen.value = false
    }

    fun onQrScanned(rawCode: String) {
        _isCameraOpen.value = false
        val cleanCode = rawCode.trim()
        codeInput.value = cleanCode
        verifyTicket(cleanCode)
    }

    fun verifyTicket(codeToVerify: String? = null) {
        val code = (codeToVerify ?: codeInput.value).trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _verifyState.value = UiState.Loading
            _checkInState.value = null
            val result = movieRepository.verifyTicket(code)
            _verifyState.value = result
        }
    }

    fun checkInTicket(ticketCode: String) {
        if (ticketCode.isBlank()) return
        viewModelScope.launch {
            _checkInState.value = UiState.Loading
            val result = movieRepository.checkInTicket(ticketCode)
            _checkInState.value = result
            if (result is UiState.Success) {
                // Update the verifyState with the new check-in data so UI reflects "ALREADY_USED" / checked in immediately
                _verifyState.value = UiState.Success(result.data)
            }
        }
    }

    fun resetVerification() {
        codeInput.value = ""
        _verifyState.value = null
        _checkInState.value = null
    }
}
