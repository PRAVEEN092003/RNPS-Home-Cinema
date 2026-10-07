package com.cineplex.app.ui.screens.snacks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
                        Text("Add Cinema Snacks", style = CinemaTypography.headlineSmall)
                        Text(
                            text = "RNPS Home Cinema Snacks Menu",
                            style = CinemaTypography.bodySmall,
                            color = CinemaGold
                        )
                    }
                },
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
            when (val state = snacksState) {
                is UiState.Loading -> LoadingStateView(message = "Loading snacks menu...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadSnacks() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadSnacks() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = "Snacks menu unavailable.",
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

                    SnacksContent(
                        snacks = filtered,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        quantities = quantities,
                        onIncrement = { viewModel.incrementQuantity(it) },
                        onDecrement = { viewModel.decrementQuantity(it) },
                        totalItems = viewModel.getTotalItemsCount(),
                        totalPrice = viewModel.calculateTotal(allSnacks),
                        onContinue = { viewModel.submitSnacks() },
                        onSkip = { onContinueToSummary(viewModel.bookingId) },
                        isSaving = saveState is UiState.Loading
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
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    totalItems: Int,
    totalPrice: Double,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isSaving: Boolean,
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
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(cat) },
                    label = { Text(cat) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CinemaGold,
                        selectedLabelColor = TextOnGold,
                        containerColor = BackgroundCard,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Snack List
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

        // Bottom Navigation Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (totalItems > 0) "$totalItems Item(s) Added" else "No Snacks Selected",
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${String.format("%.2f", totalPrice)}",
                            style = CinemaTypography.headlineMedium,
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    CinemaButton(
                        text = if (totalItems > 0) "Continue (₹${String.format("%.2f", totalPrice)})" else "Continue to Summary",
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
                        Text("Skip Snacks & Continue", color = TextSecondary, style = CinemaTypography.bodySmall)
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
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
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
                    text = "₹${String.format("%.2f", snack.price)}",
                    style = CinemaTypography.titleMedium,
                    color = CinemaGold,
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
                        containerColor = CinemaGold.copy(alpha = 0.2f),
                        contentColor = CinemaGold
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Add", style = CinemaTypography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(BackgroundElevated, RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                    }
                    Text(
                        text = "$quantity",
                        style = CinemaTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = CinemaGold)
                    }
                }
            }
        }
    }
}
