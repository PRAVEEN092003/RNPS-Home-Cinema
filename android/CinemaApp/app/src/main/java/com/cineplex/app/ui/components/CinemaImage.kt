package com.cineplex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.cineplex.app.ui.theme.BackgroundCard
import com.cineplex.app.ui.theme.BackgroundSurface
import com.cineplex.app.ui.theme.CinemaGold
import com.cineplex.app.ui.theme.TextDisabled

/**
 * Premium async image loader with shimmer/spinner loading state and broken image fallback.
 */
@Composable
fun CinemaImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Default.Movie,
) {
    Box(
        modifier = modifier
            .background(BackgroundSurface),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundCard),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CinemaGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = fallbackIcon,
                                contentDescription = contentDescription ?: "Image load failed",
                                tint = TextDisabled,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription ?: "No image available",
                tint = TextDisabled,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
