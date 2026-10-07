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

    private val _verifyState = MutableStateFlow<UiState<TicketVerificationDto>?>(null)
    val verifyState: StateFlow<UiState<TicketVerificationDto>?> = _verifyState.asStateFlow()

    fun verifyTicket() {
        val code = codeInput.value.trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _verifyState.value = UiState.Loading
            _verifyState.value = movieRepository.verifyTicket(code)
        }
    }
}
