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

data class SelectedSnackItem(
    val snack: SnackDto,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
)

@HiltViewModel
class SnacksViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookingId: Int = savedStateHandle.get<Any>("bookingId")?.toString()?.toIntOrNull() ?: 0

    private val _snacksState = MutableStateFlow<UiState<List<SnackDto>>>(UiState.Loading)
    val snacksState: StateFlow<UiState<List<SnackDto>>> = _snacksState.asStateFlow()

    // Map of snackId -> quantity selected (Single Source of Truth)
    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quantities: StateFlow<Map<Int, Int>> = _quantities.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<BookingDto>?>(null)
    val saveState: StateFlow<UiState<BookingDto>?> = _saveState.asStateFlow()

    @Volatile
    private var isBookingSnacksLoaded = false

    init {
        val cached = movieRepository.getCachedSnacks()
        if (cached != null) {
            _snacksState.value = UiState.Success(cached)
        }
        loadSnacks(showLoading = cached == null)
        loadBookingSnacks()
    }

    fun loadBookingSnacks() {
        if (isBookingSnacksLoaded) return
        viewModelScope.launch {
            val res = movieRepository.getBooking(bookingId)
            if (res is UiState.Success) {
                if (!isBookingSnacksLoaded) {
                    val existing = res.data.snacks
                    if (existing.isNotEmpty() && _quantities.value.isEmpty()) {
                        val currentMap = mutableMapOf<Int, Int>()
                        existing.forEach { snack ->
                            currentMap[snack.snackId] = snack.quantity
                        }
                        _quantities.value = currentMap.toMap()
                    }
                    isBookingSnacksLoaded = true
                }
            }
        }
    }

    fun loadSnacks(showLoading: Boolean = true, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cached = movieRepository.getCachedSnacks()
            if (cached != null && !forceRefresh) {
                _snacksState.value = UiState.Success(cached)
            } else if (showLoading && _snacksState.value !is UiState.Success) {
                _snacksState.value = UiState.Loading
            }
            _snacksState.value = movieRepository.getSnacks(forceRefresh = forceRefresh)
        }
    }

    fun incrementQuantity(snackId: Int) {
        val current = _quantities.value.toMutableMap()
        val qty = current.getOrDefault(snackId, 0)
        current[snackId] = qty + 1
        _quantities.value = current.toMap()
    }

    fun decrementQuantity(snackId: Int) {
        val current = _quantities.value.toMutableMap()
        val qty = current.getOrDefault(snackId, 0)
        if (qty > 1) {
            current[snackId] = qty - 1
        } else {
            current.remove(snackId)
        }
        _quantities.value = current.toMap()
    }

    fun removeSnack(snackId: Int) {
        val current = _quantities.value.toMutableMap()
        current.remove(snackId)
        _quantities.value = current.toMap()
    }

    fun getSelectedSnacksList(allSnacks: List<SnackDto> = emptyList()): List<SelectedSnackItem> {
        val fullList = when {
            allSnacks.isNotEmpty() -> allSnacks
            _snacksState.value is UiState.Success -> (_snacksState.value as UiState.Success<List<SnackDto>>).data
            else -> movieRepository.getCachedSnacks() ?: emptyList()
        }
        val map = _quantities.value
        return fullList
            .filter { (map[it.id] ?: 0) > 0 }
            .map { snack ->
                val qty = map[snack.id] ?: 0
                SelectedSnackItem(
                    snack = snack,
                    quantity = qty,
                    unitPrice = snack.price,
                    subtotal = roundToTwoDecimals(snack.price * qty),
                )
            }
    }

    fun calculateTotal(snacks: List<SnackDto> = emptyList()): Double {
        val fullList = when {
            snacks.isNotEmpty() -> snacks
            _snacksState.value is UiState.Success -> (_snacksState.value as UiState.Success<List<SnackDto>>).data
            else -> movieRepository.getCachedSnacks() ?: emptyList()
        }
        val map = _quantities.value
        val sum = fullList.sumOf { snack -> (map[snack.id] ?: 0) * snack.price }
        return roundToTwoDecimals(sum)
    }

    fun getTotalItemsCount(): Int = _quantities.value.values.sum()

    fun submitSnacks(clearAll: Boolean = false) {
        if (_saveState.value is UiState.Loading) return // Prevent duplicate requests
        if (clearAll) {
            _quantities.value = emptyMap()
        }
        val items = if (clearAll) emptyList() else _quantities.value.filter { it.value > 0 }.map {
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

    private fun roundToTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}

