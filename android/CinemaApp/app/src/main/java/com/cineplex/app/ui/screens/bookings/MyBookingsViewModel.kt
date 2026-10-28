package com.cineplex.app.ui.screens.bookings

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
class MyBookingsViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: All, 1: Pending, 2: Confirmed, 3: Cancelled
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _bookingsState = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Loading)
    val bookingsState: StateFlow<UiState<List<BookingDto>>> = _bookingsState.asStateFlow()

    init {
        loadBookings()
    }

    fun selectTab(tabIndex: Int) {
        if (_selectedTab.value == tabIndex && _bookingsState.value is UiState.Success) return
        _selectedTab.value = tabIndex
        loadBookings()
    }

    fun loadBookings() {
        val status = when (_selectedTab.value) {
            1 -> "PENDING"
            2 -> "CONFIRMED"
            3 -> "CANCELLED"
            else -> null
        }
        viewModelScope.launch {
            _bookingsState.value = UiState.Loading
            _bookingsState.value = movieRepository.getMyBookings(status)
        }
    }
}

