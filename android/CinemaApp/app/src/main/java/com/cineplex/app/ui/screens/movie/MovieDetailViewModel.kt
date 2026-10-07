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
        loadMovieAndShows()
    }

    fun loadMovieAndShows() {
        viewModelScope.launch {
            _uiState.value = MovieDetailUiState(
                movieState = UiState.Loading,
                showsState = UiState.Loading
            )

            val movieRes = movieRepository.getMovie(movieId)
            val showsRes = movieRepository.getShowsForMovie(movieId)

            _uiState.value = MovieDetailUiState(
                movieState = movieRes,
                showsState = showsRes
            )
        }
    }
}
