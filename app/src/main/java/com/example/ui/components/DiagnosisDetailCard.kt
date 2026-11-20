package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ScanRecordEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.EarthBrown
import com.example.ui.theme.HealthyLeaf
import com.example.ui.theme.HealthyLeafContainer
import com.example.ui.theme.SandContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosisDetailContent(
    record: ScanRecordEntity,
    modifier: Modifier = Modifier
) {
    val isHealthy = record.isHealthy
    val statusColor = if (isHealthy) HealthyLeaf else AlertCrimson
    val statusBg = if (isHealthy) HealthyLeafContainer else AlertCrimsonContainer

    var targetProgress by remember(record.id) { mutableFloatStateOf(0f) }
    val animatedConfidence by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 700),
        label = "confidence_anim"
    )
    LaunchedEffect(record.id, record.confidencePercent) {
        targetProgress = (record.confidencePercent.coerceIn(0, 100)) / 100f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Photo + Status Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .testTag("diagnosis_summary_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (record.imagePath.isNotBlank()) {
                    AsyncImage(
                        model = File(record.imagePath),
                        contentDescription = "Foto daun ${record.diseaseName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHealthy) "Daun Sehat" else "Kurang Sehat • Terdeteksi Penyakit",
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor
                            )
                        }
                    }

                    Text(
                        text = formatScanDate(record.createdAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = record.plantName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = record.diseaseName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tingkat Keyakinan AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${record.confidencePercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedConfidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                if (record.locationSummary.isNotBlank() || record.temperatureCelsius != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (record.locationSummary.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = EarthBrown,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = record.locationSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        if (record.temperatureCelsius != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Thermostat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${record.temperatureCelsius.toInt()}°C",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cause & AI Explanation Section
        DiagnosisSectionCard(
            title = "Penjelasan AI & Kondisi Daun",
            icon = Icons.Rounded.BugReport,
            accentColor = EarthBrown,
            badgeBg = SandContainer
        ) {
            Text(
                text = record.cause,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Treatment Steps
        val steps = record.treatmentList()
        if (steps.isNotEmpty()) {
            DiagnosisBulletCard(
                title = "Cara Mengobati & Penanggulangan",
                icon = Icons.Rounded.Healing,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeBg = MaterialTheme.colorScheme.primaryContainer,
                items = steps,
                numbered = true
            )
        }

        // Recommended Products / Suitable Medicines
        val products = record.productList()
        if (products.isNotEmpty()) {
            DiagnosisBulletCard(
                title = "Nama Obat / Pestisida yang Cocok",
                icon = Icons.Rounded.Science,
                accentColor = EarthBrown,
                badgeBg = SandContainer,
                items = products,
                numbered = false
            )
        }

        // Organic Solutions
        val organics = record.organicList()
        if (organics.isNotEmpty()) {
            DiagnosisBulletCard(
                title = "Solusi Obat Alami & Organik",
                icon = Icons.Rounded.Eco,
                accentColor = HealthyLeaf,
                badgeBg = HealthyLeafContainer,
                items = organics,
                numbered = false
            )
        }

        // Prevention Tips
        val tips = record.preventionList()
        if (tips.isNotEmpty()) {
            DiagnosisBulletCard(
                title = "Tips Pencegahan",
                icon = Icons.Rounded.Shield,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeBg = MaterialTheme.colorScheme.primaryContainer,
                items = tips,
                numbered = true
            )
        }
    }
}

@Composable
private fun DiagnosisSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    badgeBg: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DiagnosisBulletCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    badgeBg: Color,
    items: List<String>,
    numbered: Boolean
) {
    DiagnosisSectionCard(
        title = title,
        icon = icon,
        accentColor = accentColor,
        badgeBg = badgeBg
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (numbered) "${index + 1}" else "•",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun formatScanDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return formatter.format(Date(millis))
}
