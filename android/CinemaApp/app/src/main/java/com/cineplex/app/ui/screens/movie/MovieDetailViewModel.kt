package com.cineplex.app.ui.screens.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movieState: UiState<MovieDto> = UiState.Loading,
    val showsState: UiState<List<ShowDto>> = UiState.Loading,
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val movieId: Int = checkNotNull(savedStateHandle["movieId"]).toString().toInt()

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        val cached = movieRepository.getCachedMovie(movieId)
        if (cached != null) {
            _uiState.value = MovieDetailUiState(
                movieState = UiState.Success(cached),
                showsState = UiState.Loading
            )
        }
        loadMovieAndShows(showLoading = cached == null)
    }

    fun loadMovieAndShows(showLoading: Boolean = true) {
        viewModelScope.launch {
            val currentMovieState = _uiState.value.movieState
            val currentShowsState = _uiState.value.showsState

            if (showLoading) {
                _uiState.value = MovieDetailUiState(
                    movieState = if (currentMovieState is UiState.Success) currentMovieState else UiState.Loading,
                    showsState = if (currentShowsState is UiState.Success) currentShowsState else UiState.Loading,
                )
            }

            val movieDeferred = async { movieRepository.getMovie(movieId) }
            val showsDeferred = async { movieRepository.getShowsForMovie(movieId) }

            val movieRes = movieDeferred.await()
            val showsRes = showsDeferred.await()

            val finalMovieState = if (movieRes is UiState.Success || _uiState.value.movieState !is UiState.Success) {
                movieRes
            } else {
                _uiState.value.movieState
            }

            val finalShowsState = if (showsRes is UiState.Success || _uiState.value.showsState !is UiState.Success) {
                showsRes
            } else {
                _uiState.value.showsState
            }

            _uiState.value = MovieDetailUiState(
                movieState = finalMovieState,
                showsState = finalShowsState,
            )
        }
    }
}
