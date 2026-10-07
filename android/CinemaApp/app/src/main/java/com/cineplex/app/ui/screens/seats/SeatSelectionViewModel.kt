package com.cineplex.app.ui.screens.seats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.ShowDetailsWithSeatsDto
import com.cineplex.app.data.model.ShowSeatDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeatSelectionViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val showId: Int = checkNotNull(savedStateHandle["showId"]).toString().toInt()

    private val _seatsState = MutableStateFlow<UiState<ShowDetailsWithSeatsDto>>(UiState.Loading)
    val seatsState: StateFlow<UiState<ShowDetailsWithSeatsDto>> = _seatsState.asStateFlow()

    private val _selectedSeatIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedSeatIds: StateFlow<Set<Int>> = _selectedSeatIds.asStateFlow()

    private val _bookingState = MutableStateFlow<UiState<BookingDto>?>(null)
    val bookingState: StateFlow<UiState<BookingDto>?> = _bookingState.asStateFlow()

    init {
        loadSeats()
    }

    fun loadSeats() {
        viewModelScope.launch {
            _seatsState.value = UiState.Loading
            _seatsState.value = movieRepository.getShowSeats(showId)
        }
    }

    fun toggleSeatSelection(seat: ShowSeatDto) {
        if (seat.status == "BOOKED" || seat.status == "BLOCKED") return

        val current = _selectedSeatIds.value.toMutableSet()
        if (current.contains(seat.seatId)) {
            current.remove(seat.seatId)
        } else {
            current.add(seat.seatId)
        }
        _selectedSeatIds.value = current
    }

    fun calculateTotal(seats: List<ShowSeatDto>): Double {
        return seats.filter { _selectedSeatIds.value.contains(it.seatId) }.sumOf { it.price }
    }

    fun confirmBooking() {
        if (_bookingState.value is UiState.Loading) return // Prevent duplicate booking submissions
        val selectedIds = _selectedSeatIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _bookingState.value = UiState.Loading
            val res = movieRepository.createBooking(showId, selectedIds)
            _bookingState.value = res
        }
    }

    fun resetBookingState() {
        _bookingState.value = null
    }
}
