package com.cineplex.app.ui.screens.ticket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalTicketScreen(
    onNavigateBack: () -> Unit,
    onEditSnacks: (Int) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: FinalTicketViewModel = hiltViewModel(),
) {
    val bookingState by viewModel.bookingState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val paymentConfig by viewModel.paymentConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Final Ticket & Summary", style = CinemaTypography.headlineSmall) },
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
            when (val state = bookingState) {
                is UiState.Loading -> LoadingStateView(message = "Loading ticket details...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadBooking() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadBooking() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = "Ticket details unavailable.",
                    onRetry = { viewModel.loadBooking() }
                )
                is UiState.Success -> {
                    FinalTicketContent(
                        booking = state.data,
                        paymentConfig = paymentConfig,
                        onEditSnacks = { onEditSnacks(state.data.id) },
                        onSubmitPayment = { viewModel.submitPayment() },
                        isSubmittingPayment = paymentState is UiState.Loading,
                        paymentError = (paymentState as? UiState.Error)?.message,
                        onDone = onNavigateToHome
                    )
                }
            }
        }
    }
}

@Composable
fun FinalTicketContent(
    booking: BookingDto,
    paymentConfig: com.cineplex.app.data.model.PaymentConfigDto?,
    onEditSnacks: () -> Unit,
    onSubmitPayment: () -> Unit,
    isSubmittingPayment: Boolean,
    paymentError: String?,
    onDone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPaymentSubmitted = booking.payment != null &&
            (booking.payment.status == "SUBMITTED" || booking.payment.status == "PAID" || booking.payment.status == "COMPLETED")
    val isPaid = booking.payment?.status == "PAID" || booking.payment?.status == "COMPLETED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Premium Cinema Ticket Card ───────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RNPS HOME CINEMA",
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = if (isPaid) "Official E-Ticket" else if (isPaymentSubmitted) "Payment Verification Pending" else "Booking Summary",
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    val badgeColor = when {
                        isPaid -> SuccessGreen
                        isPaymentSubmitted -> WarningAmber
                        else -> CinemaGold
                    }
                    val badgeText = when {
                        isPaid -> "CONFIRMED / PAID"
                        isPaymentSubmitted -> "PAYMENT PENDING"
                        else -> "UNPAID"
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.18f),
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

                Spacer(modifier = Modifier.height(18.dp))

                // Movie Poster & Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.cineplex.app.ui.components.CinemaImage(
                        imageUrl = booking.show?.movie?.posterUrl,
                        contentDescription = booking.show?.movie?.title,
                        fallbackIcon = Icons.Default.Movie,
                        modifier = Modifier
                            .size(width = 85.dp, height = 125.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booking.show?.movie?.title ?: "RNPS Cinema Movie",
                            style = CinemaTypography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val timeStr = booking.show?.startTime?.replace("T", " ")?.take(16) ?: "Scheduled Show"
                        Text(
                            text = "Date & Time: $timeStr",
                            style = CinemaTypography.bodySmall,
                            color = CinemaGold
                        )

                        booking.show?.movie?.durationMinutes?.let { dur ->
                            Text(
                                text = "Runtime: $dur minutes",
                                style = CinemaTypography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val screen = booking.show?.screen
                        if (screen != null) {
                            Text(
                                text = "${screen.name} • ${screen.displaySpec}",
                                style = CinemaTypography.labelSmall.copy(color = TextPrimary)
                            )
                            if (screen.hasDolbyAtmos) {
                                Surface(
                                    color = CinemaGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "DOLBY ATMOS",
                                        style = CinemaTypography.labelSmall.copy(
                                            color = CinemaGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DashedDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Seats & Ticket Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        TicketLabel("SEATS")
                        val seatCodes = booking.seats.joinToString(", ") { it.seatCode }
                        TicketValue(if (seatCodes.isNotBlank()) seatCodes else "N/A")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TicketLabel("TICKETS SUB-TOTAL")
                        TicketValue("₹${String.format("%.2f", booking.ticketAmount.ifZero(booking.totalAmount))}")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Snacks Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TicketLabel("SNACKS & BEVERAGES")
                    if (!isPaymentSubmitted) {
                        IconButton(onClick = onEditSnacks, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Snacks", tint = CinemaGold)
                        }
                    }
                }

                if (booking.snacks.isEmpty()) {
                    Text("No snacks added", style = CinemaTypography.bodySmall, color = TextDisabled)
                } else {
                    booking.snacks.forEach { snack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${snack.quantity}x ${snack.snackName}",
                                style = CinemaTypography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "₹${String.format("%.2f", snack.totalPrice)}",
                                style = CinemaTypography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Snacks Total: ₹${String.format("%.2f", booking.snacksAmount)}",
                            style = CinemaTypography.labelSmall.copy(color = CinemaGold, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DashedDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Booking Reference / Ticket Code & Payable Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        TicketLabel("BOOKING REF")
                        Text(
                            text = booking.bookingReference,
                            style = CinemaTypography.headlineSmall,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        booking.ticket?.ticketCode?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Ticket Code: $it", style = CinemaTypography.labelSmall.copy(color = TextSecondary))
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        TicketLabel(if (isPaid) "TOTAL PAID" else "PAYABLE TOTAL")
                        Text(
                            text = "₹${String.format("%.2f", booking.totalAmount)}",
                            style = CinemaTypography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // QR Code Frame for confirmed / active tickets
                if (isPaid || booking.ticket != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    DashedDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(2.dp, CinemaGold, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Ticket QR Code",
                                    tint = Color.Black,
                                    modifier = Modifier.size(80.dp)
                                )
                                Text(
                                    text = booking.ticket?.ticketCode ?: booking.bookingReference,
                                    style = CinemaTypography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan at Cinema Entrance",
                            style = CinemaTypography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Payment Section ──────────────────────────────────────────────────
        if (!isPaymentSubmitted) {
            val upiId = paymentConfig?.upiId ?: "rnpscinema@upi"
            val payeeName = paymentConfig?.payeeName ?: "RNPS Home Cinema"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.4f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Google Pay / UPI Payment",
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Pay using GPay or any UPI App",
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = BackgroundElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Payee: $payeeName",
                                style = CinemaTypography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "UPI ID: $upiId",
                                style = CinemaTypography.labelSmall.copy(color = CinemaGold)
                            )
                            Text(
                                text = "Total Payable: ₹${String.format("%.2f", booking.totalAmount)}",
                                style = CinemaTypography.titleMedium.copy(color = CinemaGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Launch GPay / UPI Intent
                    CinemaButton(
                        text = "PAY NOW WITH GPAY / UPI",
                        onClick = {
                            launchUpiPaymentIntent(
                                context = context,
                                upiId = upiId,
                                payeeName = payeeName,
                                amount = booking.totalAmount,
                                transactionRef = booking.bookingReference
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. I PAID Button (sets payment status to PENDING)
                    OutlinedButton(
                        onClick = onSubmitPayment,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaGold),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CinemaGold)
                    ) {
                        if (isSubmittingPayment) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CinemaGold, strokeWidth = 2.dp)
                        } else {
                            Text("I HAVE PAID (SUBMIT PAYMENT)", style = CinemaTypography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (paymentError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(paymentError, color = ErrorRed, style = CinemaTypography.bodySmall)
                    }
                }
            }
        } else {
            // Submitted / Confirmed State Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isPaid) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = if (isPaid) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isPaid) "Ticket Confirmed!" else "Payment Status: PENDING",
                        style = CinemaTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isPaid)
                            "Your payment has been verified and confirmed by Admin. Enjoy your movie at RNPS Home Cinema!"
                        else
                            "Your payment is currently PENDING Admin confirmation. Once Admin approves, your ticket becomes fully active!",
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CinemaButton(
            text = "Return to Home",
            onClick = onDone
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TicketLabel(text: String) {
    Text(
        text = text,
        style = CinemaTypography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp)
    )
}

@Composable
fun TicketValue(text: String) {
    Text(
        text = text,
        style = CinemaTypography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
    )
}

@Composable
fun DashedDivider(color: Color = Divider) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

fun Double.ifZero(fallback: Double): Double = if (this == 0.0) fallback else this

fun launchUpiPaymentIntent(
    context: android.content.Context,
    upiId: String,
    payeeName: String,
    amount: Double,
    transactionRef: String,
) {
    val uri = android.net.Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", payeeName)
        .appendQueryParameter("tr", transactionRef)
        .appendQueryParameter("tn", "RNPS Cinema Ticket $transactionRef")
        .appendQueryParameter("am", String.format(java.util.Locale.US, "%.2f", amount))
        .appendQueryParameter("cu", "INR")
        .build()

    val gpayIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.n261")
    }

    try {
        context.startActivity(gpayIntent)
    } catch (e: Exception) {
        val chooserIntent = android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_VIEW, uri),
            "Pay with UPI App"
        )
        try {
            context.startActivity(chooserIntent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "No UPI app available on device", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
