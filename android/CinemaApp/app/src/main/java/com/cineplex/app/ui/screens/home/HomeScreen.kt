package com.cineplex.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.cineplex.app.R
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.ui.components.CinemaImage
import com.cineplex.app.ui.components.EmptyStateView
import com.cineplex.app.ui.components.ErrorStateView
import com.cineplex.app.ui.components.ListLoadingSkeleton
import com.cineplex.app.ui.components.OfflineStateView
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToMovieDetail: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val moviesState by viewModel.moviesState.collectAsState()
    val user by viewModel.userState.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadData(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = CinemaTypography.titleMedium.copy(
                                color = CinemaGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = if (user != null) stringResource(id = R.string.welcome_user, user?.name ?: "") else stringResource(id = R.string.now_showing),
                            style = CinemaTypography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    if (user?.role == "ADMIN") {
                        Surface(
                            color = CinemaGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.admin_badge),
                                style = CinemaTypography.labelSmall.copy(color = CinemaGold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(id = R.string.profile),
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
            when (val state = moviesState) {
                is UiState.Loading -> ListLoadingSkeleton(count = 5, itemHeight = 140.dp)
                is UiState.Offline -> OfflineStateView(onRetry = { viewModel.loadData() })
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadData() }
                )
                is UiState.Empty -> EmptyStateView(message = stringResource(id = R.string.no_movies_scheduled))
                is UiState.Success -> {
                    val movies = state.data
                    val currentlyRunning = movies.filter { !it.isUpcoming }
                    val upcoming = movies.filter { it.isUpcoming }

                    if (movies.isEmpty()) {
                        EmptyStateView(message = stringResource(id = R.string.no_movies_scheduled))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // ─── Currently Running Movies (Top Row) ───────────
                            item {
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.currently_running_movies),
                                        style = CinemaTypography.headlineMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    if (currentlyRunning.isEmpty()) {
                                        Text(
                                            text = stringResource(id = R.string.no_currently_running),
                                            style = CinemaTypography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    } else {
                                        androidx.compose.foundation.lazy.LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(currentlyRunning, key = { it.id }) { movie ->
                                                CurrentlyRunningMovieCard(
                                                    movie = movie,
                                                    onClick = { onNavigateToMovieDetail(movie.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ─── Upcoming Movies (Below) ──────────────────────
                            item {
                                Text(
                                    text = stringResource(id = R.string.upcoming_movies),
                                    style = CinemaTypography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                )
                            }

                            if (upcoming.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(id = R.string.no_upcoming_scheduled),
                                        style = CinemaTypography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                items(upcoming, key = { it.id }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onNavigateToMovieDetail(movie.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: MovieDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image with fallback
            CinemaImage(
                imageUrl = movie.posterUrl,
                contentDescription = movie.title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier
                    .size(width = 90.dp, height = 130.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    style = CinemaTypography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    movie.genre?.let {
                        Text(
                            text = it,
                            style = CinemaTypography.bodySmall,
                            color = CinemaGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(id = R.string.duration_minutes_suffix, movie.durationMinutes),
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                movie.description?.let {
                    Text(
                        text = it,
                        style = CinemaTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        movie.rating?.let {
                            Surface(
                                color = BackgroundElevated,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = it,
                                    style = CinemaTypography.labelSmall.copy(color = TextPrimary),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = CinemaGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = movie.displayTechnology,
                                style = CinemaTypography.labelSmall.copy(color = CinemaGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = if (movie.audioTechnology.equals("Dolby Atmos", ignoreCase = true)) CinemaGold.copy(alpha = 0.25f) else BackgroundElevated,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = movie.audioTechnology,
                                style = CinemaTypography.labelSmall.copy(
                                    color = if (movie.audioTechnology.equals("Dolby Atmos", ignoreCase = true)) CinemaGold else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    movie.imdbRating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = stringResource(id = R.string.rating_label),
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$rating",
                                style = CinemaTypography.labelMedium.copy(color = TextPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentlyRunningMovieCard(
    movie: MovieDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CinemaGold.copy(alpha = 0.4f))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            CinemaImage(
                imageUrl = movie.posterUrl,
                contentDescription = movie.title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.title,
                style = CinemaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(id = R.string.duration_genre_format, movie.durationMinutes, movie.genre ?: "Cinema"),
                style = CinemaTypography.bodySmall,
                color = CinemaGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
