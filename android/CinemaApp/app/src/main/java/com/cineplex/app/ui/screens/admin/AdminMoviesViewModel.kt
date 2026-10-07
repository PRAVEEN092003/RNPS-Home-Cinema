package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.MovieCreateRequest
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.MovieUpdateRequest
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminMoviesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _moviesState = MutableStateFlow<UiState<List<MovieDto>>>(UiState.Loading)
    val moviesState: StateFlow<UiState<List<MovieDto>>> = _moviesState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _moviesState.value = UiState.Loading
            _moviesState.value = repository.getMovies()
        }
    }

    fun createMovie(
        title: String,
        description: String,
        durationMinutes: Int,
        language: String,
        genre: String,
        rating: String,
        posterUrl: String,
        audioTechnology: String = "Dolby Atmos",
        displayTechnology: String = "4K"
    ) {
        viewModelScope.launch {
            val request = MovieCreateRequest(
                title = title,
                description = description.ifBlank { null },
                durationMinutes = durationMinutes,
                language = language.ifBlank { "English" },
                genre = genre.ifBlank { null },
                rating = rating.ifBlank { "U/A" },
                posterUrl = posterUrl.ifBlank { null },
                audioTechnology = audioTechnology,
                displayTechnology = displayTechnology
            )
            when (val result = repository.createMovie(request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Movie '${result.data.title}' created successfully!"
                    loadMovies()
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun updateMovie(
        movieId: Int,
        title: String,
        description: String,
        durationMinutes: Int,
        language: String,
        genre: String,
        rating: String,
        posterUrl: String,
        audioTechnology: String = "Dolby Atmos",
        displayTechnology: String = "4K"
    ) {
        viewModelScope.launch {
            val request = MovieUpdateRequest(
                title = title,
                description = description.ifBlank { null },
                durationMinutes = durationMinutes,
                language = language.ifBlank { "English" },
                genre = genre.ifBlank { null },
                rating = rating.ifBlank { "U/A" },
                posterUrl = posterUrl.ifBlank { null },
                audioTechnology = audioTechnology,
                displayTechnology = displayTechnology
            )
            when (val result = repository.updateMovie(movieId, request)) {
                is UiState.Success -> {
                    _actionMessage.value = "Movie updated successfully!"
                    loadMovies()
                }
                is UiState.Error -> _actionMessage.value = "Failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun deleteMovie(movieId: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteMovie(movieId)) {
                is UiState.Success -> {
                    _actionMessage.value = "Movie deactivated!"
                    loadMovies()
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
