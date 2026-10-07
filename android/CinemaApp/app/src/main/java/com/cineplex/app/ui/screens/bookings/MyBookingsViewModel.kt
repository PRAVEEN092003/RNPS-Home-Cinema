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

    private val _bookingsState = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Loading)
    val bookingsState: StateFlow<UiState<List<BookingDto>>> = _bookingsState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _bookingsState.value = UiState.Loading
            _bookingsState.value = movieRepository.getMyBookings()
        }
    }
}
