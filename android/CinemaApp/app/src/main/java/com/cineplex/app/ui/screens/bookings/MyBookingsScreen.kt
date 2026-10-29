package com.cineplex.app.ui.screens.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.ListLoadingSkeleton
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onSelectBooking: (BookingDto) -> Unit,
    viewModel: MyBookingsViewModel = hiltViewModel(),
) {
    val bookingsState by viewModel.bookingsState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadBookings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.my_bookings), style = CinemaTypography.headlineSmall) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: All, Pending, Confirmed, Cancelled
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundDeep,
                contentColor = CinemaGold,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CinemaGold
                    )
                }
            ) {
                val tabTitles = listOf(
                    stringResource(id = R.string.tab_all_bookings),
                    stringResource(id = R.string.tab_pending),
                    stringResource(id = R.string.tab_confirmed),
                    stringResource(id = R.string.tab_cancelled)
                )
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = CinemaTypography.titleMedium.copy(
                                    color = if (selectedTab == index) Color.White else TextSecondary,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = bookingsState) {
                    is UiState.Loading -> ListLoadingSkeleton(count = 4, itemHeight = 150.dp)
                    is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadBookings() })
                    is UiState.Error -> ErrorStateView(
                        message = state.message,
                        onRetry = { viewModel.loadBookings() }
                    )
                    is UiState.Empty -> EmptyBookingsView(onBrowseMovies = onNavigateToHome)
                    is UiState.Success -> {
                        val allBookings = state.data
                        val filtered = when (selectedTab) {
                            1 -> allBookings.filter { it.status.uppercase() == "PENDING" }
                            2 -> allBookings.filter { it.status.uppercase() in listOf("CONFIRMED", "BOOKED", "COMPLETED") }
                            3 -> allBookings.filter { it.status.uppercase() in listOf("CANCELLED", "REJECTED") || it.payment?.status?.uppercase() == "FAILED" }
                            else -> allBookings
                        }

                        if (filtered.isEmpty()) {
                            EmptyBookingsView(onBrowseMovies = onNavigateToHome)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filtered, key = { it.canonicalId }) { booking ->
                                    BookingCard(
                                        booking = booking,
                                        onClick = { onSelectBooking(booking) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingDto,
    onClick: () -> Unit,
) {
    val isConfirmed = booking.status.uppercase() in listOf("CONFIRMED", "BOOKED", "COMPLETED")
    val isCancelled = booking.status.uppercase() in listOf("CANCELLED", "REJECTED") || booking.payment?.status?.uppercase() == "FAILED"
    val badgeColor = when {
        isConfirmed -> SuccessGreen
        isCancelled -> ErrorRed
        else -> WarningAmber
    }
    val badgeText = when {
        isConfirmed -> stringResource(id = R.string.status_confirmed_issued)
        isCancelled -> stringResource(id = R.string.status_cancelled_rejected)
        else -> stringResource(id = R.string.status_pending_admin)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.bookingReference,
                    style = CinemaTypography.titleMedium.copy(
                        color = CinemaGold,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badgeText,
                        style = CinemaTypography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = booking.show?.movie?.title ?: stringResource(id = R.string.app_name),
                style = CinemaTypography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            val showDateTimeStr = DateTimeUtils.formatShowDateTime(booking.show?.startTime)
            if (showDateTimeStr.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = showDateTimeStr,
                    style = CinemaTypography.bodySmall,
                    color = CinemaGold,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val screenName = booking.show?.screen?.name ?: stringResource(id = R.string.screen_label)
            Text(
                text = stringResource(id = R.string.seats_count_format, screenName, booking.totalSeats),
                style = CinemaTypography.bodyMedium,
                color = TextSecondary
            )

            val seatCodes = booking.seats.joinToString(", ") { it.seatCode }
            if (seatCodes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CinemaGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = seatCodes,
                            style = CinemaTypography.labelSmall.copy(
                                color = CinemaGoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.total_amount_label),
                    style = CinemaTypography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", booking.totalAmount)}",
                    style = CinemaTypography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun EmptyBookingsView(
    onBrowseMovies: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ConfirmationNumber,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.no_bookings_title),
                style = CinemaTypography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.no_bookings_desc),
                style = CinemaTypography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CinemaButton(
                text = stringResource(id = R.string.browse_movies),
                onClick = onBrowseMovies,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}
