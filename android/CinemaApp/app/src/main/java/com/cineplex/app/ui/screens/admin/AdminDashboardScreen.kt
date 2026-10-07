package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineplex.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToShows: () -> Unit,
    onNavigateToSnacks: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToTicketVerify: () -> Unit,
    onNavigateToScreens: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Center", style = CinemaTypography.headlineSmall) },
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
                .padding(16.dp)
        ) {
            Text(
                text = "RNPS Home Cinema Admin",
                style = CinemaTypography.headlineMedium,
                color = CinemaGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage movies, shows, screens, snacks, and customer bookings",
                style = CinemaTypography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AdminDashboardCard(
                        title = "Movie Management",
                        subtitle = "Add, edit, or deactivate movies",
                        icon = Icons.Default.Movie,
                        onClick = onNavigateToMovies
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Show Management",
                        subtitle = "Schedule screens, dates & prices",
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToShows
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Screen & UPI Config",
                        subtitle = "Edit specs, Dolby Atmos & UPI",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToScreens
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Snack Management",
                        subtitle = "Manage food & drink inventory",
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToSnacks
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Booking Management",
                        subtitle = "View all paid, pending & cancelled",
                        icon = Icons.Default.ConfirmationNumber,
                        onClick = onNavigateToBookings
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Payment Verification",
                        subtitle = "Approve or reject customer UPI",
                        icon = Icons.Default.Payments,
                        onClick = onNavigateToPayments
                    )
                }
                item {
                    AdminDashboardCard(
                        title = "Verify Ticket QR",
                        subtitle = "Scan or enter ticket code",
                        icon = Icons.Default.QrCodeScanner,
                        onClick = onNavigateToTicketVerify
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CinemaGold,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = title,
                    style = CinemaTypography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = CinemaTypography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
