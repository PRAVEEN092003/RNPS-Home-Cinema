package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.PaymentConfigDto
import com.cineplex.app.data.model.ScreenDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreensScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminScreensViewModel = hiltViewModel(),
) {
    val screensState by viewModel.screensState.collectAsState()
    val configState by viewModel.configState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var editingScreen by remember { mutableStateOf<ScreenDto?>(null) }
    var isEditingConfig by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            editingScreen = null
            isEditingConfig = false
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screens & UPI Config", style = CinemaTypography.headlineSmall) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Screen Specifications & Seats",
                style = CinemaTypography.headlineMedium,
                color = CinemaGold,
                fontWeight = FontWeight.Bold
            )

            when (val state = screensState) {
                is UiState.Loading -> LoadingStateView(message = "Loading screens...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadData() })
                is UiState.Error -> ErrorStateView(state.message) { viewModel.loadData() }
                is UiState.Empty -> ErrorStateView("No screens available.") { viewModel.loadData() }
                is UiState.Success -> {
                    state.data.forEach { screen ->
                        AdminScreenCard(
                            screen = screen,
                            onEdit = { editingScreen = screen }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "UPI Payment Configuration",
                style = CinemaTypography.headlineMedium,
                color = CinemaGold,
                fontWeight = FontWeight.Bold
            )

            when (val state = configState) {
                is UiState.Loading -> LoadingStateView(message = "Loading UPI details...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadData() })
                is UiState.Error -> ErrorStateView(state.message) { viewModel.loadData() }
                is UiState.Empty -> ErrorStateView("No payment config.") { viewModel.loadData() }
                is UiState.Success -> {
                    AdminUpiConfigCard(
                        config = state.data,
                        onEdit = { isEditingConfig = true }
                    )
                }
            }
        }
    }

    if (editingScreen != null) {
        EditScreenDialog(
            screen = editingScreen!!,
            onDismiss = { editingScreen = null },
            onSave = { name, displaySpec, audioSpec, hasDolbyAtmos, price ->
                viewModel.updateScreen(
                    id = editingScreen!!.id,
                    name = name,
                    displaySpec = displaySpec,
                    audioSpec = audioSpec,
                    hasDolbyAtmos = hasDolbyAtmos,
                    seatBasePrice = price
                )
            },
            isSaving = actionState is UiState.Loading
        )
    }

    if (isEditingConfig && configState is UiState.Success) {
        val cfg = (configState as UiState.Success<PaymentConfigDto>).data
        EditUpiConfigDialog(
            config = cfg,
            onDismiss = { isEditingConfig = false },
            onSave = { upiId, payeeName ->
                viewModel.updatePaymentConfig(upiId, payeeName)
            },
            isSaving = actionState is UiState.Loading
        )
    }
}

@Composable
fun AdminScreenCard(
    screen: ScreenDto,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = CinemaGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(screen.name, style = CinemaTypography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Total Seats: ${screen.totalSeats}", style = CinemaTypography.bodySmall, color = TextSecondary)
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Screen", tint = CinemaGold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(color = BackgroundElevated, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Display: ${screen.displaySpec}", style = CinemaTypography.bodySmall, color = TextPrimary)
                    Text("Audio: ${screen.audioSpec}", style = CinemaTypography.bodySmall, color = TextPrimary)
                    Text("Dolby Atmos: ${if (screen.hasDolbyAtmos) "Enabled (Branding Active)" else "Disabled"}", style = CinemaTypography.bodySmall, color = CinemaGold)
                }
            }
        }
    }
}

@Composable
fun AdminUpiConfigCard(
    config: PaymentConfigDto,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Payee Name: ${config.payeeName}", style = CinemaTypography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("UPI ID: ${config.upiId}", style = CinemaTypography.bodyMedium, color = CinemaGold)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit UPI Config", tint = CinemaGold)
                }
            }
        }
    }
}

@Composable
fun EditScreenDialog(
    screen: ScreenDto,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, Double?) -> Unit,
    isSaving: Boolean,
) {
    var name by remember { mutableStateOf(screen.name) }
    var displaySpec by remember { mutableStateOf(screen.displaySpec) }
    var audioSpec by remember { mutableStateOf(screen.audioSpec) }
    var hasDolbyAtmos by remember { mutableStateOf(screen.hasDolbyAtmos) }
    var seatPriceStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${screen.name}", style = CinemaTypography.headlineSmall, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Screen Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
                OutlinedTextField(
                    value = displaySpec,
                    onValueChange = { displaySpec = it },
                    label = { Text("Display Specification (e.g. 4K Ultra HD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
                OutlinedTextField(
                    value = audioSpec,
                    onValueChange = { audioSpec = it },
                    label = { Text("Audio Specification (e.g. 13-Channel Dolby Atmos)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dolby Atmos Branding:", style = CinemaTypography.bodyMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = hasDolbyAtmos,
                        onCheckedChange = { hasDolbyAtmos = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold)
                    )
                }
                OutlinedTextField(
                    value = seatPriceStr,
                    onValueChange = { seatPriceStr = it },
                    label = { Text("Update Ticket Price (₹ INR, optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = seatPriceStr.toDoubleOrNull()
                    onSave(name, displaySpec, audioSpec, hasDolbyAtmos, price)
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = TextOnGold)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextOnGold)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = BackgroundCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditUpiConfigDialog(
    config: PaymentConfigDto,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    isSaving: Boolean,
) {
    var upiId by remember { mutableStateOf(config.upiId) }
    var payeeName by remember { mutableStateOf(config.payeeName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit UPI Payment Details", style = CinemaTypography.headlineSmall, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = payeeName,
                    onValueChange = { payeeName = it },
                    label = { Text("Payee Name (e.g. RNPS Home Cinema)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (e.g. rnpscinema@upi)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(upiId, payeeName) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = TextOnGold)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextOnGold)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = BackgroundCard,
        shape = RoundedCornerShape(20.dp)
    )
}
