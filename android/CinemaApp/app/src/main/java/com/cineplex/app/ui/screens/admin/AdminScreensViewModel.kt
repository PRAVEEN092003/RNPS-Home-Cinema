package com.cineplex.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineplex.app.data.model.PaymentConfigDto
import com.cineplex.app.data.model.ScreenDto
import com.cineplex.app.data.model.ScreenUpdateRequest
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminScreensViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _screensState = MutableStateFlow<UiState<List<ScreenDto>>>(UiState.Loading)
    val screensState: StateFlow<UiState<List<ScreenDto>>> = _screensState.asStateFlow()

    private val _configState = MutableStateFlow<UiState<PaymentConfigDto>>(UiState.Loading)
    val configState: StateFlow<UiState<PaymentConfigDto>> = _configState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>?>(null)
    val actionState: StateFlow<UiState<Unit>?> = _actionState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _screensState.value = UiState.Loading
            _configState.value = UiState.Loading
            _screensState.value = movieRepository.getAdminScreens()
            _configState.value = movieRepository.getAdminPaymentConfig()
        }
    }

    fun updateScreen(
        id: Int,
        name: String?,
        displaySpec: String?,
        audioSpec: String?,
        hasDolbyAtmos: Boolean?,
        seatBasePrice: Double?
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val req = ScreenUpdateRequest(
                name = name,
                displaySpec = displaySpec,
                audioSpec = audioSpec,
                hasDolbyAtmos = hasDolbyAtmos,
                seatBasePrice = seatBasePrice
            )
            val res = movieRepository.updateScreen(id, req)
            if (res is UiState.Success) {
                _actionState.value = UiState.Success(Unit)
                loadData()
            } else if (res is UiState.Error) {
                _actionState.value = UiState.Error(res.message)
            }
        }
    }

    fun updatePaymentConfig(upiId: String, payeeName: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val res = movieRepository.updatePaymentConfig(PaymentConfigDto(upiId = upiId, payeeName = payeeName))
            if (res is UiState.Success) {
                _actionState.value = UiState.Success(Unit)
                loadData()
            } else if (res is UiState.Error) {
                _actionState.value = UiState.Error(res.message)
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }
}
