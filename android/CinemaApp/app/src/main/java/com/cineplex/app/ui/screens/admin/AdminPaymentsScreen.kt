package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.EmptyStateView
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

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
                            items(state.data, key = { it.id }) { booking ->
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.bookingReference,
                    style = CinemaTypography.titleMedium.copy(color = CinemaGold, fontWeight = FontWeight.Bold)
                )

                val statusColor = when (booking.payment?.status) {
                    "PAID", "COMPLETED" -> SuccessGreen
                    "FAILED" -> ErrorRed
                    else -> WarningAmber
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = booking.payment?.status ?: "PENDING",
                        style = CinemaTypography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = booking.show?.movie?.title ?: "Movie",
                style = CinemaTypography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            val seatsStr = booking.seats.joinToString(", ") { it.seatCode }
            Text(
                text = "${booking.show?.screen?.name ?: "Screen"} • Seats: $seatsStr",
                style = CinemaTypography.bodyMedium,
                color = TextSecondary
            )

            if (booking.snacks.isNotEmpty()) {
                val snacksStr = booking.snacks.joinToString(", ") { "${it.quantity}x ${it.snackName}" }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Snacks: $snacksStr",
                    style = CinemaTypography.bodySmall,
                    color = CinemaGold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            booking.payment?.transactionId?.let { txn ->
                Text(
                    text = "TXN Ref: $txn (${booking.payment.method ?: "UPI"})",
                    style = CinemaTypography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Amount", style = CinemaTypography.bodySmall, color = TextSecondary)
                    Text(
                        text = "₹${String.format("%.2f", booking.totalAmount)}",
                        style = CinemaTypography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (booking.payment?.status != "PAID" && booking.payment?.status != "COMPLETED" && booking.payment?.status != "FAILED") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed.copy(alpha = 0.2f),
                                contentColor = ErrorRed
                            ),
                            enabled = !isActionLoading
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }

                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = TextPrimary
                            ),
                            enabled = !isActionLoading
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
