package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ScanRecordEntity
import com.example.ui.components.CameraPreview
import com.example.ui.components.DiagnosisDetailContent
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.EarthBrown
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HealthyLeaf
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.SandContainer
import com.example.ui.viewmodel.ScanUiState

@Composable
fun ScanLeafScreen(
    selectedImageUri: Uri?,
    scanState: ScanUiState,
    onImagesSelected: (List<Uri>) -> Unit,
    onSelectBatchRecord: (ScanRecordEntity) -> Unit,
    onAnalyzeClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCameraPreview by rememberSaveable { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showOpenSettingsAction by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            feedbackMessage = null
            showOpenSettingsAction = false
            showCameraPreview = true
        } else {
            showOpenSettingsAction = true
            feedbackMessage = "Izin kamera ditolak. Aktifkan izin kamera di Pengaturan atau pilih foto daun dari Galeri/Berkas."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            feedbackMessage = null
            onImagesSelected(uris)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            feedbackMessage = null
            onImagesSelected(uris)
        }
    }

    fun launchCamera() {
        feedbackMessage = null
        showOpenSettingsAction = false
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            showCameraPreview = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        feedbackMessage = null
        showOpenSettingsAction = false
        runCatching {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure {
            documentPickerLauncher.launch(arrayOf("image/*"))
        }
    }

    fun launchFiles() {
        feedbackMessage = null
        showOpenSettingsAction = false
        documentPickerLauncher.launch(arrayOf("image/*"))
    }

    if (showCameraPreview) {
        Dialog(
            onDismissRequest = { showCameraPreview = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            CameraPreview(
                onImageCaptured = { uri ->
                    showCameraPreview = false
                    feedbackMessage = null
                    onImagesSelected(listOf(uri))
                },
                onCaptureError = { errorMsg ->
                    showCameraPreview = false
                    feedbackMessage = errorMsg
                },
                onClose = { showCameraPreview = false }
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 82.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Deteksi Tanaman",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ambil foto langsung atau masukkan foto daun sebanyak yang Anda perlukan — AI langsung menjelaskan kondisi sehat/sakit, nama penyakit, cara mengobati, dan nama obat yang cocok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (scanState !is ScanUiState.Success) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(134.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(PastelGreenCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Foto daun yang dipilih",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_leaf_scan_guide),
                                            contentDescription = "Panduan memfoto daun tanaman",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = if (selectedImageUri != null) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hasil Deteksi",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = when {
                                            scanState is ScanUiState.Analyzing && scanState.total > 1 ->
                                                "Status: Menganalisis foto ${scanState.current} dari ${scanState.total}…"
                                            scanState is ScanUiState.Analyzing ->
                                                "Status: AI sedang menganalisis daun…"
                                            selectedImageUri != null ->
                                                "Status: Foto siap dianalisis"
                                            else ->
                                                "Status: Siap memindai daun"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedImageUri != null) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Ambil foto langsung atau pilih satu maupun banyak foto dari galeri, hasil penjelasan AI akan muncul otomatis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            ScanSourceButtonsRow(
                                onCameraClick = ::launchCamera,
                                onGalleryClick = ::launchGallery,
                                onFilesClick = ::launchFiles
                            )

                            feedbackMessage?.let { msg ->
                                Surface(
                                    color = SandContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EarthBrown
                                        )
                                        if (showOpenSettingsAction) {
                                            TextButton(
                                                onClick = {
                                                    val intent = Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.fromParts("package", context.packageName, null)
                                                    )
                                                    runCatching { context.startActivity(intent) }
                                                }
                                            ) {
                                                Text("Buka Pengaturan Izin", color = ForestGreen, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (scanState is ScanUiState.Error) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = scanState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedImageUri == null) {
                                        feedbackMessage = "Silakan pilih atau ambil foto daun terlebih dahulu lewat Kamera, Galeri, atau Berkas."
                                    } else {
                                        feedbackMessage = null
                                        onAnalyzeClick()
                                    }
                                },
                                enabled = scanState !is ScanUiState.Analyzing,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("scan_analyze_button")
                            ) {
                                if (scanState is ScanUiState.Analyzing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (scanState.total > 1) {
                                            "Menganalisis foto ${scanState.current} dari ${scanState.total}…"
                                        } else {
                                            stringResource(id = R.string.scan_analyzing)
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (scanState is ScanUiState.Error) "Ulangi Deteksi AI" else "Deteksi Daun",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (scanState is ScanUiState.Success) {
                val record = scanState.record
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                color = PastelGreenCard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (record.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = if (record.isHealthy) HealthyLeaf else AlertCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (scanState.batchRecords.size > 1) {
                                            "${scanState.batchRecords.size} foto daun selesai didiagnosis dan tersimpan ke Riwayat."
                                        } else {
                                            "Diagnosis selesai dan tersimpan ke menu Riwayat."
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ForestGreen
                                    )
                                }
                            }

                            if (scanState.batchRecords.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(scanState.batchRecords) { index, batchItem ->
                                        FilterChip(
                                            selected = batchItem.id == record.id,
                                            onClick = { onSelectBatchRecord(batchItem) },
                                            label = {
                                                Text("Foto ${index + 1}: ${batchItem.plantName}")
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ForestGreen,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            ScanSourceButtonsRow(
                                onCameraClick = ::launchCamera,
                                onGalleryClick = ::launchGallery,
                                onFilesClick = ::launchFiles
                            )

                            DiagnosisDetailContent(record = record)

                            OutlinedButton(
                                onClick = onResetClick,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("scan_another_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Daun Lainnya")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun ScanSourceButtonsRow(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFilesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onCameraClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SandContainer,
                contentColor = EarthBrown
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1.1f)
                .heightIn(min = 48.dp)
                .testTag("scan_camera_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Foto Langsung", style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }

        OutlinedButton(
            onClick = onGalleryClick,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("scan_gallery_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pilih Foto", style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }

        OutlinedButton(
            onClick = onFilesClick,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("scan_file_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Berkas", style = MaterialTheme.typography.labelLarge)
        }
    }
}
