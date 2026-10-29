package com.cineplex.app.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.TicketVerificationDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaImage
import com.cineplex.app.ui.components.CinemaTextField
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketVerifyScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminTicketVerifyViewModel = hiltViewModel(),
) {
    val isCameraOpen by viewModel.isCameraOpen.collectAsState()
    val codeInput by viewModel.codeInput.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()
    val checkInState by viewModel.checkInState.collectAsState()

    BackHandler {
        if (isCameraOpen) {
            viewModel.closeCamera()
        } else {
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Ticket QR Verification",
                            style = CinemaTypography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── Prominent Scan Ticket QR Button ───────────────────────────────
                Button(
                    onClick = { viewModel.openCamera() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan Ticket QR Code",
                        style = CinemaTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ─── Divider with OR label ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    Text(
                        text = "OR MANUAL ENTRY",
                        style = CinemaTypography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Manual Input Field ────────────────────────────────────────────
                CinemaTextField(
                    value = codeInput,
                    onValueChange = { viewModel.codeInput.value = it },
                    label = "Ticket Code or Booking Ref (e.g. TKT-... or BKG-...)",
                    leadingIcon = Icons.Default.ConfirmationNumber
                )

                Spacer(modifier = Modifier.height(12.dp))

                CinemaButton(
                    text = "Verify Code",
                    onClick = { viewModel.verifyTicket() },
                    isLoading = verifyState is UiState.Loading,
                    enabled = codeInput.isNotBlank()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Verification Results / Error Display ──────────────────────────
                when (val state = verifyState) {
                    is UiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "VERIFICATION FAILED",
                                    style = CinemaTypography.titleMedium,
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = CinemaTypography.bodyMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        TicketVerificationResultCard(
                            result = state.data,
                            checkInLoading = checkInState is UiState.Loading,
                            onCheckIn = { viewModel.checkInTicket(state.data.ticketCode) },
                            onScanNext = {
                                viewModel.resetVerification()
                                viewModel.openCamera()
                            }
                        )
                    }
                    else -> {}
                }
            }
        }

        // Full Screen Camera Viewfinder
        AnimatedVisibility(
            visible = isCameraOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            QrCameraScanner(
                onQrCodeDetected = { code ->
                    viewModel.onQrScanned(code)
                },
                onClose = { viewModel.closeCamera() }
            )
        }
    }
}

@Composable
fun TicketVerificationResultCard(
    result: TicketVerificationDto,
    checkInLoading: Boolean,
    onCheckIn: () -> Unit,
    onScanNext: () -> Unit,
) {
    val validityStatus = result.validityStatus.uppercase()
    val isUsed = result.isUsed || validityStatus == "ALREADY_USED"

    val (statusLabel, statusColor, statusIcon) = when {
        validityStatus == "ALREADY_USED" || isUsed -> Triple(
            "ALREADY USED / CHECKED IN",
            Color(0xFFFFA000), // Orange
            Icons.Default.Warning
        )
        validityStatus == "VALID" && result.isValid -> Triple(
            "VALID TICKET",
            SuccessGreen,
            Icons.Default.CheckCircle
        )
        validityStatus == "UNPAID" -> Triple(
            "UNPAID / PENDING PAYMENT",
            Color(0xFFFFD54F), // Amber
            Icons.Default.HourglassTop
        )
        validityStatus == "CANCELLED" -> Triple(
            "CANCELLED BOOKING",
            ErrorRed,
            Icons.Default.Cancel
        )
        validityStatus == "EXPIRED" -> Triple(
            "EXPIRED SHOW",
            Color(0xFF9E9E9E), // Grey
            Icons.Default.Schedule
        )
        else -> Triple(
            "INVALID TICKET",
            ErrorRed,
            Icons.Default.Cancel
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ─── Status Header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusLabel,
                        style = CinemaTypography.titleMedium.copy(
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = result.paymentStatus,
                        style = CinemaTypography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Already Used Timestamp Banner
            if (isUsed && !result.usedAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFA000).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admitted at: ${DateTimeUtils.formatToIstDateTime(result.usedAt)}",
                            style = CinemaTypography.labelSmall.copy(
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = BorderSubtle)

            // ─── Movie & Show Details ──────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                CinemaImage(
                    imageUrl = result.moviePosterUrl,
                    contentDescription = result.movieTitle,
                    fallbackIcon = Icons.Default.Movie,
                    modifier = Modifier
                        .size(width = 68.dp, height = 96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.movieTitle,
                        style = CinemaTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )

                    val formatSpec = listOfNotNull(result.language, result.format).joinToString(" • ")
                    if (formatSpec.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSpec,
                            style = CinemaTypography.labelSmall.copy(color = CinemaGoldLight, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.screenName,
                        style = CinemaTypography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val showDateFormatted = result.showDate?.let { DateTimeUtils.formatShowDate(it) } ?: ""
                    Text(
                        text = if (showDateFormatted.isNotBlank()) "$showDateFormatted | ${result.showTime}" else result.showTime,
                        style = CinemaTypography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = BorderSubtle)

            // ─── Customer Details ──────────────────────────────────────────────
            DetailRow(label = "Customer Name", value = result.userName)
            if (!result.customerEmail.isNullOrBlank()) {
                DetailRow(label = "Customer Email", value = result.customerEmail)
            }
            if (!result.customerPhone.isNullOrBlank()) {
                DetailRow(label = "Customer Phone", value = result.customerPhone)
            }
            if (result.customerId != null) {
                DetailRow(label = "Customer ID", value = "#${result.customerId}")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSubtle)

            // ─── Seat & Ticket Info ────────────────────────────────────────────
            val seatCount = if (result.ticketCount > 0) result.ticketCount else result.seats.size
            DetailRow(
                label = "Seats ($seatCount tickets)",
                value = if (result.seats.isNotEmpty()) result.seats.joinToString(", ") else "None allocated"
            )
            DetailRow(label = "Booking Reference", value = result.bookingReference, isMonospace = true)
            DetailRow(label = "Ticket Code", value = result.ticketCode, isMonospace = true)
            DetailRow(label = "Booking Status", value = result.bookingStatus)

            Spacer(modifier = Modifier.height(18.dp))

            // ─── Action Buttons ────────────────────────────────────────────────
            if (result.isValid && !isUsed) {
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White
                    ),
                    enabled = !checkInLoading
                ) {
                    if (checkInLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admit Guest / Check In",
                            style = CinemaTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onScanNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaGoldLight)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Next Ticket",
                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = CinemaTypography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = CinemaTypography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}
