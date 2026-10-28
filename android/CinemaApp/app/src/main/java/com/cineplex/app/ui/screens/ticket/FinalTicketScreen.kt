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
import androidx.compose.runtime.setValue
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

    val isPaymentSubmitted = (bookingState as? UiState.Success)?.data?.let { b ->
        b.payment != null && (b.payment.status == "SUBMITTED" || b.payment.status == "PAID" || b.payment.status == "COMPLETED")
    } ?: false

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadBooking(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.activity.compose.BackHandler {
        if (isPaymentSubmitted) {
            onNavigateToHome()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.final_ticket_summary), style = CinemaTypography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isPaymentSubmitted) {
                            onNavigateToHome()
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
                        onSubmitPayment = { ref -> viewModel.submitPayment(ref) },
                        isSubmittingPayment = paymentState is UiState.Loading,
                        paymentError = (paymentState as? UiState.Error)?.message,
                        onDone = {
                            viewModel.cancelBookingAndFinish(onNavigateToHome)
                        }
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
    onSubmitPayment: (String?) -> Unit,
    isSubmittingPayment: Boolean,
    paymentError: String?,
    onDone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showConfirmDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var txnRefInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.confirm_payment_title),
                    style = CinemaTypography.headlineSmall,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.confirm_payment_desc),
                        style = CinemaTypography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = txnRefInput,
                        onValueChange = { txnRefInput = it },
                        placeholder = { Text(booking.bookingReference, color = TextSecondary.copy(alpha = 0.5f)) },
                        label = { Text(stringResource(id = R.string.txn_ref_placeholder)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ref = txnRefInput.trim().ifEmpty { booking.bookingReference }
                        showConfirmDialog = false
                        onSubmitPayment(ref)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.submit_payment),
                        color = CinemaGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = BackgroundCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
    val isPaymentSubmitted = booking.payment != null &&
            (booking.payment.status == "SUBMITTED" || booking.payment.status == "PAID" || booking.payment.status == "COMPLETED")
    val isPaid = booking.payment?.status == "PAID" || booking.payment?.status == "COMPLETED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Luxurious Cinema Ticket Card ─────────────────────────────────────
        com.cineplex.app.ui.components.CinemaTicketCard(
            booking = booking,
            onEditSnacks = onEditSnacks,
            showQrCode = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Payment Section ──────────────────────────────────────────────────
        if (!isPaymentSubmitted) {
            val upiId = paymentConfig?.upiId ?: "rnpscinema@upi"
            val payeeName = paymentConfig?.payeeName ?: "RNPS Home Cinema"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
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
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.payee_label, payeeName),
                                style = CinemaTypography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = stringResource(id = R.string.upi_id_label, upiId),
                                style = CinemaTypography.labelSmall.copy(color = CinemaGold, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stringResource(id = R.string.total_payable_label, String.format(java.util.Locale.ENGLISH, "%.2f", booking.totalAmount)),
                                style = CinemaTypography.titleMedium.copy(color = CinemaGold, fontWeight = FontWeight.Black),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Launch UPI Intent (Cinema Gold Action Button)
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
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
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

        Spacer(modifier = Modifier.height(20.dp))

        CinemaButton(
            text = stringResource(id = R.string.return_to_home),
            onClick = onDone
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
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
