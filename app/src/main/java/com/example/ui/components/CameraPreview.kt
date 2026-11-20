package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ForestGreen
import java.io.File

private const val TAG = "CameraPreview"

/**
 * In-app CameraX live preview & shutter component for capturing leaf photos directly.
 */
@Composable
fun CameraPreview(
    onImageCaptured: (Uri) -> Unit,
    onCaptureError: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    BackHandler(enabled = !isCapturing) {
        onClose()
    }

    DisposableEffect(lifecycleOwner, lensFacing) {
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                runCatching {
                    val cameraProvider = providerFuture.get()
                    cameraProviderRef = cameraProvider

                    val hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    val hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    canSwitchCamera = hasBack && hasFront

                    val targetLens = when {
                        lensFacing == CameraSelector.LENS_FACING_BACK && hasBack -> CameraSelector.LENS_FACING_BACK
                        lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
                        hasBack -> CameraSelector.LENS_FACING_BACK
                        hasFront -> CameraSelector.LENS_FACING_FRONT
                        else -> {
                            onCaptureError("Perangkat tidak memiliki kamera yang aktif. Gunakan tombol Pilih Foto atau Berkas.")
                            return@addListener
                        }
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(targetLens)
                        .build()

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(rotation)
                        .build()

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )

                    imageCapture = capture
                    boundCamera = camera
                    hasFlash = camera.cameraInfo.hasFlashUnit()
                    torchEnabled = false
                }.onFailure { err ->
                    Log.e(TAG, "Failed to bind CameraX use cases", err)
                    onCaptureError("Gagal membuka kamera: ${err.localizedMessage ?: "Kamera tidak tersedia"}")
                }
            },
            mainExecutor
        )

        onDispose {
            disposed = true
            runCatching {
                boundCamera?.cameraControl?.enableTorch(false)
                cameraProviderRef?.unbindAll()
            }
            boundCamera = null
            imageCapture = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .testTag("camera_preview_container")
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle leaf framing corner guides
        LeafFramingOverlay(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
        )

        // Top controls (Close, Guide Pill, Flash & Switch Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (!isCapturing) onClose() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camera_preview_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Tutup kamera"
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Arahkan daun ke dalam bingkai",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasFlash) {
                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            torchEnabled = next
                            boundCamera?.cameraControl?.enableTorch(next)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            contentColor = if (torchEnabled) Color(0xFFFFD54F) else Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("camera_preview_flash_button")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = if (torchEnabled) "Matikan lampu flash" else "Nyalakan lampu flash"
                        )
                    }
                }
                if (canSwitchCamera) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("camera_preview_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cameraswitch,
                            contentDescription = "Ganti kamera"
                        )
                    }
                }
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCapturing) {
                    "Mengambil foto daun…"
                } else {
                    "Ketuk tombol hijau untuk foto & analisis AI otomatis"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .semantics(mergeDescendants = true) {},
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture
                        if (capture == null || isCapturing) return@IconButton
                        isCapturing = true
                        val outputFile = createCameraCaptureFile(context)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                        capture.takePicture(
                            outputOptions,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    val savedUri = outputFileResults.savedUri ?: FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        outputFile
                                    )
                                    onImageCaptured(savedUri)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    Log.e(TAG, "Image capture failed", exception)
                                    onCaptureError(
                                        "Gagal mengambil foto: ${exception.localizedMessage ?: "Kesalahan kamera"}"
                                    )
                                }
                            }
                        )
                    },
                    enabled = !isCapturing,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_preview_capture_button")
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Ambil foto daun",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeafFramingOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 4.dp.toPx()
        val cornerLen = 32.dp.toPx()
        val guideColor = Color(0xFF9BE7A5)

        // Top-left
        drawLine(guideColor, Offset(0f, 0f), Offset(cornerLen, 0f), stroke, cap = StrokeCap.Round)
        drawLine(guideColor, Offset(0f, 0f), Offset(0f, cornerLen), stroke, cap = StrokeCap.Round)
        // Top-right
        drawLine(guideColor, Offset(size.width - cornerLen, 0f), Offset(size.width, 0f), stroke, cap = StrokeCap.Round)
        drawLine(guideColor, Offset(size.width, 0f), Offset(size.width, cornerLen), stroke, cap = StrokeCap.Round)
        // Bottom-left
        drawLine(guideColor, Offset(0f, size.height), Offset(cornerLen, size.height), stroke, cap = StrokeCap.Round)
        drawLine(guideColor, Offset(0f, size.height - cornerLen), Offset(0f, size.height), stroke, cap = StrokeCap.Round)
        // Bottom-right
        drawLine(guideColor, Offset(size.width - cornerLen, size.height), Offset(size.width, size.height), stroke, cap = StrokeCap.Round)
        drawLine(guideColor, Offset(size.width, size.height - cornerLen), Offset(size.width, size.height), stroke, cap = StrokeCap.Round)
    }
}

private fun createCameraCaptureFile(context: Context): File {
    val cacheDir = File(context.cacheDir, "camera_cache").apply { mkdirs() }
    return File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
}
