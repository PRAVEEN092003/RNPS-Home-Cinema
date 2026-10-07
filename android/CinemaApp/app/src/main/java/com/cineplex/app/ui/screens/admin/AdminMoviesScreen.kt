package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMoviesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminMoviesViewModel = hiltViewModel()
) {
    val moviesState by viewModel.moviesState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingMovie by remember { mutableStateOf<MovieDto?>(null) }

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
                title = { Text("Movie Management", style = CinemaTypography.headlineSmall) },
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
                    editingMovie = null
                    showDialog = true
                },
                containerColor = CinemaGold,
                contentColor = BackgroundDeep
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Movie")
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
            when (val state = moviesState) {
                is UiState.Loading -> LoadingStateView(message = "Loading movies...")
                is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadMovies() })
                is UiState.Empty -> ErrorStateView(message = "No movies found", onRetry = { viewModel.loadMovies() })
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.data) { movie ->
                            MovieAdminCard(
                                movie = movie,
                                onEdit = {
                                    editingMovie = movie
                                    showDialog = true
                                },
                                onDelete = { viewModel.deleteMovie(movie.id) }
                            )
                        }
                    }
                }
                else -> {}
            }

            if (showDialog) {
                MovieFormDialog(
                    movie = editingMovie,
                    onDismiss = { showDialog = false },
                    onSubmit = { title, desc, duration, lang, genre, rating, poster, audioTech, displayTech ->
                        if (editingMovie == null) {
                            viewModel.createMovie(title, desc, duration, lang, genre, rating, poster, audioTech, displayTech)
                        } else {
                            viewModel.updateMovie(editingMovie!!.id, title, desc, duration, lang, genre, rating, poster, audioTech, displayTech)
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun MovieAdminCard(
    movie: MovieDto,
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
                        text = movie.title,
                        style = CinemaTypography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (movie.isActive) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (movie.isActive) "Active" else "Deactivated",
                            style = CinemaTypography.labelMedium,
                            color = if (movie.isActive) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${movie.language} • ${movie.durationMinutes} mins • ${movie.rating ?: "U/A"}",
                    style = CinemaTypography.bodySmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CinemaGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = movie.displayTechnology,
                            style = CinemaTypography.labelSmall,
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (movie.audioTechnology.equals("Dolby Atmos", ignoreCase = true)) CinemaGold.copy(alpha = 0.25f) else BackgroundElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = movie.audioTechnology,
                            style = CinemaTypography.labelSmall,
                            color = if (movie.audioTechnology.equals("Dolby Atmos", ignoreCase = true)) CinemaGold else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!movie.genre.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Genre: ${movie.genre}",
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
fun MovieFormDialog(
    movie: MovieDto?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, duration: Int, lang: String, genre: String, rating: String, poster: String, audioTech: String, displayTech: String) -> Unit
) {
    var title by remember { mutableStateOf(movie?.title ?: "") }
    var description by remember { mutableStateOf(movie?.description ?: "") }
    var duration by remember { mutableStateOf(movie?.durationMinutes?.toString() ?: "120") }
    var language by remember { mutableStateOf(movie?.language ?: "English") }
    var genre by remember { mutableStateOf(movie?.genre ?: "Action") }
    var rating by remember { mutableStateOf(movie?.rating ?: "U/A") }
    var posterUrl by remember { mutableStateOf(movie?.posterUrl ?: "") }
    var audioTechnology by remember { mutableStateOf(movie?.audioTechnology ?: "Dolby Atmos") }
    var displayTechnology by remember { mutableStateOf(movie?.displayTechnology ?: "4K") }

    val audioOptions = listOf("Dolby Atmos", "Dolby Digital 5.1", "DTS", "DTS-HD", "7.1 Surround", "Stereo")
    val displayOptions = listOf("4K", "2K", "Full HD")

    var audioDropdownExpanded by remember { mutableStateOf(false) }
    var displayDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (movie == null) "Add New Movie" else "Edit Movie",
                style = CinemaTypography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                    )
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Rating (e.g. U/A)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                    )
                }

                // Audio Technology Selection
                ExposedDropdownMenuBox(
                    expanded = audioDropdownExpanded,
                    onExpandedChange = { audioDropdownExpanded = !audioDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = audioTechnology,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Audio Technology") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaGold,
                            focusedLabelColor = CinemaGold
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = audioDropdownExpanded,
                        onDismissRequest = { audioDropdownExpanded = false },
                        modifier = Modifier.background(BackgroundElevated)
                    ) {
                        audioOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = if (option == audioTechnology) CinemaGold else TextPrimary,
                                        fontWeight = if (option == audioTechnology) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    audioTechnology = option
                                    audioDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Display Technology Selection
                ExposedDropdownMenuBox(
                    expanded = displayDropdownExpanded,
                    onExpandedChange = { displayDropdownExpanded = !displayDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = displayTechnology,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Display Technology") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = displayDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaGold,
                            focusedLabelColor = CinemaGold
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = displayDropdownExpanded,
                        onDismissRequest = { displayDropdownExpanded = false },
                        modifier = Modifier.background(BackgroundElevated)
                    ) {
                        displayOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = if (option == displayTechnology) CinemaGold else TextPrimary,
                                        fontWeight = if (option == displayTechnology) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    displayTechnology = option
                                    displayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = posterUrl,
                    onValueChange = { posterUrl = it },
                    label = { Text("Poster Image URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, focusedLabelColor = CinemaGold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationInt = duration.toIntOrNull() ?: 120
                    if (title.isNotBlank()) {
                        onSubmit(title, description, durationInt, language, genre, rating, posterUrl, audioTechnology, displayTechnology)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = BackgroundDeep)
            ) {
                Text(if (movie == null) "Create" else "Save")
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
