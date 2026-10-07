package com.cineplex.app.ui.screens.snacks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.SnackDto
import com.cineplex.app.data.model.SnackItemRequest
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnacksViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookingId: Int = checkNotNull(savedStateHandle["bookingId"]).toString().toInt()

    private val _snacksState = MutableStateFlow<UiState<List<SnackDto>>>(UiState.Loading)
    val snacksState: StateFlow<UiState<List<SnackDto>>> = _snacksState.asStateFlow()

    // Map of snackId -> quantity selected
    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quantities: StateFlow<Map<Int, Int>> = _quantities.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<BookingDto>?>(null)
    val saveState: StateFlow<UiState<BookingDto>?> = _saveState.asStateFlow()

    init {
        loadSnacks()
    }

    fun loadSnacks() {
        viewModelScope.launch {
            _snacksState.value = UiState.Loading
            _snacksState.value = movieRepository.getSnacks()
        }
    }

    fun incrementQuantity(snackId: Int) {
        val current = _quantities.value.toMutableMap()
        val qty = current.getOrDefault(snackId, 0)
        current[snackId] = qty + 1
        _quantities.value = current
    }

    fun decrementQuantity(snackId: Int) {
        val current = _quantities.value.toMutableMap()
        val qty = current.getOrDefault(snackId, 0)
        if (qty > 1) {
            current[snackId] = qty - 1
        } else {
            current.remove(snackId)
        }
        _quantities.value = current
    }

    fun calculateTotal(snacks: List<SnackDto>): Double {
        val map = _quantities.value
        return snacks.sumOf { snack -> (map[snack.id] ?: 0) * snack.price }
    }

    fun getTotalItemsCount(): Int = _quantities.value.values.sum()

    fun submitSnacks() {
        if (_saveState.value is UiState.Loading) return // Prevent duplicate requests
        val items = _quantities.value.filter { it.value > 0 }.map {
            SnackItemRequest(snackId = it.key, quantity = it.value)
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            val res = movieRepository.addSnacksToBooking(bookingId, items)
            _saveState.value = res
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }
}
