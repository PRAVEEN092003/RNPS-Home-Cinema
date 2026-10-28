package com.cineplex.app.data.repository

import android.content.Context
import com.cineplex.app.data.api.CinemaApiService
import com.cineplex.app.data.firebase.FirebaseDataSource
import com.cineplex.app.data.model.*
import com.cineplex.app.util.NetworkMonitor
import com.cineplex.app.util.UiState
import com.cineplex.app.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MovieRepository protected constructor(
    private val api: CinemaApiService?,
    private val network: NetworkMonitor?,
    private val context: Context?,
    private val firebase: FirebaseDataSource?,
    @Suppress("UNUSED_PARAMETER") dummy: Unit,
) {
    @Inject
    constructor(
        api: CinemaApiService,
        network: NetworkMonitor,
        @ApplicationContext context: Context,
        firebase: FirebaseDataSource,
    ) : this(api, network, context, firebase, Unit)

    constructor() : this(null, null, null, null, Unit)

    private val json = Json { ignoreUnknownKeys = true }
    private val sharedPrefs = context?.getSharedPreferences("movie_cache_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var cachedMovies: List<MovieDto>? = null
    @Volatile
    private var lastMoviesFetchTimeMs: Long = 0L
    private val CACHE_TTL_MS = 300_000L // 5 minutes

    @Volatile
    private var cachedSnacks: List<SnackDto>? = null
    @Volatile
    private var lastSnacksFetchTimeMs: Long = 0L
    private val SNACKS_CACHE_TTL_MS = 600_000L // 10 minutes

    @Volatile
    private var cachedPaymentConfig: PaymentConfigDto? = null
    @Volatile
    private var lastPaymentConfigFetchTimeMs: Long = 0L
    private val PAYMENT_CONFIG_TTL_MS = 3_600_000L // 1 hour

    init {
        // Fast local disk restoration on startup so app has instant content
        sharedPrefs?.getString("cached_movies_json", null)?.let { raw ->
            runCatching { json.decodeFromString<List<MovieDto>>(raw) }.getOrNull()?.let {
                cachedMovies = it
            }
        }
        sharedPrefs?.getString("cached_snacks_json", null)?.let { raw ->
            runCatching { json.decodeFromString<List<SnackDto>>(raw) }.getOrNull()?.let {
                cachedSnacks = it
            }
        }
        sharedPrefs?.getString("cached_payment_config_json", null)?.let { raw ->
            runCatching { json.decodeFromString<PaymentConfigDto>(raw) }.getOrNull()?.let {
                cachedPaymentConfig = it
            }
        }
    }

    open fun getCachedMovies(): List<MovieDto>? = cachedMovies

    open fun getCachedMovie(id: Int): MovieDto? = cachedMovies?.find { it.id == id }

    open suspend fun getMovies(forceRefresh: Boolean = false): UiState<List<MovieDto>> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedMovies != null && (now - lastMoviesFetchTimeMs < CACHE_TTL_MS)) {
            return UiState.Success(cachedMovies!!)
        }

        val result = safeApiCall(network!!) { api!!.getMovies() }
        if (result is UiState.Success) {
            cachedMovies = result.data
            lastMoviesFetchTimeMs = now
            runCatching {
                sharedPrefs?.edit()?.putString("cached_movies_json", json.encodeToString(result.data))?.apply()
            }
            return result
        } else if (firebase != null) {
            // Graceful Firestore workflow
            try {
                val fMovies = firebase.getMovies()
                if (fMovies.isNotEmpty()) {
                    cachedMovies = fMovies
                    lastMoviesFetchTimeMs = now
                    return UiState.Success(fMovies)
                }
            } catch (_: Exception) {}
        }
        if (cachedMovies != null) {
            return UiState.Success(cachedMovies!!)
        }
        return result
    }

    open fun invalidateMovieCache() {
        cachedMovies = null
        lastMoviesFetchTimeMs = 0L
        sharedPrefs?.edit()?.remove("cached_movies_json")?.apply()
    }

    open suspend fun getMovie(id: Int): UiState<MovieDto> {
        val cached = getCachedMovie(id)
        val result = safeApiCall(network!!) { api!!.getMovie(id) }
        if (result is UiState.Success) {
            cachedMovies = cachedMovies?.map { if (it.id == id) result.data else it } ?: listOf(result.data)
            return result
        } else if (firebase != null) {
            try {
                val fMovie = firebase.getMovie(id)
                return UiState.Success(fMovie)
            } catch (_: Exception) {}
        }
        if (cached != null) {
            return UiState.Success(cached)
        }
        return result
    }

    open suspend fun getShowsForMovie(movieId: Int): UiState<List<ShowDto>> =
        safeApiCall(network!!) { api!!.getShows(movieId = movieId) }

    open suspend fun getShow(showId: Int): UiState<ShowDto> =
        safeApiCall(network!!) { api!!.getShow(showId) }

    open suspend fun getShowSeats(showId: Int): UiState<ShowDetailsWithSeatsDto> {
        val result = safeApiCall(network!!) { api!!.getShowSeats(showId) }
        if (result is UiState.Success) return result
        if (firebase != null) {
            try {
                val fSeats = firebase.getShowSeats(showId)
                return UiState.Success(fSeats)
            } catch (_: Exception) {}
        }
        return result
    }

    open suspend fun createBooking(showId: Int, seatIds: List<Int>): UiState<BookingDto> =
        safeApiCall(network!!) { api!!.createBooking(CreateBookingRequest(showId, seatIds)) }

    open suspend fun getBooking(bookingId: Int): UiState<BookingDto> =
        safeApiCall(network!!) { api!!.getBooking(bookingId) }

    open suspend fun cancelBooking(bookingId: Int): UiState<Map<String, String>> =
        safeApiCall(network!!) { api!!.cancelBooking(bookingId) }

    open suspend fun getMyBookings(status: String? = null): UiState<List<BookingDto>> =
        safeApiCall(network!!) { api!!.getMyBookings(status) }

    open fun getCachedSnacks(): List<SnackDto>? = cachedSnacks

    open suspend fun getSnacks(forceRefresh: Boolean = false): UiState<List<SnackDto>> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedSnacks != null && (now - lastSnacksFetchTimeMs < SNACKS_CACHE_TTL_MS)) {
            return UiState.Success(cachedSnacks!!)
        }
        val result = safeApiCall(network!!) { api!!.getSnacks() }
        if (result is UiState.Success) {
            cachedSnacks = result.data
            lastSnacksFetchTimeMs = now
            runCatching {
                sharedPrefs?.edit()?.putString("cached_snacks_json", json.encodeToString(result.data))?.apply()
            }
            return result
        } else if (firebase != null) {
            try {
                val fSnacks = firebase.getSnacks()
                if (fSnacks.isNotEmpty()) {
                    cachedSnacks = fSnacks
                    lastSnacksFetchTimeMs = now
                    return UiState.Success(fSnacks)
                }
            } catch (_: Exception) {}
        }
        if (cachedSnacks != null) {
            return UiState.Success(cachedSnacks!!)
        }
        return result
    }

    open fun invalidateSnacksCache() {
        cachedSnacks = null
        lastSnacksFetchTimeMs = 0L
        sharedPrefs?.edit()?.remove("cached_snacks_json")?.apply()
    }

    open suspend fun addSnacksToBooking(bookingId: Int, items: List<SnackItemRequest>): UiState<BookingDto> =
        safeApiCall(network!!) { api!!.addSnacksToBooking(bookingId, AddSnacksRequest(items)) }

    open suspend fun submitPayment(bookingId: Int, transactionReference: String? = null): UiState<BookingDto> =
        safeApiCall(network!!) {
            api!!.submitPayment(SubmitPaymentRequest(bookingId = bookingId, transactionReference = transactionReference))
        }


    suspend fun getAdminPayments(): UiState<List<BookingDto>> =
        safeApiCall(network!!) { api!!.getAdminPayments() }

    suspend fun getAdminBookings(statusFilter: String? = null): UiState<List<BookingDto>> =
        safeApiCall(network!!) { api!!.getAdminBookings(statusFilter) }

    suspend fun verifyPayment(paymentId: Int, approve: Boolean): UiState<BookingDto> =
        safeApiCall(network!!) {
            val action = if (approve) "APPROVE" else "REJECT"
            api!!.verifyPayment(paymentId, PaymentVerificationRequest(action = action))
        }

    suspend fun verifyTicket(code: String): UiState<TicketVerificationDto> =
        safeApiCall(network!!) { api!!.verifyTicket(code) }

    open suspend fun getPaymentConfig(): UiState<PaymentConfigDto> {
        val now = System.currentTimeMillis()
        if (cachedPaymentConfig != null && (now - lastPaymentConfigFetchTimeMs < PAYMENT_CONFIG_TTL_MS)) {
            return UiState.Success(cachedPaymentConfig!!)
        }
        val result = safeApiCall(network!!) { api!!.getPaymentConfig() }
        if (result is UiState.Success) {
            cachedPaymentConfig = result.data
            lastPaymentConfigFetchTimeMs = now
            runCatching {
                sharedPrefs?.edit()?.putString("cached_payment_config_json", json.encodeToString(result.data))?.apply()
            }
            return result
        } else if (cachedPaymentConfig != null) {
            return UiState.Success(cachedPaymentConfig!!)
        }
        return result
    }

    suspend fun getAdminScreens(): UiState<List<ScreenDto>> =
        safeApiCall(network!!) { api!!.getAdminScreens() }

    suspend fun updateScreen(id: Int, request: ScreenUpdateRequest): UiState<ScreenDto> =
        safeApiCall(network!!) { api!!.updateScreen(id, request) }

    suspend fun getAdminPaymentConfig(): UiState<PaymentConfigDto> =
        safeApiCall(network!!) { api!!.getAdminPaymentConfig() }

    suspend fun updatePaymentConfig(request: PaymentConfigDto): UiState<PaymentConfigDto> {
        val result = safeApiCall(network!!) { api!!.updatePaymentConfig(request) }
        if (result is UiState.Success) {
            cachedPaymentConfig = result.data
            lastPaymentConfigFetchTimeMs = System.currentTimeMillis()
            runCatching {
                sharedPrefs?.edit()?.putString("cached_payment_config_json", json.encodeToString(result.data))?.apply()
            }
        }
        return result
    }

    // Admin CRUD Operations
    suspend fun createMovie(request: MovieCreateRequest): UiState<MovieDto> {
        val result = safeApiCall(network!!) { api!!.createMovie(request) }
        if (result is UiState.Success) invalidateMovieCache()
        return result
    }

    suspend fun updateMovie(id: Int, request: MovieUpdateRequest): UiState<MovieDto> {
        val result = safeApiCall(network!!) { api!!.updateMovie(id, request) }
        if (result is UiState.Success) invalidateMovieCache()
        return result
    }

    suspend fun deleteMovie(id: Int): UiState<MovieDto> {
        val result = safeApiCall(network!!) { api!!.deleteMovie(id) }
        if (result is UiState.Success) invalidateMovieCache()
        return result
    }

    suspend fun createShow(request: ShowCreateRequest): UiState<ShowDto> =
        safeApiCall(network!!) { api!!.createShow(request) }

    suspend fun updateShow(id: Int, request: ShowUpdateRequest): UiState<ShowDto> =
        safeApiCall(network!!) { api!!.updateShow(id, request) }

    suspend fun deleteShow(id: Int): UiState<ShowDto> =
        safeApiCall(network!!) { api!!.deleteShow(id) }

    suspend fun createSnack(request: SnackCreateRequest): UiState<SnackDto> {
        val result = safeApiCall(network!!) { api!!.createSnack(request) }
        if (result is UiState.Success) invalidateSnacksCache()
        return result
    }

    suspend fun updateSnack(id: Int, request: SnackUpdateRequest): UiState<SnackDto> {
        val result = safeApiCall(network!!) { api!!.updateSnack(id, request) }
        if (result is UiState.Success) invalidateSnacksCache()
        return result
    }

    suspend fun deleteSnack(id: Int): UiState<SnackDto> {
        val result = safeApiCall(network!!) { api!!.deleteSnack(id) }
        if (result is UiState.Success) invalidateSnacksCache()
        return result
    }
}
