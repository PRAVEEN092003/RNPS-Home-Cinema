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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.UserDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LanguageSelector
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
    val currentLanguage by viewModel.languageState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.my_profile), style = CinemaTypography.headlineSmall) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = userState) {
                is UiState.Loading -> LoadingStateView(message = stringResource(id = R.string.loading_profile))
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadProfile() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = stringResource(id = R.string.user_data_unavailable),
                    onRetry = { viewModel.loadProfile() }
                )
                is UiState.Success -> {
                    ProfileContent(
                        user = state.data,
                        currentLanguage = currentLanguage,
                        onLanguageSelected = { viewModel.setLanguage(it) },
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
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
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

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Language Selection Card ───────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.3f))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.language_setting_title),
                            style = CinemaTypography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.language_setting_subtitle),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected,
                    showLabel = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── My Bookings Card ──────────────────────────────────────────────
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
                    Text(
                        text = stringResource(id = R.string.my_bookings),
                        style = CinemaTypography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(id = R.string.view_all),
                    style = CinemaTypography.bodySmall,
                    color = CinemaGold
                )
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
                        Text(
                            text = stringResource(id = R.string.admin_control_center),
                            style = CinemaTypography.bodyMedium,
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.open_dashboard),
                        style = CinemaTypography.bodySmall,
                        color = CinemaGold
                    )
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
                        Text(
                            text = stringResource(id = R.string.verify_payments_admin),
                            style = CinemaTypography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.manage_action),
                        style = CinemaTypography.bodySmall,
                        color = CinemaGold
                    )
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
                        Text(
                            text = stringResource(id = R.string.verify_ticket_qr_admin),
                            style = CinemaTypography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.scan_action),
                        style = CinemaTypography.bodySmall,
                        color = CinemaGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── Contact Info Card ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileItem(icon = Icons.Default.Email, label = stringResource(id = R.string.email), value = user.email)
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                ProfileItem(
                    icon = Icons.Default.Phone,
                    label = stringResource(id = R.string.phone),
                    value = user.phone ?: stringResource(id = R.string.not_provided)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                ProfileItem(icon = Icons.Default.Badge, label = stringResource(id = R.string.user_id_label), value = "#${user.id}")
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
            Text(stringResource(id = R.string.logout), style = CinemaTypography.labelLarge)
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
