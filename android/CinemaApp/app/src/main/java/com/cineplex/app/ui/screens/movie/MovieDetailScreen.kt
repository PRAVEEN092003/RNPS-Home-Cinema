package com.cineplex.app.ui.screens.movie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.ui.components.CinemaButton
import com.cineplex.app.ui.components.CinemaImage
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.LoadingStateView
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    onNavigateBack: () -> Unit,
    onSelectShow: (Int) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movie Details", style = CinemaTypography.headlineSmall) },
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
            when (val movieState = state.movieState) {
                is UiState.Loading -> LoadingStateView(message = "Loading movie details...")
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadMovieAndShows() })
                is UiState.Error -> ErrorStateView(
                    message = movieState.message,
                    onRetry = { viewModel.loadMovieAndShows() }
                )
                is UiState.Empty -> ErrorStateView(
                    message = "Movie details unavailable.",
                    onRetry = { viewModel.loadMovieAndShows() }
                )
                is UiState.Success -> {
                    MovieDetailContent(
                        movie = movieState.data,
                        showsState = state.showsState,
                        onSelectShow = onSelectShow,
                        onRetryShows = { viewModel.loadMovieAndShows() }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailContent(
    movie: MovieDto,
    showsState: UiState<List<ShowDto>>,
    onSelectShow: (Int) -> Unit,
    onRetryShows: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Banner / Poster Header
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            CinemaImage(
                imageUrl = movie.posterUrl,
                contentDescription = movie.title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier
                    .size(width = 120.dp, height = 175.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = CinemaTypography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                movie.genre?.let {
                    Text(
                        text = it,
                        style = CinemaTypography.bodyMedium,
                        color = CinemaGold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${movie.durationMinutes} minutes",
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                movie.imdbRating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating / 10",
                            style = CinemaTypography.titleMedium.copy(color = TextPrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Audio & Display Technology Highlight Section
        MovieTechnologyHighlight(movie = movie)

        Spacer(modifier = Modifier.height(24.dp))

        // Description Section
        movie.description?.let {
            Text(
                text = "Synopsis",
                style = CinemaTypography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = CinemaTypography.bodyMedium,
                color = TextSecondary,
                lineHeight = CinemaTypography.bodyMedium.lineHeight
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Director & Cast
        movie.director?.let {
            Text(
                text = "Director: $it",
                style = CinemaTypography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        movie.cast?.let {
            Text(
                text = "Cast: $it",
                style = CinemaTypography.bodyMedium.copy(color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Showtimes Section
        Text(
            text = "Select Show Time",
            style = CinemaTypography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (showsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CinemaGold)
                }
            }
            is UiState.Error -> {
                Text("Failed to load showtimes.", color = ErrorRed, style = CinemaTypography.bodyMedium)
            }
            is UiState.Offline -> {
                Text("No internet connection to fetch showtimes.", color = WarningAmber, style = CinemaTypography.bodyMedium)
            }
            is UiState.Empty -> {
                Text("No shows currently scheduled for this movie.", color = TextSecondary, style = CinemaTypography.bodyMedium)
            }
            is UiState.Success -> {
                if (showsState.data.isEmpty()) {
                    Text("No shows currently scheduled for this movie.", color = TextSecondary, style = CinemaTypography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        showsState.data.forEach { show ->
                            ShowTimeCard(
                                show = show,
                                onBookTickets = { onSelectShow(show.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShowTimeCard(
    show: ShowDto,
    onBookTickets: () -> Unit,
) {
    val screen = show.screen
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.5f)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = screen?.name ?: "Screen 1",
                        style = CinemaTypography.titleMedium.copy(color = CinemaGold, fontWeight = FontWeight.Bold)
                    )
                    screen?.let { s ->
                        Text(
                            text = "${s.displaySpec} | ${s.audioSpec}",
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (screen?.hasDolbyAtmos == true) {
                    Surface(
                        color = CinemaGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DOLBY ATMOS",
                            style = CinemaTypography.labelSmall.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeString = show.startTime.split("T").getOrNull(1)?.take(5) ?: show.startTime
                Column {
                    Text(
                        text = timeString,
                        style = CinemaTypography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ticket Price: ₹${String.format("%.2f", show.basePrice)}",
                        style = CinemaTypography.bodySmall,
                        color = CinemaGold
                    )
                }

                CinemaButton(
                    text = "BOOK TICKETS",
                    onClick = onBookTickets,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
fun MovieTechnologyHighlight(movie: MovieDto) {
    val isDolbyAtmos = movie.audioTechnology.equals("Dolby Atmos", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDolbyAtmos) BackgroundElevated else BackgroundCard
        ),
        border = BorderStroke(
            1.dp,
            if (isDolbyAtmos) {
                Brush.horizontalGradient(
                    listOf(
                        CinemaGold,
                        CinemaGold.copy(alpha = 0.5f),
                        CinemaGold.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        BackgroundElevated,
                        BackgroundCard
                    )
                )
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CINEMA EXPERIENCE",
                    style = CinemaTypography.labelSmall.copy(
                        color = CinemaGold,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                )

                Surface(
                    color = CinemaGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = movie.displayTechnology.uppercase(),
                        style = CinemaTypography.labelMedium.copy(
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isDolbyAtmos) {
                // Official Dolby Atmos Branding Presentation (Preserving official logo without modification)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dolby_atmos),
                            contentDescription = "Dolby Atmos Official Logo",
                            modifier = Modifier
                                .height(26.dp)
                                .width(155.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Breathtaking multi-dimensional sound immersion",
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = CinemaGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "ATMOS",
                            style = CinemaTypography.labelSmall.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Clean technology badge without the Dolby logo for other audio formats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.audioTechnology.uppercase(),
                            style = CinemaTypography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "High-fidelity surround audio format",
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = BackgroundElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "AUDIO",
                            style = CinemaTypography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
