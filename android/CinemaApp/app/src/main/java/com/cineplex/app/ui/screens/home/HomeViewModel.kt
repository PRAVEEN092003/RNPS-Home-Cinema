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
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _userState.value = authRepository.getCachedUser()
            _moviesState.value = UiState.Loading
            _moviesState.value = movieRepository.getMovies()
        }
    }
}
