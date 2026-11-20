package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    secondary = EarthBrown,
    onSecondary = Color.White,
    secondaryContainer = SandContainer,
    onSecondaryContainer = OnSandContainer,
    tertiary = PastelBlueIcon,
    onTertiary = Color.White,
    tertiaryContainer = PastelBlueCard,
    onTertiaryContainer = Color(0xFF123A63),
    background = WarmCream,
    onBackground = Color(0xFF1B241C),
    surface = Color.White,
    onSurface = Color(0xFF1B241C),
    surfaceVariant = SoftSoilSurface,
    onSurfaceVariant = Color(0xFF556054),
    outline = SoilOutline,
    outlineVariant = Color(0xFFE5E9E0),
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = Color(0xFF6B1414)
)

private val DarkColorScheme = darkColorScheme(
    primary = LightLeafPrimary,
    onPrimary = ForestGreenDark,
    primaryContainer = ForestGreen,
    onPrimaryContainer = MintMist,
    secondary = LightEarthSecondary,
    onSecondary = WarmBark,
    secondaryContainer = Color(0xFF4D3422),
    onSecondaryContainer = SandContainer,
    tertiary = Color(0xFF81C2FF),
    onTertiary = Color(0xFF0B3054),
    tertiaryContainer = Color(0xFF184878),
    onTertiaryContainer = PastelBlueCard,
    background = DarkForestBackground,
    onBackground = Color(0xFFEBF2EC),
    surface = DarkSoilSurface,
    onSurface = Color(0xFFEBF2EC),
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = Color(0xFFC2CCC3),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2D3A30),
    error = Color(0xFFFF8E8E),
    onError = Color(0xFF5B0B0B),
    errorContainer = Color(0xFF7B1C1C),
    onErrorContainer = AlertCrimsonContainer
)

val AgriShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AgriSignalTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgriSignalTypography,
        shapes = AgriShapes,
        content = content
    )
}
