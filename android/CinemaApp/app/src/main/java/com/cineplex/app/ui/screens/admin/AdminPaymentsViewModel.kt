package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPaymentsViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _paymentsState = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Loading)
    val paymentsState: StateFlow<UiState<List<BookingDto>>> = _paymentsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<BookingDto>?>(null)
    val actionState: StateFlow<UiState<BookingDto>?> = _actionState.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _paymentsState.value !is UiState.Success) {
                _paymentsState.value = UiState.Loading
            }
            _paymentsState.value = movieRepository.getAdminPayments()
        }
    }

    fun verifyPayment(paymentId: Int, approve: Boolean) {
        val previousState = _paymentsState.value
        val currentList = (previousState as? UiState.Success)?.data ?: emptyList()
        val targetBooking = currentList.find { it.payment?.id == paymentId }

        // Optimistic UI update: update booking and payment status immediately in list
        if (targetBooking != null) {
            val updatedPayment = targetBooking.payment?.copy(
                status = if (approve) "PAID" else "FAILED"
            )
            val updatedBooking = targetBooking.copy(
                status = if (approve) "CONFIRMED" else "CANCELLED",
                payment = updatedPayment
            )
            val updatedList = currentList.map {
                if (it.payment?.id == paymentId) updatedBooking else it
            }
            _paymentsState.value = UiState.Success(updatedList)
        }

        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val res = movieRepository.verifyPayment(paymentId, approve)
            _actionState.value = res
            if (res is UiState.Error) {
                // Rollback on failure
                _paymentsState.value = previousState
            } else {
                loadPayments(showLoading = false) // refresh list silently with server-authoritative data
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }
}
