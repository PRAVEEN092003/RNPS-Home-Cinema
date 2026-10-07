package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.SnackDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSnacksScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminSnacksViewModel = hiltViewModel()
) {
    val snacksState by viewModel.snacksState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingSnack by remember { mutableStateOf<SnackDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snack Management", style = CinemaTypography.headlineSmall) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSnack = null
                    showDialog = true
                },
                containerColor = CinemaGold,
                contentColor = BackgroundDeep
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Snack")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDeep,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = snacksState) {
                is UiState.Loading -> LoadingStateView(message = "Loading snacks...")
                is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadSnacks() })
                is UiState.Empty -> ErrorStateView(message = "No snacks found", onRetry = { viewModel.loadSnacks() })
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.data) { snack ->
                            SnackAdminCard(
                                snack = snack,
                                onEdit = {
                                    editingSnack = snack
                                    showDialog = true
                                },
                                onDelete = { viewModel.deleteSnack(snack.id) }
                            )
                        }
                    }
                }
                else -> {}
            }

            if (showDialog) {
                SnackFormDialog(
                    snack = editingSnack,
                    onDismiss = { showDialog = false },
                    onSubmit = { name, desc, cat, price, img ->
                        if (editingSnack == null) {
                            viewModel.createSnack(name, desc, cat, price, img)
                        } else {
                            viewModel.updateSnack(editingSnack!!.id, name, desc, cat, price, img)
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SnackAdminCard(
    snack: SnackDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snack.name,
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (snack.isAvailable) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (snack.isAvailable) "Available" else "Disabled",
                            style = CinemaTypography.labelMedium,
                            color = if (snack.isAvailable) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${snack.category} • Price: ₹${"%.2f".format(snack.price)}",
                    style = CinemaTypography.bodyMedium,
                    color = CinemaGold,
                    fontWeight = FontWeight.Bold
                )
                if (!snack.description.isNullOrBlank()) {
                    Text(
                        text = snack.description,
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = CinemaGold)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnackFormDialog(
    snack: SnackDto?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, desc: String, category: String, price: Double, image: String) -> Unit
) {
    var name by remember { mutableStateOf(snack?.name ?: "") }
    var description by remember { mutableStateOf(snack?.description ?: "") }
    var category by remember { mutableStateOf(snack?.category ?: "POPCORN") }
    var priceStr by remember { mutableStateOf(snack?.price?.toString() ?: "150.0") }
    var imageUrl by remember { mutableStateOf(snack?.imageUrl ?: "") }

    val categories = listOf("POPCORN", "DRINKS", "COMBOS", "OTHER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (snack == null) "Add New Snack" else "Edit Snack",
                style = CinemaTypography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Snack Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )

                Text("Category", style = CinemaTypography.bodySmall, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category.uppercase() == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = CinemaTypography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = BackgroundDeep
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 100.0
                    if (name.isNotBlank()) {
                        onSubmit(name, description, category, price, imageUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = BackgroundDeep)
            ) {
                Text(if (snack == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = BackgroundElevated
    )
}
