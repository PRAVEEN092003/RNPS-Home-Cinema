package com.cineplex.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.DateTimeUtils
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
                title = { Text("Show Management", style = CinemaTypography.headlineSmall, color = TextPrimary) },
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
                contentColor = TextOnGold
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
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BorderSubtle)
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
                    text = "${show.screen?.name ?: "Screen #${show.screenId}"} • Price: ₹${String.format(java.util.Locale.ENGLISH, "%.2f", show.basePrice)}",
                    style = CinemaTypography.bodyMedium,
                    color = ElectricBlueLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateTimeUtils.formatShowDate(show.startTime)} • ${DateTimeUtils.formatShowTime(show.startTime)}",
                    style = CinemaTypography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = ElectricCinemaBlue)
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
    onSubmit: (movieId: Int, screenId: Int, startTime: String, endTime: String?, price: Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedMovieId by remember { mutableStateOf(show?.movieId ?: movies.firstOrNull()?.id ?: 1) }
    var selectedScreenId by remember { mutableStateOf(show?.screenId ?: 1) }

    // Parse initial date & time safely without timezone shift
    val initialDt = remember(show) {
        if (show != null) DateTimeUtils.parseLocalDateTime(show.startTime)
        else java.time.LocalDateTime.now().plusDays(1).withHour(18).withMinute(0)
    }

    var selectedDate by remember { mutableStateOf(initialDt.toLocalDate()) }
    val (initialHour12, initialIsAm) = remember(initialDt) {
        DateTimeUtils.to12Hour(initialDt.hour)
    }
    var selectedHour12 by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialDt.minute) }
    var isAm by remember { mutableStateOf(initialIsAm) }

    var basePriceStr by remember { mutableStateOf(show?.basePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var priceError by remember { mutableStateOf<String?>(null) }
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
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select Movie Dropdown
                Text("Select Movie", style = CinemaTypography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Box {
                    val currentMovieTitle = movies.find { it.id == selectedMovieId }?.title ?: "Select Movie"
                    OutlinedButton(
                        onClick = { movieDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentMovieTitle, style = CinemaTypography.bodyMedium)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricCinemaBlue)
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
                Text("Select Screen", style = CinemaTypography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedScreenId == 1,
                        onClick = { selectedScreenId = 1 },
                        label = { Text("Screen 1 (Standard)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = TextOnGold,
                            containerColor = BackgroundCard,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = selectedScreenId == 2,
                        onClick = { selectedScreenId = 2 },
                        label = { Text("Screen 2 (VIP)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = TextOnGold,
                            containerColor = BackgroundCard,
                            labelColor = TextSecondary
                        )
                    )
                }

                // Show Date Selection
                Text("Show Date", style = CinemaTypography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            )
                            dialog.show()
                        },
                    color = BackgroundCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = ElectricCinemaBlue)
                            Text(
                                text = DateTimeUtils.formatShowDate(selectedDate),
                                style = CinemaTypography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Change",
                            style = CinemaTypography.labelSmall.copy(color = ElectricCinemaBlue, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Show Time Selection (12-Hour AM/PM)
                Text("Show Time (12-Hour AM/PM)", style = CinemaTypography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatTime12H(selectedHour12, selectedMinute, isAm),
                                style = CinemaTypography.headlineMedium.copy(color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                            )
                            OutlinedButton(
                                onClick = {
                                    val currentH24 = DateTimeUtils.to24Hour(selectedHour12, isAm)
                                    val timeDialog = android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minuteOfHour ->
                                            val (h12, am) = DateTimeUtils.to12Hour(hourOfDay)
                                            selectedHour12 = h12
                                            selectedMinute = minuteOfHour
                                            isAm = am
                                        },
                                        currentH24,
                                        selectedMinute,
                                        false // 12-hour format with AM/PM selector
                                    )
                                    timeDialog.show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCinemaBlue),
                                border = BorderStroke(1.dp, BorderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clock", style = CinemaTypography.labelSmall)
                            }
                        }

                        // Quick controls for Hour, Minute, AM/PM
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Dropdown (01..12)
                            var hourExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { hourExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.ENGLISH, "%02d", selectedHour12),
                                        style = CinemaTypography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                DropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                                    (1..12).forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text(String.format(java.util.Locale.ENGLISH, "%02d", h)) },
                                            onClick = {
                                                selectedHour12 = h
                                                hourExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Minute Dropdown (00, 05, 10, ..., 55)
                            var minuteExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { minuteExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.ENGLISH, "%02d", selectedMinute),
                                        style = CinemaTypography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                DropdownMenu(expanded = minuteExpanded, onDismissRequest = { minuteExpanded = false }) {
                                    listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(String.format(java.util.Locale.ENGLISH, "%02d", m)) },
                                            onClick = {
                                                selectedMinute = m
                                                minuteExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // AM / PM Toggle
                            Row(modifier = Modifier.weight(1.3f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = isAm,
                                    onClick = { isAm = true },
                                    label = { Text("AM", fontWeight = FontWeight.Bold, style = CinemaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCinemaBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !isAm,
                                    onClick = { isAm = false },
                                    label = { Text("PM", fontWeight = FontWeight.Bold, style = CinemaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCinemaBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Base Ticket Price
                OutlinedTextField(
                    value = basePriceStr,
                    onValueChange = {
                        basePriceStr = it
                        priceError = null
                    },
                    label = { Text("Ticket Price (₹)") },
                    isError = priceError != null,
                    supportingText = priceError?.let { err -> { Text(err, color = ErrorRed) } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCinemaBlue,
                        focusedLabelColor = ElectricBlueLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = basePriceStr.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        priceError = "Please enter a valid ticket price (greater than ₹0)"
                        return@Button
                    }
                    val startIso = DateTimeUtils.createIsoDateTime(selectedDate, selectedHour12, selectedMinute, isAm)
                    val selectedMovie = movies.find { it.id == selectedMovieId }
                    val durationMins = selectedMovie?.durationMinutes ?: 150
                    val startDt = DateTimeUtils.parseLocalDateTime(startIso)
                    val endDt = startDt.plusMinutes(durationMins.toLong())
                    val endIso = endDt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    onSubmit(selectedMovieId, selectedScreenId, startIso, endIso, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = TextOnGold)
            ) {
                Text(if (show == null) "Create" else "Save", fontWeight = FontWeight.Bold)
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
