package com.cineplex.app.data.firebase

import com.cineplex.app.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // ─── AUTHENTICATION ───────────────────────────────────────────────────────

    suspend fun register(name: String, email: String, password: String, phone: String?): TokenResponse {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Registration failed: user is null")
        val uid = user.uid

        val userDoc = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to (phone ?: ""),
            "role" to "USER",
            "createdAt" to Timestamp.now(),
        )
        firestore.collection("users").document(uid).set(userDoc).await()

        val token = user.getIdToken(false).await().token ?: ""
        return TokenResponse(
            accessToken = token,
            tokenType = "bearer",
            user = UserDto(
                id = 0, // Staging numeric ID alias
                name = name,
                email = email,
                phone = phone,
                role = "USER",
                isActive = true,
                createdAt = isoFormat.format(Date()),
                uid = uid,
            )
        )
    }

    suspend fun login(email: String, password: String): TokenResponse {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Login failed: user is null")
        val uid = user.uid

        val doc = firestore.collection("users").document(uid).get().await()
        val name = doc.getString("name") ?: email.substringBefore("@")
        val role = doc.getString("role") ?: "USER"
        val phone = doc.getString("phone")

        val token = user.getIdToken(false).await().token ?: ""
        return TokenResponse(
            accessToken = token,
            tokenType = "bearer",
            user = UserDto(
                id = 0,
                name = name,
                email = email,
                phone = phone,
                role = role,
                isActive = true,
                createdAt = isoFormat.format(Date()),
                uid = uid,
            )
        )
    }

    suspend fun getMe(): UserDto {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val doc = firestore.collection("users").document(user.uid).get().await()
        return UserDto(
            id = 0,
            name = doc.getString("name") ?: user.displayName ?: "User",
            email = user.email ?: "",
            phone = doc.getString("phone"),
            role = doc.getString("role") ?: "USER",
            isActive = true,
            createdAt = isoFormat.format(Date()),
            uid = user.uid,
        )
    }

    fun logout() {
        auth.signOut()
    }

    // ─── MOVIES & SHOWS ───────────────────────────────────────────────────────

    suspend fun getMovies(): List<MovieDto> {
        val snap = firestore.collection("movies")
            .whereEqualTo("isActive", true)
            .get()
            .await()

        return snap.documents.mapIndexed { index, doc ->
            MovieDto(
                id = doc.getLong("legacyId")?.toInt() ?: (index + 1),
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                genre = doc.getString("genre") ?: "Cinema",
                language = doc.getString("language") ?: "English",
                durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 120,
                rating = doc.getString("rating") ?: "U/A",
                imdbRating = doc.getDouble("imdbRating") ?: 8.0,
                cast = doc.getString("cast"),
                director = doc.getString("director"),
                posterUrl = doc.getString("posterUrl"),
                bannerUrl = doc.getString("bannerUrl"),
                trailerUrl = doc.getString("trailerUrl"),
                releaseDate = doc.getString("releaseDate") ?: "2026-10-26",
                audioTechnology = doc.getString("audioTechnology") ?: "13-Channel Dolby Atmos",
                displayTechnology = doc.getString("displayTechnology") ?: "4K Ultra HD",
                isUpcoming = doc.getBoolean("isUpcoming") ?: false,
                isActive = doc.getBoolean("isActive") ?: true,
                createdAt = doc.getString("createdAt") ?: "2026-10-26T00:00:00",
                documentId = doc.id,
            )
        }
    }

    suspend fun getMovie(id: Int): MovieDto {
        val movies = getMovies()
        return movies.firstOrNull { it.id == id } ?: throw Exception("Movie not found")
    }

    suspend fun getShows(movieId: Int? = null): List<ShowDto> {
        val query = firestore.collection("shows").whereEqualTo("isActive", true)
        val snap = query.get().await()

        val allShows = snap.documents.mapIndexed { index, doc ->
            val startTime = doc.getTimestamp("startTime")?.toDate() ?: Date()
            val endTime = doc.getTimestamp("endTime")?.toDate() ?: Date(System.currentTimeMillis() + 7200000)
            ShowDto(
                id = doc.getLong("legacyId")?.toInt() ?: (index + 1),
                movieId = doc.getLong("movieId")?.toInt() ?: 1,
                screenId = doc.getLong("screenId")?.toInt() ?: 1,
                startTime = isoFormat.format(startTime),
                endTime = isoFormat.format(endTime),
                basePrice = doc.getDouble("basePrice") ?: 150.0,
                isActive = doc.getBoolean("isActive") ?: true,
                movie = null,
                documentId = doc.id,
                screen = ScreenDto(
                    id = 1,
                    name = doc.getString("screenName") ?: "Screen 1",
                    displaySpec = "4K Ultra HD",
                    audioSpec = "13-Channel Dolby Atmos",
                    hasDolbyAtmos = true,
                    totalSeats = 98,
                    isActive = true,
                    documentId = doc.getString("screenId") ?: "screen_1",
                )
            )
        }

        return if (movieId != null) allShows.filter { it.movieId == movieId } else allShows
    }

    suspend fun getShowSeats(showId: Int): ShowDetailsWithSeatsDto {
        val shows = getShows()
        val show = shows.firstOrNull { it.id == showId } ?: throw Exception("Show not found")

        // Fetch seat documents from shows/{showId}/seats
        val snap = firestore.collection("shows")
            .document(showId.toString())
            .collection("seats")
            .orderBy("rowLabel")
            .orderBy("seatNumber")
            .get()
            .await()

        val seats = snap.documents.mapIndexed { index, doc ->
            val row = doc.getString("rowLabel") ?: "A"
            val num = doc.getLong("seatNumber")?.toInt() ?: (index + 1)
            val code = doc.getString("seatCode") ?: "$row$num"
            val status = doc.getString("status") ?: "AVAILABLE"
            val price = doc.getDouble("price") ?: show.basePrice

            ShowSeatDto(
                id = index + 1,
                showId = showId,
                seatId = index + 1,
                rowLabel = row,
                seatNumber = num,
                seatCode = code,
                seatType = doc.getString("seatType") ?: "NORMAL",
                status = status,
                price = price,
            )
        }

        return ShowDetailsWithSeatsDto(
            show = show,
            seats = seats
        )
    }

    // ─── SNACKS ───────────────────────────────────────────────────────────────

    suspend fun getSnacks(): List<SnackDto> {
        val snap = firestore.collection("snacks")
            .whereEqualTo("isAvailable", true)
            .get()
            .await()

        return snap.documents.mapIndexed { index, doc ->
            SnackDto(
                id = doc.getLong("legacyId")?.toInt() ?: (index + 1),
                name = doc.getString("name") ?: "",
                category = doc.getString("category") ?: "POPCORN",
                price = doc.getDouble("price") ?: 100.0,
                imageUrl = doc.getString("imageUrl"),
                isAvailable = doc.getBoolean("isAvailable") ?: true,
                description = doc.getString("description"),
                documentId = doc.id,
            )
        }
    }

    // ─── THEATRE CONFIGURATION ────────────────────────────────────────────────

    suspend fun getPaymentConfig(): PaymentConfigDto {
        val doc = firestore.collection("theatreConfig").document("payment").get().await()
        return PaymentConfigDto(
            upiId = doc.getString("upiId") ?: "rnpscinema@upi",
            payeeName = doc.getString("payeeName") ?: "RNPS Home Cinema",
        )
    }
}
