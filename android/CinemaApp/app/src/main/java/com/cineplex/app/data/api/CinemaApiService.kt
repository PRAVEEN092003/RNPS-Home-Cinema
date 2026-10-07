package com.cineplex.app.data.api

import com.cineplex.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CinemaApiService {

    // ─── Health ───────────────────────────────────────────────────────────────

    @GET("api/health")
    suspend fun health(): Response<HealthDto>

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserDto>

    // ─── Users ────────────────────────────────────────────────────────────────

    @PATCH("api/users/me")
    suspend fun updateProfile(@Body request: UserUpdateRequest): Response<UserDto>

    // ─── Movies ───────────────────────────────────────────────────────────────

    @GET("api/movies/")
    suspend fun getMovies(): Response<List<MovieDto>>

    @GET("api/movies/{id}")
    suspend fun getMovie(@Path("id") id: Int): Response<MovieDto>

    // ─── Shows ────────────────────────────────────────────────────────────────

    @GET("api/shows/")
    suspend fun getShows(@Query("movie_id") movieId: Int? = null): Response<List<ShowDto>>

    @GET("api/shows/{id}")
    suspend fun getShow(@Path("id") id: Int): Response<ShowDto>

    @GET("api/shows/{id}/seats")
    suspend fun getShowSeats(@Path("id") showId: Int): Response<ShowDetailsWithSeatsDto>

    // ─── Bookings ─────────────────────────────────────────────────────────────

    @POST("api/bookings/")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<BookingDto>

    @GET("api/bookings/my")
    suspend fun getMyBookings(): Response<List<BookingDto>>

    // ─── Snacks ───────────────────────────────────────────────────────────────

    @GET("api/snacks/")
    suspend fun getSnacks(): Response<List<SnackDto>>

    @POST("api/snacks/booking/{bookingId}")
    suspend fun addSnacksToBooking(
        @Path("bookingId") bookingId: Int,
        @Body request: AddSnacksRequest
    ): Response<BookingDto>

    // ─── Payments ─────────────────────────────────────────────────────────────

    @GET("api/payments/config")
    suspend fun getPaymentConfig(): Response<PaymentConfigDto>

    @POST("api/payments/submit")
    suspend fun submitPayment(@Body request: SubmitPaymentRequest): Response<BookingDto>

    // ─── Admin Management ─────────────────────────────────────────────────────

    @GET("api/admin/screens/")
    suspend fun getAdminScreens(): Response<List<ScreenDto>>

    @PUT("api/admin/screens/{id}")
    suspend fun updateScreen(
        @Path("id") id: Int,
        @Body request: ScreenUpdateRequest
    ): Response<ScreenDto>

    @GET("api/admin/payments/config")
    suspend fun getAdminPaymentConfig(): Response<PaymentConfigDto>

    @PUT("api/admin/payments/config")
    suspend fun updatePaymentConfig(@Body request: PaymentConfigDto): Response<PaymentConfigDto>

    @POST("api/admin/movies/")
    suspend fun createMovie(@Body request: MovieCreateRequest): Response<MovieDto>

    @PUT("api/admin/movies/{id}")
    suspend fun updateMovie(@Path("id") id: Int, @Body request: MovieUpdateRequest): Response<MovieDto>

    @DELETE("api/admin/movies/{id}")
    suspend fun deleteMovie(@Path("id") id: Int): Response<MovieDto>

    @POST("api/admin/shows/")
    suspend fun createShow(@Body request: ShowCreateRequest): Response<ShowDto>

    @PUT("api/admin/shows/{id}")
    suspend fun updateShow(@Path("id") id: Int, @Body request: ShowUpdateRequest): Response<ShowDto>

    @DELETE("api/admin/shows/{id}")
    suspend fun deleteShow(@Path("id") id: Int): Response<ShowDto>

    @POST("api/admin/snacks/")
    suspend fun createSnack(@Body request: SnackCreateRequest): Response<SnackDto>

    @PUT("api/admin/snacks/{id}")
    suspend fun updateSnack(@Path("id") id: Int, @Body request: SnackUpdateRequest): Response<SnackDto>

    @DELETE("api/admin/snacks/{id}")
    suspend fun deleteSnack(@Path("id") id: Int): Response<SnackDto>

    @GET("api/admin/bookings/")
    suspend fun getAdminBookings(@Query("status_filter") statusFilter: String? = null): Response<List<BookingDto>>

    @GET("api/admin/payments/")
    suspend fun getAdminPayments(): Response<List<BookingDto>>

    @POST("api/admin/payments/{id}/verify")
    suspend fun verifyPayment(
        @Path("id") paymentId: Int,
        @Body request: PaymentVerificationRequest
    ): Response<BookingDto>

    @GET("api/admin/tickets/verify/{code}")
    suspend fun verifyTicket(
        @Path("code") code: String
    ): Response<TicketVerificationDto>
}
