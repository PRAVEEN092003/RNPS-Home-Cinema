package com.cineplex.app.navigation

/**
 * All navigation routes in the app.
 * Single source of truth for route strings.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object MyBookings : Screen("my_bookings")
    data object BookingDetail : Screen("booking_detail/{bookingId}") {
        fun createRoute(bookingId: Int) = "booking_detail/$bookingId"
    }
    data object MovieDetail : Screen("movie/{movieId}") {
        fun createRoute(movieId: Int) = "movie/$movieId"
    }
    data object SeatSelection : Screen("seats/{showId}") {
        fun createRoute(showId: Int) = "seats/$showId"
    }
    data object SnacksSelection : Screen("snacks/{bookingId}") {
        fun createRoute(bookingId: Int) = "snacks/$bookingId"
    }
    data object FinalSummary : Screen("summary/{bookingId}") {
        fun createRoute(bookingId: Int) = "summary/$bookingId"
    }
    data object AdminPayments : Screen("admin_payments")
    data object AdminTicketVerify : Screen("admin_ticket_verify")
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminMovies : Screen("admin_movies")
    data object AdminShows : Screen("admin_shows")
    data object AdminSnacks : Screen("admin_snacks")
    data object AdminBookings : Screen("admin_bookings")
    data object AdminScreens : Screen("admin_screens")
}
