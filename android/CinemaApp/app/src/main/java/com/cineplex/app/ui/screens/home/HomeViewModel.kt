package com.cineplex.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.UserDto
import com.cineplex.app.data.repository.AuthRepository
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _moviesState = MutableStateFlow<UiState<List<MovieDto>>>(UiState.Loading)
    val moviesState: StateFlow<UiState<List<MovieDto>>> = _moviesState.asStateFlow()

    private val _userState = MutableStateFlow<UserDto?>(null)
    val userState: StateFlow<UserDto?> = _userState.asStateFlow()

    init {
        val cached = movieRepository.getCachedMovies()
        if (cached != null) {
            _moviesState.value = UiState.Success(cached)
        }
        loadData(showLoading = cached == null)
    }

    fun loadData(showLoading: Boolean = true, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _userState.value = authRepository.getCachedUser()
            val cached = movieRepository.getCachedMovies()
            if (cached != null) {
                _moviesState.value = UiState.Success(cached)
            } else if (showLoading && _moviesState.value !is UiState.Success) {
                _moviesState.value = UiState.Loading
            }
            val result = movieRepository.getMovies(forceRefresh = forceRefresh)
            _moviesState.value = result
        }
    }
}
