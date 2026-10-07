package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShowsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminShowsViewModel = hiltViewModel()
) {
    val showsState by viewModel.showsState.collectAsState()
    val moviesList by viewModel.moviesList.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingShow by remember { mutableStateOf<ShowDto?>(null) }

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
                title = { Text("Show Management", style = CinemaTypography.headlineSmall) },
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
                    editingShow = null
                    showDialog = true
                },
                containerColor = CinemaGold,
                contentColor = BackgroundDeep
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Show")
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
            when (val state = showsState) {
                is UiState.Loading -> LoadingStateView(message = "Loading shows...")
                is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadShows() })
                is UiState.Empty -> ErrorStateView(message = "No shows found", onRetry = { viewModel.loadShows() })
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.data) { show ->
                            ShowAdminCard(
                                show = show,
                                onEdit = {
                                    editingShow = show
                                    showDialog = true
                                },
                                onDelete = { viewModel.deleteShow(show.id) }
                            )
                        }
                    }
                }
                else -> {}
            }

            if (showDialog) {
                ShowFormDialog(
                    show = editingShow,
                    movies = moviesList,
                    onDismiss = { showDialog = false },
                    onSubmit = { movieId, screenId, startTime, endTime, price ->
                        if (editingShow == null) {
                            viewModel.createShow(movieId, screenId, startTime, endTime, price)
                        } else {
                            viewModel.updateShow(editingShow!!.id, movieId, screenId, startTime, endTime, price)
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ShowAdminCard(
    show: ShowDto,
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
                        text = show.movie?.title ?: "Movie #${show.movieId}",
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (show.isActive) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (show.isActive) "Active" else "Deactivated",
                            style = CinemaTypography.labelMedium,
                            color = if (show.isActive) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${show.screen?.name ?: "Screen #${show.screenId}"} • Price: ₹${"%.2f".format(show.basePrice)}",
                    style = CinemaTypography.bodyMedium,
                    color = CinemaGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Time: ${show.startTime.replace("T", " ").take(16)}",
                    style = CinemaTypography.bodySmall,
                    color = TextSecondary
                )
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
fun ShowFormDialog(
    show: ShowDto?,
    movies: List<MovieDto>,
    onDismiss: () -> Unit,
    onSubmit: (movieId: Int, screenId: Int, startTime: String, endTime: String, price: Double) -> Unit
) {
    var selectedMovieId by remember { mutableStateOf(show?.movieId ?: movies.firstOrNull()?.id ?: 1) }
    var selectedScreenId by remember { mutableStateOf(show?.screenId ?: 1) }
    var startTime by remember { mutableStateOf(show?.startTime ?: "2026-10-06T18:00:00") }
    var endTime by remember { mutableStateOf(show?.endTime ?: "2026-10-06T20:30:00") }
    var basePriceStr by remember { mutableStateOf(show?.basePrice?.toString() ?: "250.0") }

    var movieDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (show == null) "Add New Show" else "Edit Show",
                style = CinemaTypography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select Movie Dropdown
                Text("Select Movie", style = CinemaTypography.bodySmall, color = TextSecondary)
                Box {
                    val currentMovieTitle = movies.find { it.id == selectedMovieId }?.title ?: "Select Movie"
                    OutlinedButton(
                        onClick = { movieDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentMovieTitle, style = CinemaTypography.bodyMedium)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = CinemaGold)
                        }
                    }
                    DropdownMenu(
                        expanded = movieDropdownExpanded,
                        onDismissRequest = { movieDropdownExpanded = false }
                    ) {
                        movies.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.title) },
                                onClick = {
                                    selectedMovieId = m.id
                                    movieDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Select Screen
                Text("Select Screen", style = CinemaTypography.bodySmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedScreenId == 1,
                        onClick = { selectedScreenId = 1 },
                        label = { Text("Screen 1 (Standard)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = BackgroundDeep
                        )
                    )
                    FilterChip(
                        selected = selectedScreenId == 2,
                        onClick = { selectedScreenId = 2 },
                        label = { Text("Screen 2 (VIP)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = BackgroundDeep
                        )
                    )
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (YYYY-MM-DDTHH:MM:SS)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time (YYYY-MM-DDTHH:MM:SS)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )

                OutlinedTextField(
                    value = basePriceStr,
                    onValueChange = { basePriceStr = it },
                    label = { Text("Ticket Price (₹)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = basePriceStr.toDoubleOrNull() ?: 250.0
                    onSubmit(selectedMovieId, selectedScreenId, startTime, endTime, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = BackgroundDeep)
            ) {
                Text(if (show == null) "Create" else "Save")
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
