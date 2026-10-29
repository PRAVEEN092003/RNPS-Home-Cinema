package com.cineplex.app.ui.screens.ticket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.PaymentConfigDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinalTicketViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookingId: Int = savedStateHandle.get<Any>("bookingId")?.toString()?.toIntOrNull() ?: 0

    private val _bookingState = MutableStateFlow<UiState<BookingDto>>(UiState.Loading)
    val bookingState: StateFlow<UiState<BookingDto>> = _bookingState.asStateFlow()

    private val _paymentState = MutableStateFlow<UiState<BookingDto>?>(null)
    val paymentState: StateFlow<UiState<BookingDto>?> = _paymentState.asStateFlow()

    private val _paymentConfig = MutableStateFlow<PaymentConfigDto?>(null)
    val paymentConfig: StateFlow<PaymentConfigDto?> = _paymentConfig.asStateFlow()

    init {
        loadBooking()
        loadPaymentConfig()
    }

    fun loadPaymentConfig() {
        viewModelScope.launch {
            val res = movieRepository.getPaymentConfig()
            if (res is UiState.Success) {
                _paymentConfig.value = res.data
            }
        }
    }

    fun loadBooking(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _bookingState.value !is UiState.Success) {
                _bookingState.value = UiState.Loading
            }
            val res = movieRepository.getBooking(bookingId)
            if (res is UiState.Success) {
                _bookingState.value = res
            } else if (_bookingState.value !is UiState.Success) {
                val allRes = movieRepository.getMyBookings()
                if (allRes is UiState.Success) {
                    val match = allRes.data.find { it.id == bookingId }
                    if (match != null) {
                        _bookingState.value = UiState.Success(match)
                    } else {
                        _bookingState.value = UiState.Error("Booking details not found")
                    }
                } else if (allRes is UiState.Error) {
                    _bookingState.value = UiState.Error(allRes.message)
                } else if (allRes is UiState.Offline) {
                    _bookingState.value = UiState.Offline
                }
            }
        }
    }

    fun submitPayment(transactionRef: String? = null) {
        if (_paymentState.value is UiState.Loading) return // Prevent duplicate payment submissions
        _paymentState.value = UiState.Loading
        viewModelScope.launch {
            val res = movieRepository.submitPayment(bookingId, transactionRef)
            if (res is UiState.Success) {
                _bookingState.value = UiState.Success(res.data)
            }
            _paymentState.value = res
        }
    }

    fun resetPaymentState() {
        _paymentState.value = null
    }

    fun cancelBookingAndFinish(onFinish: () -> Unit) {
        val isPaidOrSubmitted = (bookingState.value as? UiState.Success)?.data?.let { b ->
            b.payment != null && (b.payment.status == "SUBMITTED" || b.payment.status == "PAID" || b.payment.status == "COMPLETED")
        } ?: false
        if (isPaidOrSubmitted) {
            onFinish()
            return
        }
        viewModelScope.launch {
            try {
                movieRepository.cancelBooking(bookingId)
            } catch (_: Exception) {}
            onFinish()
        }
    }
}

