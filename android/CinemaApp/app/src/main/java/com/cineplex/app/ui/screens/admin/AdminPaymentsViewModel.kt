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

    fun loadPayments() {
        viewModelScope.launch {
            _paymentsState.value = UiState.Loading
            _paymentsState.value = movieRepository.getAdminPayments()
        }
    }

    fun verifyPayment(paymentId: Int, approve: Boolean) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val res = movieRepository.verifyPayment(paymentId, approve)
            _actionState.value = res
            loadPayments() // refresh list
        }
    }
}
