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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineplex.app.R
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.theme.*
import java.util.Locale

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
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(id = R.string.booking_confirmed_header),
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.app_name),
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
                            text = stringResource(id = R.string.label_booking_ref),
                            style = CinemaTypography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = bookingReference,
                            style = CinemaTypography.headlineLarge,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Divider)

                        DetailRow(label = stringResource(id = R.string.movie_label), value = movieTitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = stringResource(id = R.string.screen_label), value = screenName)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = stringResource(id = R.string.label_seats), value = seatsSummary)

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Divider)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_paid),
                                style = CinemaTypography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "₹${String.format(Locale.ENGLISH, "%.2f", totalAmount)}",
                                style = CinemaTypography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                CinemaButton(
                    text = stringResource(id = R.string.back_to_home),
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
