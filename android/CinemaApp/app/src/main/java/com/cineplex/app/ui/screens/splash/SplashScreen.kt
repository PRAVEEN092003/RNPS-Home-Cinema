package com.cineplex.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cineplex.app.R
import com.cineplex.app.ui.theme.*
import com.cineplex.app.util.CinematicAudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var hasNavigated by remember { mutableStateOf(false) }

    // Clean audio teardown on screen exit
    DisposableEffect(Unit) {
        onDispose {
            CinematicAudioPlayer.stop()
        }
    }

    // Play synchronized bass pulse
    LaunchedEffect(isMuted) {
        if (!isMuted) {
            delay(200)
            CinematicAudioPlayer.playSubwooferBass(context, isMuted)
        }
    }

    // Safe navigation with short cinematic intro (~0.5s total)
    LaunchedEffect(destination) {
        if (destination !is SplashDestination.Loading && !hasNavigated) {
            delay(300) // Brief brand impression without stalling user
            hasNavigated = true
            when (destination) {
                SplashDestination.Home -> onNavigateToHome()
                SplashDestination.Login -> onNavigateToLogin()
                SplashDestination.Loading -> {}
            }
        }
    }

    fun proceedImmediately() {
        if (!hasNavigated) {
            hasNavigated = true
            CinematicAudioPlayer.stop()
            when (destination) {
                SplashDestination.Home -> onNavigateToHome()
                SplashDestination.Login -> onNavigateToLogin()
                SplashDestination.Loading -> {
                    // If still loading session synchronously, proceed to Home if token exists
                    onNavigateToHome()
                }
            }
        }
    }

    // Speaker Bass Thump Animations
    val infiniteTransition = rememberInfiniteTransition(label = "speakerBass")

    // Bass cone oscillation (kick & decay)
    val coneScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coneScale"
    )

    // Expanding sound wave 1
    val wave1Radius by infiniteTransition.animateFloat(
        initialValue = 70f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )

    // Expanding sound wave 2
    val wave2Radius by infiniteTransition.animateFloat(
        initialValue = 70f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 280, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 280, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )

    // Overall entrance fade
    var enterAnimation by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (enterAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "enterAlpha"
    )

    LaunchedEffect(Unit) {
        enterAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141414),
                        DeepBlack,
                        DeepBlack
                    ),
                    radius = 1100f
                )
            )
            .clickable { proceedImmediately() },
        contentAlignment = Alignment.Center
    ) {
        // Sound Mute Toggle (Top-End)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    if (isMuted) CinematicAudioPlayer.stop()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BackgroundCard.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) TextSecondary else CinemaGold
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(enterAlpha)
                .padding(horizontal = 24.dp)
        ) {
            // Animated Subwoofer & Sound Waves Canvas
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 1. Expanding Sound Waves (Gold pulsing)
                    if (wave1Alpha > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CinemaGold.copy(alpha = wave1Alpha * 0.5f),
                                    CinemaGoldDark.copy(alpha = wave1Alpha * 0.2f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = wave1Radius
                            ),
                            radius = wave1Radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    if (wave2Alpha > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CinemaGoldLight.copy(alpha = wave2Alpha * 0.35f),
                                    CinemaGold.copy(alpha = wave2Alpha * 0.15f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = wave2Radius
                            ),
                            radius = wave2Radius,
                            center = center,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )
                    }

                    // 2. Subwoofer Outer Enclosure (Dark Black Rim)
                    val outerRadius = 88.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF282018), Color(0xFF141008), Color(0xFF080808)),
                            center = center,
                            radius = outerRadius
                        ),
                        radius = outerRadius,
                        center = center
                    )

                    // Gold rim accent
                    drawCircle(
                        color = CinemaGold.copy(alpha = 0.55f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 3. Subwoofer Surround Roll (Rubber Ring)
                    val surroundRadius = 72.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0A0A0A), Color(0xFF1A1408), Color(0xFF0A0A0A)),
                            center = center,
                            radius = surroundRadius
                        ),
                        radius = surroundRadius,
                        center = center,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // 4. Oscillating Speaker Cone
                    val coneRadius = 56.dp.toPx() * coneScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2A2015),
                                Color(0xFF141008),
                                Color(0xFF080808)
                            ),
                            center = center,
                            radius = coneRadius
                        ),
                        radius = coneRadius,
                        center = center
                    )

                    // Inner Dome (Dust Cap with Gold glow)
                    val capRadius = 24.dp.toPx() * coneScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CinemaGoldLight.copy(alpha = 0.8f),
                                CinemaGold.copy(alpha = 0.5f),
                                Color(0xFF080808)
                            ),
                            center = center - androidx.compose.ui.geometry.Offset(3f, 3f),
                            radius = capRadius
                        ),
                        radius = capRadius,
                        center = center
                    )

                    // Center Logo Accent dot
                    drawCircle(
                        color = CinemaGold,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography: RNPS
            Text(
                text = "RNPS",
                style = CinemaTypography.displayLarge.copy(
                    color = TextPrimary,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cinematic Gold HOME CINEMA Badge
            Surface(
                color = CinemaGold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "HOME CINEMA",
                    style = CinemaTypography.titleMedium.copy(
                        color = CinemaGold,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-branding tagline: Dolby Atmos & 4K Laser
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "13-CH DOLBY ATMOS",
                    style = CinemaTypography.bodySmall.copy(
                        color = CinemaGold,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = "  •  ",
                    color = CinemaGoldDark
                )
                Text(
                    text = "4K LASER PROJECTION",
                    style = CinemaTypography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }

        // Tap to continue hint at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Tap anywhere to enter",
                style = CinemaTypography.bodySmall.copy(
                    color = TextDisabled,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
