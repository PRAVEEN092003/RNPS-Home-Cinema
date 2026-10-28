package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.SnackCreateRequest
import com.cineplex.app.data.model.SnackDto
import com.cineplex.app.data.model.SnackUpdateRequest
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSnacksViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _snacksState = MutableStateFlow<UiState<List<SnackDto>>>(UiState.Loading)
    val snacksState: StateFlow<UiState<List<SnackDto>>> = _snacksState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadSnacks()
    }

    fun loadSnacks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                repository.invalidateSnacksCache()
            }
            if (_snacksState.value !is UiState.Success) {
                _snacksState.value = UiState.Loading
            }
            _snacksState.value = repository.getSnacks(forceRefresh = forceRefresh)
        }
    }

    fun createSnack(
        name: String,
        description: String,
        category: String,
        price: Double,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val request = SnackCreateRequest(
                name = name,
                description = description.ifBlank { null },
                category = category,
                price = price,
                imageUrl = imageUrl.ifBlank { null }
            )
            when (val result = repository.createSnack(request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Snack '${result.data.name}' added successfully!"
                    loadSnacks(forceRefresh = true)
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun updateSnack(
        snackId: Int,
        name: String,
        description: String,
        category: String,
        price: Double,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val request = SnackUpdateRequest(
                name = name,
                description = description.ifBlank { null },
                category = category,
                price = price,
                imageUrl = imageUrl.ifBlank { null }
            )
            when (val result = repository.updateSnack(snackId, request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Snack updated successfully!"
                    loadSnacks(forceRefresh = true)
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun deleteSnack(snackId: Int) {
        val previousState = _snacksState.value
        val currentList = (previousState as? UiState.Success)?.data ?: emptyList()
        // Optimistic UI update: immediately remove from displayed list
        _snacksState.value = UiState.Success(currentList.filter { it.id != snackId })

        viewModelScope.launch {
            when (val result = repository.deleteSnack(snackId)) {
                is UiState.Success -> {
                    _actionMessage.value = "Snack deactivated!"
                    loadSnacks(forceRefresh = true)
                }
                is UiState.Error -> {
                    // Rollback to previous state on failure
                    _snacksState.value = previousState
                    _actionMessage.value = "Failed: ${result.message}"
                }
                else -> {}
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
