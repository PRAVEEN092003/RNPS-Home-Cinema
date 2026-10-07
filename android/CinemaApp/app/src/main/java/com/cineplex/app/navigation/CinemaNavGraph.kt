package com.cineplex.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cineplex.app.ui.screens.admin.AdminBookingsScreen
import com.cineplex.app.ui.screens.admin.AdminDashboardScreen
import com.cineplex.app.ui.screens.admin.AdminMoviesScreen
import com.cineplex.app.ui.screens.admin.AdminPaymentsScreen
import com.cineplex.app.ui.screens.admin.AdminScreensScreen
import com.cineplex.app.ui.screens.admin.AdminShowsScreen
import com.cineplex.app.ui.screens.admin.AdminSnacksScreen
import com.cineplex.app.ui.screens.admin.AdminTicketVerifyScreen
import com.cineplex.app.ui.screens.auth.LoginScreen
import com.cineplex.app.ui.screens.auth.RegisterScreen
import com.cineplex.app.ui.screens.bookings.BookingDetailScreen
import com.cineplex.app.ui.screens.bookings.MyBookingsScreen
import com.cineplex.app.ui.screens.home.HomeScreen
import com.cineplex.app.ui.screens.movie.MovieDetailScreen
import com.cineplex.app.ui.screens.profile.ProfileScreen
import com.cineplex.app.ui.screens.seats.BookingConfirmationScreen
import com.cineplex.app.ui.screens.seats.SeatSelectionScreen
import com.cineplex.app.ui.screens.snacks.SnacksScreen
import com.cineplex.app.ui.screens.splash.SplashScreen
import com.cineplex.app.ui.screens.ticket.FinalTicketScreen

@Composable
fun CinemaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToMovieDetail = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                },
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            MovieDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectShow = { showId ->
                    navController.navigate(Screen.SeatSelection.createRoute(showId))
                }
            )
        }

        composable(
            route = Screen.SeatSelection.route,
            arguments = listOf(navArgument("showId") { type = NavType.IntType })
        ) {
            SeatSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookingSuccessWithSnacks = { booking ->
                    navController.navigate(Screen.SnacksSelection.createRoute(booking.id)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onBookingSuccessWithoutSnacks = { booking ->
                    navController.navigate(Screen.FinalSummary.createRoute(booking.id)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.SnacksSelection.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) {
            SnacksScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinueToSummary = { bookingId ->
                    navController.navigate(Screen.FinalSummary.createRoute(bookingId))
                }
            )
        }

        composable(
            route = Screen.FinalSummary.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) {
            FinalTicketScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditSnacks = { bookingId ->
                    navController.navigate(Screen.SnacksSelection.createRoute(bookingId)) {
                        popUpTo(Screen.FinalSummary.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMovies = { navController.navigate(Screen.AdminMovies.route) },
                onNavigateToShows = { navController.navigate(Screen.AdminShows.route) },
                onNavigateToScreens = { navController.navigate(Screen.AdminScreens.route) },
                onNavigateToSnacks = { navController.navigate(Screen.AdminSnacks.route) },
                onNavigateToBookings = { navController.navigate(Screen.AdminBookings.route) },
                onNavigateToPayments = { navController.navigate(Screen.AdminPayments.route) },
                onNavigateToTicketVerify = { navController.navigate(Screen.AdminTicketVerify.route) }
            )
        }

        composable(Screen.AdminMovies.route) {
            AdminMoviesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminShows.route) {
            AdminShowsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminScreens.route) {
            AdminScreensScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminSnacks.route) {
            AdminSnacksScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminBookings.route) {
            AdminBookingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminPayments.route) {
            AdminPaymentsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AdminTicketVerify.route) {
            AdminTicketVerifyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.MyBookings.route) {
            MyBookingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onSelectBooking = { booking ->
                    navController.navigate(Screen.FinalSummary.createRoute(booking.id))
                }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) {
            BookingDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBookings = {
                    navController.navigate(Screen.MyBookings.route)
                },
                onNavigateToAdminDashboard = {
                    navController.navigate(Screen.AdminDashboard.route)
                },
                onNavigateToAdminPayments = {
                    navController.navigate(Screen.AdminPayments.route)
                },
                onNavigateToAdminTicketVerify = {
                    navController.navigate(Screen.AdminTicketVerify.route)
                }
            )
        }
    }
}
