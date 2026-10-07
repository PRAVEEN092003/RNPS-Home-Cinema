package com.cineplex.app.ui.screens.seats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.ShowDetailsWithSeatsDto
import com.cineplex.app.data.model.ShowSeatDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    onNavigateBack: () -> Unit,
    onBookingSuccessWithSnacks: (BookingDto) -> Unit,
    onBookingSuccessWithoutSnacks: (BookingDto) -> Unit,
    viewModel: SeatSelectionViewModel = hiltViewModel(),
) {
    val seatsState by viewModel.seatsState.collectAsState()
    val selectedSeatIds by viewModel.selectedSeatIds.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    var pendingBooking by remember { mutableStateOf<BookingDto?>(null) }

    LaunchedEffect(bookingState) {
        if (bookingState is UiState.Success) {
            val booking = (bookingState as UiState.Success<BookingDto>).data
            viewModel.resetBookingState()
            pendingBooking = booking
        }
    }

    if (pendingBooking != null) {
        AlertDialog(
            onDismissRequest = { /* force choice */ },
            title = { Text("Do you want snacks?", style = CinemaTypography.headlineSmall, color = TextPrimary) },
            text = { Text("Would you like to add delicious snacks & beverages to your movie ticket?", style = CinemaTypography.bodyMedium, color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = pendingBooking!!
                        pendingBooking = null
                        onBookingSuccessWithSnacks(b)
                    }
                ) {
                    Text("YES, ADD SNACKS", color = CinemaGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val b = pendingBooking!!
                        pendingBooking = null
                        onBookingSuccessWithoutSnacks(b)
                    }
                ) {
                    Text("NO, CONTINUE TO SUMMARY", color = TextSecondary)
                }
            },
            containerColor = BackgroundCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Seats", style = CinemaTypography.headlineSmall)
                        Text(
                            text = "RNPS Home Cinema",
                            style = CinemaTypography.bodySmall,
                            color = CinemaGold
                        )
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = seatsState) {
                is UiState.Loading -> LoadingStateView(message = "Loading seat layout...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadSeats() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadSeats() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = "Seat map unavailable.",
                    onRetry = { viewModel.loadSeats() }
                )
                is UiState.Success -> {
                    SeatSelectionContent(
                        data = state.data,
                        selectedSeatIds = selectedSeatIds,
                        onSeatClick = { viewModel.toggleSeatSelection(it) },
                        totalPrice = viewModel.calculateTotal(state.data.seats),
                        onConfirmBooking = { viewModel.confirmBooking() },
                        isBookingLoading = bookingState is UiState.Loading,
                        bookingError = (bookingState as? UiState.Error)?.message
                    )
                }
            }
        }
    }
}

@Composable
fun SeatSelectionContent(
    data: ShowDetailsWithSeatsDto,
    selectedSeatIds: Set<Int>,
    onSeatClick: (ShowSeatDto) -> Unit,
    totalPrice: Double,
    onConfirmBooking: () -> Unit,
    isBookingLoading: Boolean,
    bookingError: String?,
) {
    val seatsByRow = data.seats.groupBy { it.rowLabel }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Info & Screen indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${data.show.movie?.title ?: "Movie"} — ${data.show.screen?.name ?: "Screen"}",
                style = CinemaTypography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            val screen = data.show.screen
            if (screen != null) {
                Text(
                    text = "${screen.displaySpec} • ${screen.audioSpec}",
                    style = CinemaTypography.bodySmall,
                    color = CinemaGold
                )
                if (screen.hasDolbyAtmos) {
                    Surface(
                        color = CinemaGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "DOLBY ATMOS",
                            style = CinemaTypography.labelSmall.copy(color = CinemaGold, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Screen Graphic Curved Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CinemaGold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SCREEN THIS WAY",
                style = CinemaTypography.labelSmall.copy(
                    color = TextDisabled,
                    letterSpacing = 2.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = BackgroundElevated, label = "Available")
            LegendItem(color = CinemaGold, label = "Selected")
            LegendItem(color = ErrorRed.copy(alpha = 0.4f), label = "Booked")
        }

        if (bookingError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bookingError,
                color = ErrorRed,
                style = CinemaTypography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Seat Layout Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(seatsByRow.keys.toList()) { rowLabel ->
                    val rowSeats = seatsByRow[rowLabel] ?: emptyList()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = rowLabel,
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(rowSeats, key = { it.id }) { seat ->
                                val isSelected = selectedSeatIds.contains(seat.seatId)
                                val isBooked = seat.status == "BOOKED" || seat.status == "BLOCKED"

                                SeatChip(
                                    seatCode = seat.seatCode,
                                    isSelected = isSelected,
                                    isBooked = isBooked,
                                    onClick = { onSeatClick(seat) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${selectedSeatIds.size} Seats Selected",
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹${String.format("%.2f", totalPrice)}",
                        style = CinemaTypography.displayMedium,
                        color = CinemaGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                CinemaButton(
                    text = "Confirm Booking",
                    onClick = onConfirmBooking,
                    enabled = selectedSeatIds.isNotEmpty(),
                    isLoading = isBookingLoading,
                    modifier = Modifier.width(170.dp)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = CinemaTypography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun SeatChip(
    seatCode: String,
    isSelected: Boolean,
    isBooked: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = when {
        isBooked -> ErrorRed.copy(alpha = 0.3f)
        isSelected -> CinemaGold
        else -> BackgroundElevated
    }
    val contentColor = when {
        isBooked -> TextDisabled
        isSelected -> TextOnGold
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = !isBooked) { onClick() }
            .border(
                width = 1.dp,
                color = if (isSelected) CinemaGold else Divider,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seatCode,
            style = CinemaTypography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        )
    }
}
