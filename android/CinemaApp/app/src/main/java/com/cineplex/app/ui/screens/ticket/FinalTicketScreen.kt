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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
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
                title = { Text(stringResource(id = R.string.final_ticket_summary), style = CinemaTypography.headlineSmall) },
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
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadBooking() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadBooking() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = stringResource(id = R.string.ticket_details_unavailable),
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
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = if (isPaid) {
                                stringResource(id = R.string.official_e_ticket)
                            } else if (isPaymentSubmitted) {
                                stringResource(id = R.string.payment_verification_pending)
                            } else {
                                stringResource(id = R.string.booking_summary)
                            },
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
                        isPaid -> stringResource(id = R.string.status_confirmed_paid)
                        isPaymentSubmitted -> stringResource(id = R.string.status_payment_pending)
                        else -> stringResource(id = R.string.status_unpaid)
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
                            text = booking.show?.movie?.title ?: stringResource(id = R.string.app_name),
                            style = CinemaTypography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val timeStr = DateTimeUtils.formatShowDateTime(booking.show?.startTime)
                        Text(
                            text = if (timeStr.isNotBlank()) stringResource(id = R.string.date_time_label, timeStr) else "",
                            style = CinemaTypography.bodySmall,
                            color = CinemaGold
                        )

                        booking.show?.movie?.durationMinutes?.let { dur ->
                            Text(
                                text = stringResource(id = R.string.runtime_format, dur),
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
                                        text = stringResource(id = R.string.dolby_atmos),
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
                        TicketLabel(stringResource(id = R.string.label_seats))
                        val seatCodes = booking.seats.joinToString(", ") { it.seatCode }
                        TicketValue(if (seatCodes.isNotBlank()) seatCodes else "N/A")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TicketLabel(stringResource(id = R.string.label_tickets_subtotal))
                        val subtotal = if (booking.ticketAmount > 0.0) booking.ticketAmount else booking.seats.sumOf { it.price }
                        TicketValue("₹${String.format(java.util.Locale.ENGLISH, "%.2f", subtotal)}")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Snacks Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TicketLabel(stringResource(id = R.string.label_snacks_beverages))
                    if (!isPaymentSubmitted) {
                        IconButton(onClick = onEditSnacks, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_snacks), tint = CinemaGold)
                        }
                    }
                }

                if (booking.snacks.isEmpty()) {
                    Text(stringResource(id = R.string.no_snacks_added), style = CinemaTypography.bodySmall, color = TextDisabled)
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
                                text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", snack.totalPrice)}",
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
                            text = stringResource(id = R.string.snacks_total_format, String.format(java.util.Locale.ENGLISH, "%.2f", booking.snacksAmount)),
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
                        TicketLabel(stringResource(id = R.string.label_booking_ref))
                        Text(
                            text = booking.bookingReference,
                            style = CinemaTypography.headlineSmall,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        booking.ticket?.ticketCode?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.ticket_code_label, it),
                                style = CinemaTypography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        TicketLabel(if (isPaid) stringResource(id = R.string.total_paid) else stringResource(id = R.string.payable_total))
                        Text(
                            text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", booking.totalAmount)}",
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
                                    contentDescription = stringResource(id = R.string.ticket_qr_code),
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
                            text = stringResource(id = R.string.scan_cinema_entrance),
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
                        text = stringResource(id = R.string.upi_payment_title),
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = R.string.upi_payment_subtitle),
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
                                text = stringResource(id = R.string.payee_label, payeeName),
                                style = CinemaTypography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = stringResource(id = R.string.upi_id_label, upiId),
                                style = CinemaTypography.labelSmall.copy(color = CinemaGold)
                            )
                            Text(
                                text = stringResource(id = R.string.total_payable_label, String.format(java.util.Locale.ENGLISH, "%.2f", booking.totalAmount)),
                                style = CinemaTypography.titleMedium.copy(color = CinemaGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Launch GPay / UPI Intent
                    CinemaButton(
                        text = stringResource(id = R.string.btn_pay_now_upi),
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
                            Text(
                                text = stringResource(id = R.string.btn_i_have_paid),
                                style = CinemaTypography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
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
                        text = if (isPaid) stringResource(id = R.string.ticket_confirmed_title) else stringResource(id = R.string.payment_pending_title),
                        style = CinemaTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isPaid) {
                            stringResource(id = R.string.ticket_confirmed_desc)
                        } else {
                            stringResource(id = R.string.payment_pending_desc)
                        },
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CinemaButton(
            text = stringResource(id = R.string.return_to_home),
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
        setPackage("com.google.android.apps.nbu.paisa.user")
    }

    try {
        context.startActivity(gpayIntent)
    } catch (e: Exception) {
        val chooserIntent = android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_VIEW, uri),
            context.getString(R.string.pay_with_upi)
        )
        try {
            context.startActivity(chooserIntent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, context.getString(R.string.no_upi_app), android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
