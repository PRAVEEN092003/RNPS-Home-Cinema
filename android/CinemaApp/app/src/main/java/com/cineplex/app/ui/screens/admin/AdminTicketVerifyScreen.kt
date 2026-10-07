package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.TicketVerificationDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaTextField
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketVerifyScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminTicketVerifyViewModel = hiltViewModel(),
) {
    val codeInput by viewModel.codeInput.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Scanner & Verification", style = CinemaTypography.headlineSmall) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CinemaTextField(
                value = codeInput,
                onValueChange = { viewModel.codeInput.value = it },
                label = "Enter Ticket Code or Booking Ref (e.g. TKT-...) ",
                leadingIcon = Icons.Default.QrCodeScanner
            )

            Spacer(modifier = Modifier.height(16.dp))

            CinemaButton(
                text = "Verify Ticket Code",
                onClick = { viewModel.verifyTicket() },
                isLoading = verifyState is UiState.Loading,
                enabled = codeInput.isNotBlank()
            )

            Spacer(modifier = Modifier.height(28.dp))

            when (val state = verifyState) {
                is UiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("INVALID TICKET CODE", style = CinemaTypography.headlineSmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, style = CinemaTypography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
                is UiState.Success -> {
                    TicketVerificationResultCard(result = state.data)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TicketVerificationResultCard(result: TicketVerificationDto) {
    val isValid = result.isValid
    val statusColor = if (isValid) SuccessGreen else ErrorRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(statusColor))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isValid) "VALID TICKET" else "INVALID / UNPAID",
                        style = CinemaTypography.headlineSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = result.paymentStatus,
                        style = CinemaTypography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 14.dp), color = Divider)

            VerifyItem(label = "Customer Name", value = result.userName)
            VerifyItem(label = "Movie", value = result.movieTitle)
            VerifyItem(label = "Show", value = "${result.screenName} • ${result.showTime}")
            VerifyItem(label = "Seats", value = result.seats.joinToString(", "))
            VerifyItem(label = "Booking Ref", value = result.bookingReference)
            VerifyItem(label = "Ticket Code", value = result.ticketCode)
        }
    }
}

@Composable
fun VerifyItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = CinemaTypography.bodySmall, color = TextSecondary)
        Text(text = value, style = CinemaTypography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}
