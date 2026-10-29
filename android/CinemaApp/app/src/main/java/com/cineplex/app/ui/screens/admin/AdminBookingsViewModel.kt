package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBookingsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _bookingsState = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Loading)
    val bookingsState: StateFlow<UiState<List<BookingDto>>> = _bookingsState.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow("ALL")
    val selectedPaymentFilter: StateFlow<String> = _selectedPaymentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBookingDetail = MutableStateFlow<BookingDto?>(null)
    val selectedBookingDetail: StateFlow<BookingDto?> = _selectedBookingDetail.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadBookings()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce search queries
            loadBookings()
        }
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
        loadBookings()
    }

    fun setPaymentFilter(paymentStatus: String) {
        _selectedPaymentFilter.value = paymentStatus
        loadBookings()
    }

    fun selectBooking(booking: BookingDto?) {
        _selectedBookingDetail.value = booking
    }

    fun loadBookings() {
        viewModelScope.launch {
            _bookingsState.value = UiState.Loading
            val statusParam = _selectedStatusFilter.value.takeIf { it != "ALL" }
            val paymentParam = _selectedPaymentFilter.value.takeIf { it != "ALL" }
            val searchParam = _searchQuery.value.trim().takeIf { it.isNotEmpty() }

            val result = repository.getAdminBookings(
                statusFilter = statusParam,
                search = searchParam,
                paymentStatus = paymentParam,
            )
            _bookingsState.value = result
        }
    }

    fun refresh() {
        loadBookings()
    }
}
