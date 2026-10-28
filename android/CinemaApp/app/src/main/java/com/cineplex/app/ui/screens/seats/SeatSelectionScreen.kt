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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.cineplex.app.R
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadSeats(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.activity.compose.BackHandler {
        viewModel.cancelActiveBooking()
        onNavigateBack()
    }

    LaunchedEffect(bookingState) {
        if (bookingState is UiState.Success) {
            val booking = (bookingState as UiState.Success<BookingDto>).data
            viewModel.resetBookingState()
            pendingBooking = booking
        }
    }

    if (pendingBooking != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.cancelActiveBooking()
                pendingBooking = null
            },
            title = { Text(stringResource(id = R.string.dialog_snacks_title), style = CinemaTypography.headlineSmall, color = TextPrimary) },
            text = { Text(stringResource(id = R.string.dialog_snacks_desc), style = CinemaTypography.bodyMedium, color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = pendingBooking!!
                        pendingBooking = null
                        onBookingSuccessWithSnacks(b)
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_snacks_yes), color = CinemaGold, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(id = R.string.dialog_snacks_no), color = TextSecondary)
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
                        Text(stringResource(id = R.string.select_seats), style = CinemaTypography.headlineSmall)
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelActiveBooking()
                        onNavigateBack()
                    }) {
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
                is UiState.Loading -> LoadingStateView(message = stringResource(id = R.string.loading_seat_layout))
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadSeats() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadSeats() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = stringResource(id = R.string.seat_map_unavailable),
                    onRetry = { viewModel.loadSeats() }
                )
                is UiState.Success -> {
                    SeatSelectionContent(
                        data = state.data,
                        selectedSeatIds = selectedSeatIds,
                        onSeatClick = { viewModel.toggleSeatSelection(it) },
                        totalPrice = viewModel.calculateTotal(state.data.seats, state.data.show.basePrice),
                        onConfirmBooking = { viewModel.confirmBooking(state.data.show.basePrice) },
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    color = ElectricBlueLight
                )
                if (screen.hasDolbyAtmos) {
                    Surface(
                        color = ElectricCinemaBlue.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlueLight.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.dolby_atmos),
                            style = CinemaTypography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Glowing Curved Cinema Screen Graphic ─────────────────────────
            CinematicCurvedScreen(modifier = Modifier.fillMaxWidth().height(42.dp))

            Text(
                text = stringResource(id = R.string.screen_this_way).uppercase(),
                style = CinemaTypography.labelSmall.copy(
                    color = TextDisabled,
                    letterSpacing = 2.5.sp,
                    fontSize = 9.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                boxColor = SeatAvailable,
                borderColor = BorderSubtle,
                label = stringResource(id = R.string.seat_available)
            )
            LegendItem(
                boxColor = SeatSelected,
                borderColor = CinemaGoldLight,
                label = stringResource(id = R.string.seat_selected)
            )
            LegendItem(
                boxColor = SeatBooked,
                borderColor = Color(0xFF222222),
                label = stringResource(id = R.string.seat_booked)
            )
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

        Spacer(modifier = Modifier.height(14.dp))

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
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(rowSeats, key = { it.id }) { seat ->
                                val isSelected = selectedSeatIds.contains(seat.seatId)
                                val isBooked = (seat.status == "BOOKED" || seat.status == "BLOCKED" || seat.status == "HELD") && !isSelected

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
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.seats_selected_count, selectedSeatIds.size),
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", totalPrice)}",
                        style = CinemaTypography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                CinemaButton(
                    text = stringResource(id = R.string.confirm_booking),
                    onClick = onConfirmBooking,
                    enabled = selectedSeatIds.isNotEmpty(),
                    isLoading = isBookingLoading,
                    modifier = Modifier.width(170.dp)
                )
            }
        }
    }
}

/**
 * Curved Cinema Projection Screen with ambient blue glow projection beam.
 */
@Composable
fun CinematicCurvedScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Projection light beam gradient downwards
        val beamPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.15f, 4f)
            lineTo(width * 0.85f, 4f)
            lineTo(width * 0.95f, height)
            lineTo(width * 0.05f, height)
            close()
        }

        // Gold glow beam downward from screen
        drawPath(
            path = beamPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    CinemaGold.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Screen Curved Arch
        val arcPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.12f, 18f)
            quadraticTo(width * 0.5f, 2f, width * 0.88f, 18f)
        }

        // Screen glow aura — gold
        drawPath(
            path = arcPath,
            color = CinemaGold.copy(alpha = 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )

        // Sharp screen border
        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = 0.95f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Composable
fun LegendItem(
    boxColor: Color,
    borderColor: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(boxColor)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = CinemaTypography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondary
        )
    }
}

@Composable
fun SeatChip(
    seatCode: String,
    isSelected: Boolean,
    isBooked: Boolean,
    onClick: () -> Unit,
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f),
        label = "seatScale"
    )

    val bgColor = when {
        isBooked -> SeatBooked
        isSelected -> SeatSelected
        else -> SeatAvailable
    }

    val borderColor = when {
        isBooked -> Color(0xFF222222)
        isSelected -> CinemaGoldLight
        else -> BorderSubtle
    }

    val contentColor = when {
        isBooked -> Color(0xFF555555)
        isSelected -> Color(0xFF080808)
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 34.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(enabled = !isBooked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seatCode,
            style = CinemaTypography.labelSmall.copy(
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = 9.sp
            )
        )
    }
}
