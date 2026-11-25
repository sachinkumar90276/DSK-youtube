package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DskDarkColorScheme = darkColorScheme(
    primary = DskRed,
    onPrimary = DskWhite,
    primaryContainer = DskRedDark,
    onPrimaryContainer = DskWhite,
    secondary = DskRedGlow,
    onSecondary = DskWhite,
    secondaryContainer = DskGlassSurface,
    onSecondaryContainer = DskWhite,
    tertiary = DskInfo,
    onTertiary = DskBlack,
    background = DskBlack,
    onBackground = DskWhite,
    surface = DskCharcoal,
    onSurface = DskWhite,
    surfaceVariant = DskSurfaceElevated,
    onSurfaceVariant = DskTextSecondary,
    surfaceContainerHigh = DskGlassSurface,
    outline = DskOutline,
    error = DskRedGlow,
    onError = DskWhite
)

private val DskLightColorScheme = lightColorScheme(
    primary = DskRed,
    onPrimary = DskWhite,
    primaryContainer = Color(0xFFFFDADF),
    onPrimaryContainer = DskRedDark,
    secondary = DskRedDark,
    onSecondary = DskWhite,
    background = DskLightBackground,
    onBackground = DskLightOnBackground,
    surface = DskLightSurface,
    onSurface = DskLightOnBackground,
    surfaceVariant = DskLightSurfaceVariant,
    onSurfaceVariant = DskLightOnSurfaceVariant,
    outline = Color(0xFFD5D5E0),
    error = DskRed
)

private val DskShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DskYouTubeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DskDarkColorScheme else DskLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DskShapes,
        content = content
    )
}
