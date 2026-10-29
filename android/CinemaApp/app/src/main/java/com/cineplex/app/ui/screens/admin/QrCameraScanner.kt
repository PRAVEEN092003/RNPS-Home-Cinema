package com.cineplex.app.ui.screens.admin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cineplex.app.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrCameraScanner(
    onQrCodeDetected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDeniedPermanently = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreviewWithScanner(
                onQrCodeDetected = { code ->
                    triggerHapticFeedback(context)
                    onQrCodeDetected(code)
                },
                onClose = onClose
            )
        } else {
            // Permission denied UI with option to retry or cancel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = CinemaGold,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = CinemaTypography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To scan ticket QR codes quickly, please allow access to your device camera.",
                    style = CinemaTypography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) {
                    Text("Use Manual Entry Instead", color = CinemaGoldLight)
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithScanner(
    onQrCodeDetected: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    val isScanningDebounced = remember { AtomicBoolean(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isScanningDebounced.get()) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.valueType == Barcode.TYPE_TEXT ||
                                            barcode.format == Barcode.FORMAT_QR_CODE
                                        ) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank() && isScanningDebounced.compareAndSet(false, true)) {
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onQrCodeDetected(rawValue)
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Reticle target overlay
        ScannerOverlay()

        // Top Control Bar (Torch & Close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Scanner",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    val targetTorch = !isTorchOn
                    camera?.cameraControl?.enableTorch(targetTorch)
                    isTorchOn = targetTorch
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isTorchOn) CinemaGold.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Torch",
                    tint = if (isTorchOn) Color.Black else Color.White
                )
            }
        }

        // Bottom instruction label
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Align ticket QR code inside the target frame",
                    style = CinemaTypography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = size.width * 0.72f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f - 40.dp.toPx()
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerLength = 36.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // 1. Semi-transparent black scrim background with square cutout
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            size = size
        )
        // Cutout target box
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 2. Cinema Gold Frame Border
        drawRoundRect(
            color = CinemaGold.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // 3. Prominent Gold Corner Brackets
        val goldColor = CinemaGold
        // Top-Left
        drawLine(goldColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(goldColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
        // Top-Right
        drawLine(goldColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(goldColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
        // Bottom-Left
        drawLine(goldColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(goldColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
        // Bottom-Right
        drawLine(goldColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(goldColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

        // 4. Animated Laser scanning line
        val laserY = top + (boxSize * laserProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    CinemaGoldLight,
                    CinemaGold,
                    CinemaGoldLight,
                    Color.Transparent
                ),
                startX = left,
                endX = right
            ),
            start = Offset(left + 6.dp.toPx(), laserY),
            end = Offset(right - 6.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

private fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        }
    } catch (e: Exception) {
        // Fallback silently if vibration permission or service is unavailable
    }
}
