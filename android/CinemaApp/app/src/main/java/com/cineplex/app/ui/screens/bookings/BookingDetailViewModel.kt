package com.cineplex.app.ui.screens.bookings

import androidx.lifecycle.SavedStateHandle
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
class BookingDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookingId: Int = checkNotNull(savedStateHandle["bookingId"]).toString().toInt()

    private val _bookingState = MutableStateFlow<UiState<BookingDto>>(UiState.Loading)
    val bookingState: StateFlow<UiState<BookingDto>> = _bookingState.asStateFlow()

    init {
        loadBookingDetail()
    }

    fun loadBookingDetail() {
        viewModelScope.launch {
            _bookingState.value = UiState.Loading
            val allRes = movieRepository.getMyBookings()
            if (allRes is UiState.Success) {
                val match = allRes.data.find { it.id == bookingId }
                if (match != null) {
                    _bookingState.value = UiState.Success(match)
                } else {
                    _bookingState.value = UiState.Error("Booking not found")
                }
            } else if (allRes is UiState.Error) {
                _bookingState.value = UiState.Error(allRes.message)
            } else if (allRes is UiState.Offline) {
                _bookingState.value = UiState.Offline
            }
        }
    }
}
