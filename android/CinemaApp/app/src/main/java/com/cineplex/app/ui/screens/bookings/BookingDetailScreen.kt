package com.cineplex.app.ui.screens.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookingDetailViewModel = hiltViewModel(),
) {
    val bookingState by viewModel.bookingState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ticket_details_title), style = CinemaTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = bookingState) {
                is UiState.Loading -> LoadingStateView(message = stringResource(id = R.string.loading_ticket_details))
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadBookingDetail() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadBookingDetail() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = stringResource(id = R.string.ticket_details_not_found),
                    onRetry = { viewModel.loadBookingDetail() }
                )
                is UiState.Success -> {
                    TicketDetailCard(booking = state.data)
                }
            }
        }
    }
}

@Composable
fun TicketDetailCard(booking: BookingDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Premium Ticket Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header: Branding & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(id = R.string.official_e_ticket),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    val isConfirmed = booking.status == "CONFIRMED" || booking.status == "BOOKED"
                    val isCancelled = booking.status == "CANCELLED" || booking.payment?.status == "FAILED"
                    val badgeColor = when {
                        isConfirmed -> SuccessGreen
                        isCancelled -> ErrorRed
                        else -> WarningAmber
                    }
                    val badgeText = when {
                        isConfirmed -> stringResource(id = R.string.status_confirmed_issued)
                        isCancelled -> stringResource(id = R.string.status_cancelled_rejected)
                        else -> stringResource(id = R.string.status_pending_admin)
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = CinemaTypography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Movie Title
                Text(
                    text = booking.show?.movie?.title ?: stringResource(id = R.string.app_name),
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                booking.show?.movie?.genre?.let {
                    Text(text = it, style = CinemaTypography.bodyMedium, color = CinemaGold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Divider(color = Divider, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Details Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        TicketMetaLabel(stringResource(id = R.string.screen_label))
                        TicketMetaValue(booking.show?.screen?.name ?: "Main Hall")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TicketMetaLabel(stringResource(id = R.string.label_seats))
                        TicketMetaValue("${booking.totalSeats}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        TicketMetaLabel(stringResource(id = R.string.date_time_label, "").replace(":", "").trim())
                        val timeStr = DateTimeUtils.formatShowDateTime(booking.show?.startTime)
                        TicketMetaValue(if (timeStr.isNotBlank()) timeStr else "Unspecified")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TicketMetaLabel(stringResource(id = R.string.label_seats))
                        val seatCodes = booking.seats.joinToString(", ") { it.seatCode }
                        TicketMetaValue(if (seatCodes.isNotBlank()) seatCodes else "Unspecified")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Divider, thickness = 1.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // Booking Reference & Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        TicketMetaLabel(stringResource(id = R.string.label_booking_ref))
                        Text(
                            text = booking.bookingReference,
                            style = CinemaTypography.headlineSmall,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        TicketMetaLabel(stringResource(id = R.string.total_amount_label))
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                            style = CinemaTypography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TicketMetaLabel(label: String) {
    Text(
        text = label,
        style = CinemaTypography.labelSmall.copy(
            color = TextSecondary,
            letterSpacing = 1.sp
        )
    )
}

@Composable
fun TicketMetaValue(value: String) {
    Text(
        text = value,
        style = CinemaTypography.titleMedium.copy(
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    )
}
