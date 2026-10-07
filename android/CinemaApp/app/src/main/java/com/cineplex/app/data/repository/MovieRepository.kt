package com.cineplex.app.data.repository

import com.cineplex.app.data.api.CinemaApiService
import com.cineplex.app.data.model.*
import com.cineplex.app.util.NetworkMonitor
import com.cineplex.app.util.UiState
import com.cineplex.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val api: CinemaApiService,
    private val network: NetworkMonitor,
) {
    suspend fun getMovies(): UiState<List<MovieDto>> =
        safeApiCall(network) { api.getMovies() }

    suspend fun getMovie(id: Int): UiState<MovieDto> =
        safeApiCall(network) { api.getMovie(id) }

    suspend fun getShowsForMovie(movieId: Int): UiState<List<ShowDto>> =
        safeApiCall(network) { api.getShows(movieId = movieId) }

    suspend fun getShow(showId: Int): UiState<ShowDto> =
        safeApiCall(network) { api.getShow(showId) }

    suspend fun getShowSeats(showId: Int): UiState<ShowDetailsWithSeatsDto> =
        safeApiCall(network) { api.getShowSeats(showId) }

    suspend fun createBooking(showId: Int, seatIds: List<Int>): UiState<BookingDto> =
        safeApiCall(network) { api.createBooking(CreateBookingRequest(showId, seatIds)) }

    suspend fun getMyBookings(): UiState<List<BookingDto>> =
        safeApiCall(network) { api.getMyBookings() }

    suspend fun getSnacks(): UiState<List<SnackDto>> =
        safeApiCall(network) { api.getSnacks() }

    suspend fun addSnacksToBooking(bookingId: Int, items: List<SnackItemRequest>): UiState<BookingDto> =
        safeApiCall(network) { api.addSnacksToBooking(bookingId, AddSnacksRequest(items)) }

    suspend fun submitPayment(bookingId: Int, transactionReference: String? = null): UiState<BookingDto> =
        safeApiCall(network) {
            api.submitPayment(SubmitPaymentRequest(bookingId = bookingId, transactionReference = transactionReference))
        }

    suspend fun getAdminPayments(): UiState<List<BookingDto>> =
        safeApiCall(network) { api.getAdminPayments() }

    suspend fun getAdminBookings(statusFilter: String? = null): UiState<List<BookingDto>> =
        safeApiCall(network) { api.getAdminBookings(statusFilter) }

    suspend fun verifyPayment(paymentId: Int, approve: Boolean): UiState<BookingDto> =
        safeApiCall(network) {
            val action = if (approve) "APPROVE" else "REJECT"
            api.verifyPayment(paymentId, PaymentVerificationRequest(action = action))
        }

    suspend fun verifyTicket(code: String): UiState<TicketVerificationDto> =
        safeApiCall(network) { api.verifyTicket(code) }

    suspend fun getPaymentConfig(): UiState<PaymentConfigDto> =
        safeApiCall(network) { api.getPaymentConfig() }

    suspend fun getAdminScreens(): UiState<List<ScreenDto>> =
        safeApiCall(network) { api.getAdminScreens() }

    suspend fun updateScreen(id: Int, request: ScreenUpdateRequest): UiState<ScreenDto> =
        safeApiCall(network) { api.updateScreen(id, request) }

    suspend fun getAdminPaymentConfig(): UiState<PaymentConfigDto> =
        safeApiCall(network) { api.getAdminPaymentConfig() }

    suspend fun updatePaymentConfig(request: PaymentConfigDto): UiState<PaymentConfigDto> =
        safeApiCall(network) { api.updatePaymentConfig(request) }

    // Admin CRUD Operations
    suspend fun createMovie(request: MovieCreateRequest): UiState<MovieDto> =
        safeApiCall(network) { api.createMovie(request) }

    suspend fun updateMovie(id: Int, request: MovieUpdateRequest): UiState<MovieDto> =
        safeApiCall(network) { api.updateMovie(id, request) }

    suspend fun deleteMovie(id: Int): UiState<MovieDto> =
        safeApiCall(network) { api.deleteMovie(id) }

    suspend fun createShow(request: ShowCreateRequest): UiState<ShowDto> =
        safeApiCall(network) { api.createShow(request) }

    suspend fun updateShow(id: Int, request: ShowUpdateRequest): UiState<ShowDto> =
        safeApiCall(network) { api.updateShow(id, request) }

    suspend fun deleteShow(id: Int): UiState<ShowDto> =
        safeApiCall(network) { api.deleteShow(id) }

    suspend fun createSnack(request: SnackCreateRequest): UiState<SnackDto> =
        safeApiCall(network) { api.createSnack(request) }

    suspend fun updateSnack(id: Int, request: SnackUpdateRequest): UiState<SnackDto> =
        safeApiCall(network) { api.updateSnack(id, request) }

    suspend fun deleteSnack(id: Int): UiState<SnackDto> =
        safeApiCall(network) { api.deleteSnack(id) }
}
