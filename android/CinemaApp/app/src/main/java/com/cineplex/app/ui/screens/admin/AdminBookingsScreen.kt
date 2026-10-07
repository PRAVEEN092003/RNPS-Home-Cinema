package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminBookingsViewModel = hiltViewModel()
) {
    val bookingsState by viewModel.bookingsState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var selectedBookingDetail by remember { mutableStateOf<BookingDto?>(null) }

    val filters = listOf("ALL" to "All Bookings", "PENDING" to "Pending", "PAID" to "Paid", "CANCELLED" to "Cancelled")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Management", style = CinemaTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDeep,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDeep,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filters.indexOfFirst { it.first == selectedFilter }.coerceAtLeast(0),
                containerColor = BackgroundDeep,
                contentColor = CinemaGold,
                edgePadding = 16.dp
            ) {
                filters.forEach { (key, label) ->
                    Tab(
                        selected = selectedFilter == key,
                        onClick = { viewModel.setFilter(key) },
                        text = {
                            Text(
                                text = label,
                                style = CinemaTypography.labelLarge,
                                fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (val state = bookingsState) {
                    is UiState.Loading -> LoadingStateView(message = "Loading bookings...")
                    is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadBookings() })
                    is UiState.Empty -> ErrorStateView(message = "No bookings found", onRetry = { viewModel.loadBookings() })
                    is UiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.data) { booking ->
                                AdminBookingCard(
                                    booking = booking,
                                    onClick = { selectedBookingDetail = booking }
                                )
                            }
                        }
                    }
                    else -> {}
                }

                selectedBookingDetail?.let { booking ->
                    BookingDetailDialog(
                        booking = booking,
                        onDismiss = { selectedBookingDetail = null }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminBookingCard(
    booking: BookingDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.2f))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.bookingReference,
                    style = CinemaTypography.headlineSmall,
                    color = CinemaGold,
                    fontWeight = FontWeight.Bold
                )

                val (statusText, statusColor) = when (booking.status.uppercase()) {
                    "CONFIRMED" -> "PAID / CONFIRMED" to SuccessGreen
                    "CANCELLED" -> "CANCELLED" to ErrorRed
                    else -> "PENDING" to WarningAmber
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = CinemaTypography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = booking.show?.movie?.title ?: "Movie Booking",
                style = CinemaTypography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${booking.show?.screen?.name ?: "Screen"} • ${booking.show?.startTime?.replace("T", " ")?.take(16) ?: ""}",
                style = CinemaTypography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Seats (${booking.totalSeats}): ${booking.seats.joinToString { it.seatCode }}",
                    style = CinemaTypography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Total: ₹${"%.2f".format(booking.totalAmount)}",
                    style = CinemaTypography.bodyMedium,
                    color = CinemaGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BookingDetailDialog(
    booking: BookingDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Booking ${booking.bookingReference}",
                style = CinemaTypography.headlineSmall,
                color = CinemaGold,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Movie: ${booking.show?.movie?.title ?: "N/A"}", style = CinemaTypography.bodyMedium, color = TextPrimary)
                Text("Screen: ${booking.show?.screen?.name ?: "N/A"}", style = CinemaTypography.bodySmall, color = TextSecondary)
                Text("Show Time: ${booking.show?.startTime?.replace("T", " ")?.take(16) ?: "N/A"}", style = CinemaTypography.bodySmall, color = TextSecondary)

                Divider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))

                Text("Seats: ${booking.seats.joinToString { it.seatCode }}", style = CinemaTypography.bodyMedium, color = TextPrimary)
                if (booking.snacks.isNotEmpty()) {
                    Text("Snacks: ${booking.snacks.joinToString { "${it.snackName} x${it.quantity}" }}", style = CinemaTypography.bodySmall, color = TextSecondary)
                }

                Divider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))

                Text("Ticket Amount: ₹${"%.2f".format(booking.ticketAmount)}", style = CinemaTypography.bodySmall, color = TextSecondary)
                Text("Snacks Amount: ₹${"%.2f".format(booking.snacksAmount)}", style = CinemaTypography.bodySmall, color = TextSecondary)
                Text("Convenience Fee: ₹${"%.2f".format(booking.convenienceFee)}", style = CinemaTypography.bodySmall, color = TextSecondary)
                Text("Total Paid/Payable: ₹${"%.2f".format(booking.totalAmount)}", style = CinemaTypography.bodyMedium, color = CinemaGold, fontWeight = FontWeight.Bold)

                Divider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))

                Text("Payment Status: ${booking.payment?.status ?: "PENDING"}", style = CinemaTypography.bodySmall, color = TextPrimary)
                if (booking.ticket != null) {
                    Text("Ticket Code: ${booking.ticket.ticketCode}", style = CinemaTypography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = BackgroundDeep)
            ) {
                Text("Close")
            }
        },
        containerColor = BackgroundElevated
    )
}
