package com.cineplex.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.UserDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToBookings: () -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToAdminPayments: () -> Unit = {},
    onNavigateToAdminTicketVerify: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", style = CinemaTypography.headlineSmall) },
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
            when (val state = userState) {
                is UiState.Loading -> LoadingStateView(message = "Loading profile...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadProfile() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = "User data not available",
                    onRetry = { viewModel.loadProfile() }
                )
                is UiState.Success -> {
                    ProfileContent(
                        user = state.data,
                        onLogout = { viewModel.logout(onLogout) },
                        onNavigateToBookings = onNavigateToBookings,
                        onNavigateToAdminDashboard = onNavigateToAdminDashboard,
                        onNavigateToAdminPayments = onNavigateToAdminPayments,
                        onNavigateToAdminTicketVerify = onNavigateToAdminTicketVerify
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: UserDto,
    onLogout: () -> Unit,
    onNavigateToBookings: () -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToAdminPayments: () -> Unit = {},
    onNavigateToAdminTicketVerify: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(CinemaGold.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = CinemaGold,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.name,
            style = CinemaTypography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = if (user.role == "ADMIN") CinemaGold.copy(alpha = 0.2f) else BackgroundElevated,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = user.role,
                style = CinemaTypography.labelMedium.copy(
                    color = if (user.role == "ADMIN") CinemaGold else TextSecondary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBookings() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("My Bookings", style = CinemaTypography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Text("View All >", style = CinemaTypography.bodySmall, color = CinemaGold)
            }
        }

        if (user.role == "ADMIN") {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdminDashboard() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CinemaGold))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Admin Control Center", style = CinemaTypography.bodyMedium, color = CinemaGold, fontWeight = FontWeight.Bold)
                    }
                    Text("Open Dashboard >", style = CinemaTypography.bodySmall, color = CinemaGold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdminPayments() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.4f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Verify Payments (Admin)", style = CinemaTypography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text("Manage >", style = CinemaTypography.bodySmall, color = CinemaGold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdminTicketVerify() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.4f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Verify Ticket QR (Admin)", style = CinemaTypography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text("Scan >", style = CinemaTypography.bodySmall, color = CinemaGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileItem(icon = Icons.Default.Email, label = "Email", value = user.email)
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                ProfileItem(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = user.phone ?: "Not provided"
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                ProfileItem(icon = Icons.Default.Badge, label = "User ID", value = "#${user.id}")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed.copy(alpha = 0.15f),
                contentColor = ErrorRed
            )
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", style = CinemaTypography.labelLarge)
        }
    }
}

@Composable
fun ProfileItem(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CinemaGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = CinemaTypography.bodySmall, color = TextSecondary)
            Text(text = value, style = CinemaTypography.bodyMedium, color = TextPrimary)
        }
    }
}
