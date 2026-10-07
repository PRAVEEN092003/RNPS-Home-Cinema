package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ShowCreateRequest
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.data.model.ShowUpdateRequest
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminShowsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _showsState = MutableStateFlow<UiState<List<ShowDto>>>(UiState.Loading)
    val showsState: StateFlow<UiState<List<ShowDto>>> = _showsState.asStateFlow()

    private val _moviesList = MutableStateFlow<List<MovieDto>>(emptyList())
    val moviesList: StateFlow<List<MovieDto>> = _moviesList.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _showsState.value = UiState.Loading
            when (val moviesRes = repository.getMovies()) {
                is UiState.Success -> _moviesList.value = moviesRes.data
                else -> {}
            }
            _showsState.value = repository.getShowsForMovie(0).let {
                // If getShowsForMovie(0) returns all shows, or we call api.getShows()
                // repository.getShowsForMovie with null returns all shows
                repository.getShowsForMovie(0)
            }
            // Load all shows
            loadShows()
        }
    }

    fun loadShows() {
        viewModelScope.launch {
            _showsState.value = UiState.Loading
            // Passing null/0 to list all shows
            when (val res = repository.getShowsForMovie(0)) {
                is UiState.Success -> _showsState.value = res
                is UiState.Error -> _showsState.value = res
                else -> _showsState.value = UiState.Empty
            }
        }
    }

    fun createShow(
        movieId: Int,
        screenId: Int,
        startTime: String,
        endTime: String,
        basePrice: Double
    ) {
        viewModelScope.launch {
            val request = ShowCreateRequest(
                movieId = movieId,
                screenId = screenId,
                startTime = startTime,
                endTime = endTime,
                basePrice = basePrice
            )
            when (val result = repository.createShow(request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Show created successfully!"
                    loadShows()
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun updateShow(
        showId: Int,
        movieId: Int,
        screenId: Int,
        startTime: String,
        endTime: String,
        basePrice: Double
    ) {
        viewModelScope.launch {
            val request = ShowUpdateRequest(
                movieId = movieId,
                screenId = screenId,
                startTime = startTime,
                endTime = endTime,
                basePrice = basePrice
            )
            when (val result = repository.updateShow(showId, request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Show updated successfully!"
                    loadShows()
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun deleteShow(showId: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteShow(showId)) {
                is UiState.Success -> {
                    _actionMessage.value = "Show deactivated!"
                    loadShows()
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
