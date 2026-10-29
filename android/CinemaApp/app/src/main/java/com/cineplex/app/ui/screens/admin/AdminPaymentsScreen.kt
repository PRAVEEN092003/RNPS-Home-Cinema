package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.EmptyStateView
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminPaymentsViewModel = hiltViewModel(),
) {
    val paymentsState by viewModel.paymentsState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Verification", style = CinemaTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPayments() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CinemaGold
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
            when (val state = paymentsState) {
                is UiState.Loading -> LoadingStateView(message = "Loading payment submissions...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadPayments() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadPayments() }
                )
                is UiState.Empty -> EmptyStateView(message = "No payment submissions found.")
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyStateView(message = "No payment submissions found.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.data, key = { it.canonicalId }) { booking ->
                                AdminPaymentCard(
                                    booking = booking,
                                    onApprove = {
                                        booking.payment?.id?.let { pid -> viewModel.verifyPayment(pid, true) }
                                    },
                                    onReject = {
                                        booking.payment?.id?.let { pid -> viewModel.verifyPayment(pid, false) }
                                    },
                                    isActionLoading = actionState is UiState.Loading
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPaymentCard(
    booking: BookingDto,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isActionLoading: Boolean,
) {
    val customerName = booking.user?.name?.takeIf { it.isNotBlank() } ?: "Customer #${booking.userId}"
    val customerEmail = booking.user?.email?.takeIf { it.isNotBlank() } ?: "No email provided"
    val movieTitle = booking.show?.movie?.title?.takeIf { it.isNotBlank() } ?: "Movie"
    val showDateTime = DateTimeUtils.formatShowDateTime(booking.show?.startTime)
    val screenName = booking.show?.screen?.name?.takeIf { it.isNotBlank() } ?: "Screen"
    val seatsStr = booking.seats.map { it.seatCode }.filter { it.isNotBlank() }.let { list ->
        if (list.isEmpty()) "None" else list.joinToString(", ")
    }
    val upiTxnId = booking.payment?.transactionId?.takeIf { it.isNotBlank() } ?: "N/A"
    val paymentSubmissionDateTime = DateTimeUtils.formatToIstDateTime(booking.payment?.createdAt ?: booking.createdAt)
    val paymentStatus = booking.payment?.status ?: "PENDING"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, CardBorderGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Booking Number + Payment Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BOOKING NUMBER",
                        style = CinemaTypography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp)
                    )
                    Text(
                        text = booking.bookingReference,
                        style = CinemaTypography.titleMedium.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                val statusColor = when (paymentStatus) {
                    "PAID", "COMPLETED" -> SuccessGreen
                    "FAILED" -> ErrorRed
                    else -> WarningAmber
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = paymentStatus,
                        style = CinemaTypography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Details Box (Fetched from backend)
            Surface(
                color = BackgroundDeep.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, Divider.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CinemaGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customerName,
                            style = CinemaTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customerEmail,
                                style = CinemaTypography.bodySmall.copy(color = TextSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Movie Title
            Text(
                text = movieTitle,
                style = CinemaTypography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Show Date & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = CinemaGoldLight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showDateTime.isNotBlank()) showDateTime else "Show Time N/A",
                    style = CinemaTypography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Screen & Seats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.EventSeat,
                    contentDescription = null,
                    tint = CinemaGoldLight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$screenName • Seats: $seatsStr",
                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }

            // Snacks if any
            if (booking.snacks.isNotEmpty()) {
                val snacksStr = booking.snacks.joinToString(", ") { "${it.quantity}x ${it.snackName}" }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Snacks: $snacksStr",
                    style = CinemaTypography.bodySmall,
                    color = ElectricBlueLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Submission & UPI Transaction Box
            Surface(
                color = BackgroundDeep.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Divider.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // UPI Transaction ID Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = CinemaGoldLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UPI Transaction ID",
                                style = CinemaTypography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = upiTxnId,
                            style = CinemaTypography.bodySmall.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // Payment Submission Date/Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Submitted Date/Time",
                            style = CinemaTypography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = paymentSubmissionDateTime,
                            style = CinemaTypography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

            // Total Amount & Verification Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Amount", style = CinemaTypography.labelSmall, color = TextSecondary)
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                        style = CinemaTypography.headlineMedium,
                        color = CinemaGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (paymentStatus != "PAID" && paymentStatus != "COMPLETED" && paymentStatus != "FAILED") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed.copy(alpha = 0.2f),
                                contentColor = ErrorRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isActionLoading
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isActionLoading
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
