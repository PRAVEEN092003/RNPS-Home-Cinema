package com.cineplex.app.ui.screens.seats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    bookingReference: String,
    movieTitle: String,
    screenName: String,
    seatsSummary: String,
    totalAmount: Double,
    onNavigateToHome: () -> Unit,
) {
    Scaffold(
        containerColor = BackgroundDeep,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Booking Confirmed!",
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "RNPS Home Cinema",
                    style = CinemaTypography.bodyMedium,
                    color = CinemaGold,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Ticket Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "BOOKING REFERENCE",
                            style = CinemaTypography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = bookingReference,
                            style = CinemaTypography.headlineLarge,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Divider)

                        DetailRow(label = "Movie", value = movieTitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "Screen", value = screenName)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "Seats", value = seatsSummary)

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Divider)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Paid",
                                style = CinemaTypography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "$${String.format("%.2f", totalAmount)}",
                                style = CinemaTypography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                CinemaButton(
                    text = "Back to Home",
                    onClick = onNavigateToHome
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = CinemaTypography.bodyMedium, color = TextSecondary)
        Text(text = value, style = CinemaTypography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}
