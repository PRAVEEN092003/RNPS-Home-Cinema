package com.cineplex.app.ui.screens.snacks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cineplex.app.R
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.SnackDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaImage
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnacksScreen(
    onNavigateBack: () -> Unit,
    onContinueToSummary: (Int) -> Unit,
    viewModel: SnacksViewModel = hiltViewModel(),
) {
    val snacksState by viewModel.snacksState.collectAsState()
    val quantities by viewModel.quantities.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var selectedCategory by remember { mutableStateOf("ALL") }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadSnacks(showLoading = false, forceRefresh = true)
                viewModel.loadBookingSnacks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            viewModel.resetSaveState()
            onContinueToSummary(viewModel.bookingId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.add_cinema_snacks), style = CinemaTypography.headlineSmall)
                        Text(
                            text = stringResource(R.string.snacks_menu_subtitle),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            when (val state = snacksState) {
                is UiState.Loading -> LoadingStateView(message = stringResource(R.string.loading_snacks_menu))
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadSnacks() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadSnacks() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = stringResource(R.string.snacks_menu_unavailable),
                    onRetry = { viewModel.loadSnacks() }
                )
                is UiState.Success -> {
                    val allSnacks = state.data
                    val categories = listOf("ALL", "POPCORN", "DRINKS", "COMBOS", "OTHER")
                    val filtered = if (selectedCategory == "ALL") {
                        allSnacks
                    } else {
                        allSnacks.filter { it.category.uppercase() == selectedCategory }
                    }

                    val selectedSnacks = viewModel.getSelectedSnacksList(allSnacks)
                    val saveError = (saveState as? UiState.Error)?.message

                    SnacksContent(
                        snacks = filtered,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        quantities = quantities,
                        selectedSnacks = selectedSnacks,
                        onIncrement = { viewModel.incrementQuantity(it) },
                        onDecrement = { viewModel.decrementQuantity(it) },
                        onRemove = { viewModel.removeSnack(it) },
                        totalItems = viewModel.getTotalItemsCount(),
                        totalPrice = viewModel.calculateTotal(allSnacks),
                        onContinue = { viewModel.submitSnacks(clearAll = false) },
                        onSkip = {
                            viewModel.submitSnacks(clearAll = true)
                        },
                        isSaving = saveState is UiState.Loading,
                        saveError = saveError,
                    )
                }
            }
        }
    }
}

@Composable
fun SnacksContent(
    snacks: List<SnackDto>,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    quantities: Map<Int, Int>,
    selectedSnacks: List<SelectedSnackItem>,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    totalItems: Int,
    totalPrice: Double,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isSaving: Boolean,
    saveError: String? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = BackgroundDeep,
            contentColor = CinemaGold,
            edgePadding = 16.dp,
            indicator = {}
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                val labelText = when (cat) {
                    "ALL" -> stringResource(R.string.category_all)
                    "POPCORN" -> stringResource(R.string.category_popcorn)
                    "DRINKS" -> stringResource(R.string.category_drinks)
                    "COMBOS" -> stringResource(R.string.category_combos)
                    "OTHER" -> stringResource(R.string.category_other)
                    else -> cat
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(cat) },
                    label = { Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CinemaGold,
                        selectedLabelColor = TextOnGold,
                        containerColor = BackgroundCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CinemaGold else CardBorderGold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Snack Menu List
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(snacks, key = { it.id }) { snack ->
                    val qty = quantities[snack.id] ?: 0
                    SnackItemCard(
                        snack = snack,
                        quantity = qty,
                        onIncrement = { onIncrement(snack.id) },
                        onDecrement = { onDecrement(snack.id) }
                    )
                }
            }
        }

        // ─── Visible Selected Snacks Cart Breakdown ─────────────────────────
        if (selectedSnacks.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.selected_snacks_title, totalItems),
                            style = CinemaTypography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CinemaGold)
                        )
                        Text(
                            text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", totalPrice)}",
                            style = CinemaTypography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))

                    selectedSnacks.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.snack.name,
                                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stringResource(R.string.unit_price_each, String.format(java.util.Locale.ENGLISH, "%.2f", item.unitPrice))} • ₹${String.format(java.util.Locale.ENGLISH, "%.2f", item.subtotal)}",
                                    style = CinemaTypography.labelSmall.copy(color = TextSecondary)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDecrement(item.snack.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = stringResource(R.string.decrease),
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${item.quantity}",
                                    style = CinemaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { onIncrement(item.snack.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.increase),
                                        tint = CinemaGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemove(item.snack.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.remove_item),
                                        tint = ErrorRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (saveError != null) {
                    Text(
                        text = saveError,
                        color = ErrorRed,
                        style = CinemaTypography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (totalItems > 0) stringResource(R.string.items_added_count, totalItems) else stringResource(R.string.no_snacks_selected),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", totalPrice)}",
                            style = CinemaTypography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }

                    CinemaButton(
                        text = if (totalItems > 0) stringResource(R.string.continue_with_amount, String.format(java.util.Locale.ENGLISH, "%.2f", totalPrice)) else stringResource(R.string.continue_to_summary),
                        onClick = if (totalItems > 0) onContinue else onSkip,
                        isLoading = isSaving,
                        modifier = Modifier.width(200.dp)
                    )
                }

                if (totalItems > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.skip_snacks_continue), color = TextSecondary, style = CinemaTypography.bodySmall)
                    }
                }
            }
        }
    }
}


@Composable
fun SnackItemCard(
    snack: SnackDto,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snack Image with fallback
            CinemaImage(
                imageUrl = snack.imageUrl,
                contentDescription = snack.name,
                fallbackIcon = Icons.Default.Fastfood,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snack.name,
                    style = CinemaTypography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                snack.description?.let {
                    Text(
                        text = it,
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "₹${String.format(java.util.Locale.ENGLISH, "%.2f", snack.price)}",
                    style = CinemaTypography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quantity Counter
            if (quantity == 0) {
                Button(
                    onClick = onIncrement,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold.copy(alpha = 0.16f),
                        contentColor = CinemaGold
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.add_snack), style = CinemaTypography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(BackgroundElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, CardBorderGold, RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease), tint = TextPrimary)
                    }
                    Text(
                        text = "$quantity",
                        style = CinemaTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase), tint = CinemaGold)
                    }
                }
            }
        }
    }
}
