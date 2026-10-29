package com.cineplex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineplex.app.R
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.QrCodeUtils
import java.util.Locale

@Composable
fun CinemaTicketCard(
    booking: BookingDto,
    modifier: Modifier = Modifier,
    onEditSnacks: (() -> Unit)? = null,
    showQrCode: Boolean = true,
) {
    val isPaid = booking.payment?.status == "PAID" || booking.payment?.status == "COMPLETED" || booking.status == "CONFIRMED"
    val isPaymentSubmitted = booking.payment != null &&
            (booking.payment.status == "SUBMITTED" || booking.payment.status == "PAID" || booking.payment.status == "COMPLETED")
    val isCancelled = booking.status == "CANCELLED" || booking.payment?.status == "FAILED"

    val showDate = DateTimeUtils.formatShowDate(booking.show?.startTime)
    val showTime = DateTimeUtils.formatShowTime(booking.show?.startTime)

    // Outer ticket wrapper
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF202020),
                        Color(0xFF141414),
                        Color(0xFF202020)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = BorderSubtle,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ─── Film-strip Top Sprocket Holes ──────────────────────────────────
            FilmPerforationRow(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp))

            // ─── Ticket Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CinemaGold.copy(alpha = 0.15f))
                            .border(1.dp, CinemaGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = CinemaTypography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
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
                            style = CinemaTypography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                        )
                    }
                }

                // Status Badge
                val badgeColor = when {
                    isPaid -> SuccessGreen
                    isCancelled -> ErrorRed
                    isPaymentSubmitted -> WarningAmber
                    else -> CinemaGold
                }
                val badgeText = when {
                    isPaid -> stringResource(id = R.string.status_confirmed_paid)
                    isCancelled -> stringResource(id = R.string.status_cancelled_rejected)
                    isPaymentSubmitted -> stringResource(id = R.string.status_payment_pending)
                    else -> stringResource(id = R.string.status_unpaid)
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = badgeText,
                        style = CinemaTypography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Movie Poster & Specs Section ───────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                CinemaImage(
                    imageUrl = booking.show?.movie?.posterUrl,
                    contentDescription = booking.show?.movie?.title,
                    fallbackIcon = Icons.Default.Movie,
                    modifier = Modifier
                        .size(width = 92.dp, height = 135.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.show?.movie?.title ?: stringResource(id = R.string.app_name),
                        style = CinemaTypography.headlineSmall.copy(fontSize = 19.sp),
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date & AM/PM Time Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BackgroundElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = showDate,
                                style = CinemaTypography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = CinemaGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = showTime,
                                style = CinemaTypography.labelSmall.copy(
                                    color = CinemaGoldLight,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val screen = booking.show?.screen
                    if (screen != null) {
                        Text(
                            text = "${screen.name} • ${screen.displaySpec}",
                            style = CinemaTypography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        if (screen.hasDolbyAtmos) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = CinemaGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = stringResource(id = R.string.dolby_atmos),
                                    style = CinemaTypography.labelSmall.copy(
                                        color = CinemaGoldLight,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─── Ticket Stub Perforation Divider ────────────────────────────────
            TicketPerforationDivider()

            Spacer(modifier = Modifier.height(18.dp))

            // ─── Seats & Subtotal Section ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = stringResource(id = R.string.label_seats).uppercase(),
                        style = CinemaTypography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val seats = booking.seats
                    if (seats.isEmpty()) {
                        Text("N/A", style = CinemaTypography.titleMedium.copy(color = TextPrimary))
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            seats.forEach { seat ->
                                Surface(
                                    color = CinemaGold.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = seat.seatCode,
                                        style = CinemaTypography.labelSmall.copy(
                                            color = CinemaGoldLight,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(0.8f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(id = R.string.label_tickets_subtotal).uppercase(),
                        style = CinemaTypography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val subtotal = if (booking.ticketAmount > 0.0) booking.ticketAmount else booking.seats.sumOf { it.price }
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", subtotal)}",
                        style = CinemaTypography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // ─── Snacks & Beverages Section ─────────────────────────────────────
            if (booking.snacks.isNotEmpty() || onEditSnacks != null) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.label_snacks_beverages).uppercase(),
                        style = CinemaTypography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                    if (onEditSnacks != null && !isPaymentSubmitted) {
                        IconButton(onClick = onEditSnacks, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.edit_snacks),
                                tint = CinemaGold
                            )
                        }
                    }
                }

                if (booking.snacks.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_snacks_added),
                        style = CinemaTypography.bodySmall,
                        color = TextDisabled,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
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
                                    text = "₹${String.format(Locale.ENGLISH, "%.2f", snack.totalPrice)}",
                                    style = CinemaTypography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Booking Reference & Total Payable ──────────────────────────────
            Surface(
                color = Color(0xFF181410),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.label_booking_ref).uppercase(),
                            style = CinemaTypography.labelSmall.copy(
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = booking.bookingReference,
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPaid) stringResource(id = R.string.total_paid).uppercase() else stringResource(id = R.string.payable_total).uppercase(),
                            style = CinemaTypography.labelSmall.copy(
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                            style = CinemaTypography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }

            // ─── High Contrast Cinema QR Code Section ───────────────────────────
            if (showQrCode && (isPaid || booking.ticket != null)) {
                Spacer(modifier = Modifier.height(18.dp))

                val qrRaw = booking.ticket?.qrData?.takeIf { it.isNotBlank() }
                    ?: booking.ticket?.ticketCode?.takeIf { it.isNotBlank() }
                    ?: booking.bookingReference
                val qrBitmap = remember(qrRaw) {
                    QrCodeUtils.generateQrBitmap(qrRaw, sizePx = 400)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, CinemaGold, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(id = R.string.ticket_qr_code),
                                    modifier = Modifier.size(112.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = stringResource(id = R.string.ticket_qr_code),
                                    tint = Color(0xFF050505),
                                    modifier = Modifier.size(90.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = booking.ticket?.ticketCode ?: booking.bookingReference,
                                style = CinemaTypography.labelSmall.copy(
                                    color = Color(0xFF050505),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.scan_cinema_entrance),
                        style = CinemaTypography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // ─── Film-strip Bottom Sprocket Holes ───────────────────────────────
            Spacer(modifier = Modifier.height(12.dp))
            FilmPerforationRow(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
        }
    }
}

/**
 * Renders ticket side notches and dashed tear-off line.
 */
@Composable
fun TicketPerforationDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        // Dashed divider line across center
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .height(1.dp)
        ) {
            drawLine(
                color = CinemaGold.copy(alpha = 0.6f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            )
        }

        // Left Notch
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
        ) {
            drawCircle(
                color = BackgroundDeep,
                radius = size.width / 2f,
                center = Offset(0f, size.height / 2f)
            )
        }

        // Right Notch
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterEnd)
        ) {
            drawCircle(
                color = BackgroundDeep,
                radius = size.width / 2f,
                center = Offset(size.width, size.height / 2f)
            )
        }
    }
}

/**
 * Film sprocket perforation notches.
 */
@Composable
fun FilmPerforationRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(9) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF080808))
                    .border(0.5.dp, CinemaGold.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    }
}
