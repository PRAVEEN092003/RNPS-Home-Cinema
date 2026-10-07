package com.cineplex.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: UserDto,
)

// ─── User ─────────────────────────────────────────────────────────────────────

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class UserUpdateRequest(
    val name: String? = null,
    val phone: String? = null,
)

// ─── Movie ────────────────────────────────────────────────────────────────────

@Serializable
data class MovieDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val genre: String? = null,
    val language: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val rating: String? = null,
    @SerialName("imdb_rating") val imdbRating: Double? = null,
    val cast: String? = null,
    val director: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("trailer_url") val trailerUrl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("audio_technology") val audioTechnology: String = "Dolby Atmos",
    @SerialName("display_technology") val displayTechnology: String = "4K",
    @SerialName("is_upcoming") val isUpcoming: Boolean = false,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Serializable
data class ScreenDto(
    val id: Int,
    val name: String,
    @SerialName("display_spec") val displaySpec: String = "4K Ultra HD",
    @SerialName("audio_spec") val audioSpec: String = "13-Channel Dolby Atmos",
    @SerialName("has_dolby_atmos") val hasDolbyAtmos: Boolean = true,
    @SerialName("total_seats") val totalSeats: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class ScreenUpdateRequest(
    val name: String? = null,
    @SerialName("display_spec") val displaySpec: String? = null,
    @SerialName("audio_spec") val audioSpec: String? = null,
    @SerialName("has_dolby_atmos") val hasDolbyAtmos: Boolean? = null,
    @SerialName("seat_base_price") val seatBasePrice: Double? = null,
)

@Serializable
data class PaymentConfigDto(
    @SerialName("upi_id") val upiId: String,
    @SerialName("payee_name") val payeeName: String,
)

// ─── Show ─────────────────────────────────────────────────────────────────────

@Serializable
data class ShowDto(
    val id: Int,
    @SerialName("movie_id") val movieId: Int,
    @SerialName("screen_id") val screenId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("base_price") val basePrice: Double,
    @SerialName("is_active") val isActive: Boolean,
    val movie: MovieDto? = null,
    val screen: ScreenDto? = null,
)

// ─── ShowSeat ─────────────────────────────────────────────────────────────────

@Serializable
data class ShowSeatDto(
    val id: Int,
    @SerialName("show_id") val showId: Int,
    @SerialName("seat_id") val seatId: Int,
    @SerialName("row_label") val rowLabel: String,
    @SerialName("seat_number") val seatNumber: Int,
    @SerialName("seat_code") val seatCode: String,
    @SerialName("seat_type") val seatType: String,
    val status: String,
    val price: Double,
)

@Serializable
data class ShowDetailsWithSeatsDto(
    val show: ShowDto,
    val seats: List<ShowSeatDto>,
)

// ─── Movie CRUD DTOs ─────────────────────────────────────────────────────────

@Serializable
data class MovieCreateRequest(
    val title: String,
    val description: String? = null,
    val genre: String? = null,
    val language: String = "English",
    @SerialName("duration_minutes") val durationMinutes: Int,
    val rating: String? = "U/A",
    @SerialName("imdb_rating") val imdbRating: Double? = null,
    val cast: String? = null,
    val director: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("audio_technology") val audioTechnology: String = "Dolby Atmos",
    @SerialName("display_technology") val displayTechnology: String = "4K",
)

@Serializable
data class MovieUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val language: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val rating: String? = null,
    @SerialName("imdb_rating") val imdbRating: Double? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("audio_technology") val audioTechnology: String? = null,
    @SerialName("display_technology") val displayTechnology: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

// ─── Show CRUD DTOs ──────────────────────────────────────────────────────────

@Serializable
data class ShowCreateRequest(
    @SerialName("movie_id") val movieId: Int,
    @SerialName("screen_id") val screenId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("base_price") val basePrice: Double,
)

@Serializable
data class ShowUpdateRequest(
    @SerialName("movie_id") val movieId: Int? = null,
    @SerialName("screen_id") val screenId: Int? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("base_price") val basePrice: Double? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

// ─── Snack DTOs ───────────────────────────────────────────────────────────────

@Serializable
data class SnackCreateRequest(
    val name: String,
    val description: String? = null,
    val category: String = "POPCORN",
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class SnackUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val price: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_available") val isAvailable: Boolean? = null,
)

@Serializable
data class SnackDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val category: String,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
)

@Serializable
data class SnackItemRequest(
    @SerialName("snack_id") val snackId: Int,
    val quantity: Int,
)

@Serializable
data class AddSnacksRequest(
    val items: List<SnackItemRequest>,
)

@Serializable
data class BookingSnackDto(
    val id: Int = 0,
    @SerialName("snack_id") val snackId: Int,
    @SerialName("snack_name") val snackName: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("total_price") val totalPrice: Double,
)

// ─── Payment & Ticket DTOs ────────────────────────────────────────────────────

@Serializable
data class SubmitPaymentRequest(
    @SerialName("booking_id") val bookingId: Int,
    @SerialName("payment_method") val paymentMethod: String = "UPI",
    @SerialName("transaction_reference") val transactionReference: String? = null,
)

@Serializable
data class PaymentVerificationRequest(
    val action: String, // "APPROVE" or "REJECT"
)

@Serializable
data class TicketVerificationDto(
    @SerialName("is_valid") val isValid: Boolean,
    @SerialName("ticket_code") val ticketCode: String,
    @SerialName("booking_reference") val bookingReference: String,
    @SerialName("user_name") val userName: String,
    @SerialName("movie_title") val movieTitle: String,
    @SerialName("screen_name") val screenName: String,
    @SerialName("show_time") val showTime: String,
    val seats: List<String>,
    @SerialName("booking_status") val bookingStatus: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("issued_at") val issuedAt: String,
)

@Serializable
data class PaymentDto(
    val id: Int,
    @SerialName("booking_id") val bookingId: Int,
    val amount: Double,
    val method: String? = null,
    val status: String,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TicketDto(
    val id: Int,
    @SerialName("ticket_code") val ticketCode: String,
    @SerialName("qr_data") val qrData: String? = null,
    @SerialName("issued_at") val issuedAt: String,
    @SerialName("is_used") val isUsed: Boolean = false,
)

// ─── Booking DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class CreateBookingRequest(
    @SerialName("show_id") val showId: Int,
    @SerialName("seat_ids") val seatIds: List<Int>,
)

@Serializable
data class BookingSeatDto(
    val id: Int,
    @SerialName("seat_id") val seatId: Int,
    @SerialName("seat_code") val seatCode: String,
    @SerialName("row_label") val rowLabel: String,
    @SerialName("seat_number") val seatNumber: Int,
    val price: Double,
)

@Serializable
data class BookingDto(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("show_id") val showId: Int,
    @SerialName("booking_reference") val bookingReference: String,
    val status: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("convenience_fee") val convenienceFee: Double,
    @SerialName("total_seats") val totalSeats: Int,
    @SerialName("created_at") val createdAt: String,
    val show: ShowDto? = null,
    val seats: List<BookingSeatDto> = emptyList(),
    val snacks: List<BookingSnackDto> = emptyList(),
    @SerialName("ticket_amount") val ticketAmount: Double = 0.0,
    @SerialName("snacks_amount") val snacksAmount: Double = 0.0,
    val payment: PaymentDto? = null,
    val ticket: TicketDto? = null,
)

// ─── Health ───────────────────────────────────────────────────────────────────

@Serializable
data class HealthDto(
    val status: String,
    @SerialName("app_name") val appName: String,
    val version: String,
    val database: String,
    val timestamp: String,
)

// ─── API Error ────────────────────────────────────────────────────────────────

@Serializable
data class ApiError(
    val detail: String,
    @SerialName("error_code") val errorCode: String? = null,
)

// ─── User Role ────────────────────────────────────────────────────────────────

enum class UserRole { USER, ADMIN }

fun UserDto.userRole(): UserRole =
    if (role == "ADMIN") UserRole.ADMIN else UserRole.USER
