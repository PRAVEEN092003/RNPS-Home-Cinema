package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.CinemaImage
import com.cineplex.app.ui.components.CinemaTextField
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminBookingsViewModel = hiltViewModel(),
) {
    val bookingsState by viewModel.bookingsState.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val selectedPaymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBookingDetail by viewModel.selectedBookingDetail.collectAsState()

    val statusFilters = listOf(
        "ALL" to "All Bookings",
        "CONFIRMED" to "Confirmed",
        "PENDING" to "Pending",
        "CANCELLED" to "Cancelled"
    )

    val paymentFilters = listOf(
        "ALL" to "All Payments",
        "SUBMITTED" to "Action Needed (Submitted)",
        "PAID" to "Paid",
        "PENDING" to "Pending",
        "FAILED" to "Failed"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customer Bookings",
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
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
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Bookings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Search Bar ───────────────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CinemaTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = "Search by Booking #, Name, or Email...",
                    leadingIcon = Icons.Default.Search
                )
            }

            // ─── Status Filter Tabs ───────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = statusFilters.indexOfFirst { it.first == selectedStatusFilter }.coerceAtLeast(0),
                containerColor = BackgroundDeep,
                contentColor = CinemaGold,
                edgePadding = 16.dp
            ) {
                statusFilters.forEach { (key, label) ->
                    Tab(
                        selected = selectedStatusFilter == key,
                        onClick = { viewModel.setStatusFilter(key) },
                        text = {
                            Text(
                                text = label,
                                style = CinemaTypography.labelMedium,
                                color = if (selectedStatusFilter == key) Color.White else TextSecondary,
                                fontWeight = if (selectedStatusFilter == key) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ─── Payment Status Filter Chips ──────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paymentFilters) { (key, label) ->
                    val isSelected = selectedPaymentFilter == key
                    val isActionNeeded = key == "SUBMITTED"
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPaymentFilter(key) },
                        label = {
                            Text(
                                text = label,
                                style = CinemaTypography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isActionNeeded) Color(0xFFFFA000).copy(alpha = 0.15f) else BackgroundCard,
                            labelColor = if (isSelected) CinemaGoldLight else if (isActionNeeded) Color(0xFFFFA000) else TextSecondary,
                            selectedContainerColor = CinemaGold.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isActionNeeded) Color(0xFFFFA000).copy(alpha = 0.6f) else BorderSubtle,
                            selectedBorderColor = CinemaGold,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // ─── Bookings List ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                when (val state = bookingsState) {
                    is UiState.Loading -> LoadingStateView(message = "Loading bookings...")
                    is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadBookings() })
                    is UiState.Empty -> ErrorStateView(message = "No matching bookings found", onRetry = { viewModel.loadBookings() })
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No bookings found matching current filters.",
                                    style = CinemaTypography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(state.data) { booking ->
                                    AdminBookingCard(
                                        booking = booking,
                                        onClick = { viewModel.selectBooking(booking) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }

                selectedBookingDetail?.let { booking ->
                    BookingDetailDialog(
                        booking = booking,
                        onDismiss = { viewModel.selectBooking(null) }
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
    val paymentStatus = booking.payment?.status?.uppercase() ?: "PENDING"
    val isActionNeeded = paymentStatus == "SUBMITTED"
    val isPaid = paymentStatus == "PAID" || paymentStatus == "COMPLETED" || booking.status == "CONFIRMED"
    val isCancelled = booking.status == "CANCELLED"

    val customerName = booking.user?.name ?: "Customer #${booking.userId}"
    val customerEmail = booking.user?.email ?: ""

    val borderColor = when {
        isCancelled -> ErrorRed.copy(alpha = 0.5f)
        isActionNeeded -> Color(0xFFFFA000)
        isPaid -> SuccessGreen.copy(alpha = 0.6f)
        else -> CardBorderGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(if (isActionNeeded) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.bookingReference,
                        style = CinemaTypography.titleMedium.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Booked on: ${DateTimeUtils.formatToIstDateTime(booking.createdAt)}",
                        style = CinemaTypography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isActionNeeded) {
                        Surface(
                            color = Color(0xFFFFA000).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "VERIFY PAYMENT",
                                style = CinemaTypography.labelSmall.copy(
                                    color = Color(0xFFFFA000),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val (paymentBadgeText, paymentBadgeColor) = when (paymentStatus) {
                        "PAID", "COMPLETED" -> "PAID" to SuccessGreen
                        "SUBMITTED" -> "SUBMITTED" to Color(0xFFFFA000)
                        "FAILED" -> "FAILED" to ErrorRed
                        "REFUNDED" -> "REFUNDED" to Color(0xFF9E9E9E)
                        else -> "PENDING" to WarningAmber
                    }

                    Surface(
                        color = paymentBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = paymentBadgeText,
                            style = CinemaTypography.labelSmall.copy(
                                color = paymentBadgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Info Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CinemaGoldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = customerName,
                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (customerEmail.isNotBlank()) {
                    Text(
                        text = " • $customerEmail",
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Movie & Screen
            Text(
                text = "${booking.show?.movie?.title ?: "Movie"} — ${booking.show?.screen?.name ?: "Screen"}",
                style = CinemaTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = CinemaGoldLight
            )

            // Show Schedule vs Booking Creation distinction
            val showTimeStr = booking.show?.startTime?.let { DateTimeUtils.formatToIstDateTime(it) }
                ?: booking.show?.startTime?.replace("T", " ")?.take(16)
                ?: "N/A"
            Text(
                text = "Show Time (IST): $showTimeStr",
                style = CinemaTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = ElectricBlueLight
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Seats & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val seatList = booking.seats.joinToString { it.seatCode }
                Text(
                    text = "Seats (${booking.totalSeats}): ${if (seatList.isNotBlank()) seatList else "N/A"}",
                    style = CinemaTypography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Total: ₹${String.format(Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(22.dp),
            color = BackgroundElevated,
            border = BorderStroke(1.5.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Booking Details",
                            style = CinemaTypography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = booking.bookingReference,
                            style = CinemaTypography.titleSmall.copy(
                                color = CinemaGold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ─── Section A: Customer Information ─────────────────────────
                    SectionCard(title = "Customer Information", icon = Icons.Default.Person) {
                        DetailItem("Full Name", booking.user?.name ?: "N/A")
                        DetailItem("Registered Email", booking.user?.email ?: "N/A")
                        DetailItem("Phone Number", booking.user?.phone ?: "Not collected")
                        DetailItem("User / Customer ID", "#${booking.user?.id ?: booking.userId}")
                        DetailItem("Booking Reference", booking.bookingReference, isMonospace = true)
                    }

                    // ─── Section B: Booking Information ──────────────────────────
                    SectionCard(title = "Booking Information", icon = Icons.Default.ConfirmationNumber) {
                        DetailItem("Booking Created (IST)", DateTimeUtils.formatToIstDateTime(booking.createdAt))
                        DetailItem("Booking Status", booking.status)
                        if (booking.status == "CANCELLED" || !booking.cancelledAt.isNullOrBlank()) {
                            DetailItem(
                                "Cancelled At (IST)",
                                DateTimeUtils.formatToIstDateTime(booking.cancelledAt),
                                highlightColor = ErrorRed
                            )
                        }
                        DetailItem("Screen / Theatre", booking.show?.screen?.name ?: "N/A")
                        DetailItem("Tickets Count", "${booking.totalSeats} seats")
                        val seatCodes = booking.seats.joinToString(", ") { it.seatCode }
                        DetailItem("Seat Identifiers", if (seatCodes.isNotBlank()) seatCodes else "N/A")

                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                        DetailItem("Ticket Subtotal", "₹${String.format(Locale.ENGLISH, "%.2f", booking.ticketAmount)}")
                        DetailItem("Convenience Fee", "₹${String.format(Locale.ENGLISH, "%.2f", booking.convenienceFee)}")

                        if (booking.snacks.isNotEmpty()) {
                            Text(
                                text = "Snacks & Refreshments:",
                                style = CinemaTypography.labelSmall.copy(color = CinemaGoldLight, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            booking.snacks.forEach { snack ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${snack.quantity}x ${snack.snackName} (@ ₹${String.format(Locale.ENGLISH, "%.2f", snack.unitPrice)})",
                                        style = CinemaTypography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.2f", snack.totalPrice)}",
                                        style = CinemaTypography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                            DetailItem("Snacks Subtotal", "₹${String.format(Locale.ENGLISH, "%.2f", booking.snacksAmount)}")
                        }

                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                        DetailItem(
                            "Final Booking Total",
                            "₹${String.format(Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                            highlightColor = Color.White,
                            isBold = true
                        )
                    }

                    // ─── Section C: Movie & Show Information ─────────────────────
                    SectionCard(title = "Movie & Show Information", icon = Icons.Default.Movie) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            CinemaImage(
                                imageUrl = booking.show?.movie?.posterUrl,
                                contentDescription = booking.show?.movie?.title,
                                fallbackIcon = Icons.Default.Movie,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking.show?.movie?.title ?: "N/A",
                                    style = CinemaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                val formatSpec = listOfNotNull(
                                    booking.show?.movie?.language,
                                    booking.show?.screen?.displaySpec
                                ).joinToString(" • ")
                                if (formatSpec.isNotBlank()) {
                                    Text(
                                        text = formatSpec,
                                        style = CinemaTypography.labelSmall.copy(color = CinemaGoldLight),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Movie ID: #${booking.show?.movie?.id ?: "N/A"} • Show ID: #${booking.showId}",
                                    style = CinemaTypography.labelSmall.copy(color = TextSecondary),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 6.dp))

                        DetailItem("Screen / Hall", booking.show?.screen?.name ?: "N/A")
                        val showStartIso = booking.show?.startTime
                        DetailItem("Show Start (IST)", DateTimeUtils.formatToIstDateTime(showStartIso), highlightColor = ElectricBlueLight)
                        if (!booking.show?.endTime.isNullOrBlank()) {
                            DetailItem("Show End (IST)", DateTimeUtils.formatToIstDateTime(booking.show.endTime))
                        }
                    }

                    // ─── Section D: Payment Information ──────────────────────────
                    SectionCard(title = "Payment Tracking", icon = Icons.Default.Payment) {
                        val payment = booking.payment
                        val paymentStatus = payment?.status ?: "PENDING"
                        val (pColor, _) = when (paymentStatus) {
                            "PAID", "COMPLETED" -> SuccessGreen to "PAID"
                            "SUBMITTED" -> Color(0xFFFFA000) to "SUBMITTED"
                            "FAILED" -> ErrorRed to "FAILED"
                            else -> WarningAmber to "PENDING"
                        }

                        DetailItem("Payment Status", paymentStatus, highlightColor = pColor, isBold = true)
                        DetailItem("Payment Method", payment?.method ?: "UPI")
                        DetailItem("Transaction / Ref ID", payment?.transactionId ?: "N/A", isMonospace = true)
                        DetailItem("Submitted Date & Time", DateTimeUtils.formatToIstDateTime(payment?.createdAt))
                        if (!payment?.verifiedAt.isNullOrBlank()) {
                            DetailItem("Verified Date & Time", DateTimeUtils.formatToIstDateTime(payment.verifiedAt))
                        }
                        if (!payment?.verifiedByName.isNullOrBlank() || payment?.verifiedById != null) {
                            DetailItem(
                                "Verified / Actioned By",
                                payment.verifiedByName ?: "Admin #${payment.verifiedById}"
                            )
                        }
                        if (!payment?.remarks.isNullOrBlank()) {
                            DetailItem("Remarks / Audit Notes", payment.remarks)
                        }
                        DetailItem(
                            "Amount Submitted / Expected",
                            "₹${String.format(Locale.ENGLISH, "%.2f", payment?.amount ?: booking.totalAmount)}"
                        )

                        if (booking.ticket != null) {
                            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                            DetailItem("Generated Ticket Code", booking.ticket.ticketCode, isMonospace = true, highlightColor = SuccessGreen)
                            DetailItem("Ticket Admitted / Used", if (booking.ticket.isUsed) "YES" else "NO")
                            if (booking.ticket.isUsed && !booking.ticket.usedAt.isNullOrBlank()) {
                                DetailItem("Check-In Timestamp", DateTimeUtils.formatToIstDateTime(booking.ticket.usedAt))
                            }
                        }
                    }
                }

                // Footer Close Button
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CinemaGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = CinemaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
            content()
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    highlightColor: Color? = null,
    isBold: Boolean = false,
    isMonospace: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = CinemaTypography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = value,
            style = CinemaTypography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            color = highlightColor ?: TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.56f)
        )
    }
}
