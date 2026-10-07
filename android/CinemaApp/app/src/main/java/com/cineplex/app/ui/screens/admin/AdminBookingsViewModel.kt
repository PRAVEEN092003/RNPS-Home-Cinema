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
class AdminBookingsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _bookingsState = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Loading)
    val bookingsState: StateFlow<UiState<List<BookingDto>>> = _bookingsState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    init {
        loadBookings("ALL")
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        loadBookings(filter)
    }

    fun loadBookings(filter: String = _selectedFilter.value) {
        viewModelScope.launch {
            _bookingsState.value = UiState.Loading
            val filterParam = if (filter == "ALL") null else filter
            _bookingsState.value = repository.getAdminBookings(filterParam)
        }
    }
}
