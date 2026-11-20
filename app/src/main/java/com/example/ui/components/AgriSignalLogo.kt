package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EarthBrown
import com.example.ui.theme.EmeraldLeaf
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDark

/**
 * Custom AGRI SIGNAL emblem matching the reference illustration:
 * Rounded white square with dark green border, brown soil mounds at bottom,
 * two sprouting green leaves in the center, and three green signal waves above.
 */
@Composable
fun AgriSignalLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    badgeBackground: Boolean = true
) {
    val cornerRadius = size * 0.24f
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Logo Agri Signal" }
            .then(
                if (badgeBackground) {
                    Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(Color.White)
                        .border(
                            width = (size * 0.035f).coerceAtLeast(1.5.dp),
                            color = ForestGreenDark,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.84f)) {
            val w = this.size.width
            val h = this.size.height

            // Top three signal arcs radiating upward from center
            val arcCenter = Offset(w * 0.50f, h * 0.42f)
            val strokeW = w * 0.058f
            listOf(0.14f, 0.25f, 0.36f).forEach { rFactor ->
                val radius = w * rFactor
                drawArc(
                    color = ForestGreen,
                    startAngle = -132f,
                    sweepAngle = 84f,
                    useCenter = false,
                    topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Left sprouting green leaf
            val leftLeaf = Path().apply {
                moveTo(w * 0.49f, h * 0.72f)
                cubicTo(w * 0.42f, h * 0.46f, w * 0.20f, h * 0.42f, w * 0.14f, h * 0.50f)
                cubicTo(w * 0.19f, h * 0.66f, w * 0.35f, h * 0.74f, w * 0.49f, h * 0.72f)
                close()
            }
            drawPath(path = leftLeaf, color = ForestGreen)

            // Right sprouting green leaf
            val rightLeaf = Path().apply {
                moveTo(w * 0.51f, h * 0.72f)
                cubicTo(w * 0.58f, h * 0.46f, w * 0.80f, h * 0.42f, w * 0.86f, h * 0.50f)
                cubicTo(w * 0.81f, h * 0.66f, w * 0.65f, h * 0.74f, w * 0.51f, h * 0.72f)
                close()
            }
            drawPath(path = rightLeaf, color = EmeraldLeaf)

            // White veins on leaves
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(w * 0.48f, h * 0.70f),
                end = Offset(w * 0.23f, h * 0.52f),
                strokeWidth = w * 0.024f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(w * 0.52f, h * 0.70f),
                end = Offset(w * 0.77f, h * 0.52f),
                strokeWidth = w * 0.024f,
                cap = StrokeCap.Round
            )

            // Curved brown soil hills at the base
            val backSoil = Path().apply {
                moveTo(w * 0.10f, h * 0.86f)
                quadraticTo(w * 0.45f, h * 0.64f, w * 0.90f, h * 0.82f)
                lineTo(w * 0.90f, h * 0.94f)
                lineTo(w * 0.10f, h * 0.94f)
                close()
            }
            drawPath(path = backSoil, color = EarthBrown)

            // Subtle cream furrow lines on soil
            drawArc(
                color = Color(0xFFF5EFE6),
                startAngle = 205f,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.74f),
                size = Size(w * 0.76f, h * 0.34f),
                style = Stroke(width = w * 0.026f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun AgriSignalWordmark(
    fontSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ForestGreen, fontWeight = FontWeight.Bold)) {
                append("Agri ")
            }
            withStyle(SpanStyle(color = EarthBrown, fontWeight = FontWeight.Bold)) {
                append("Signal")
            }
        },
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize),
        modifier = modifier
    )
}

@Composable
fun AgriSignalBrandRow(
    logoSize: Dp = 36.dp,
    fontSize: TextUnit = 19.sp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AgriSignalLogoMark(size = logoSize)
        AgriSignalWordmark(fontSize = fontSize)
    }
}
