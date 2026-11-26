package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalIsDarkGlass = staticCompositionLocalOf { true }

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
    surfaceContainerHigh = Color(0xFFF6F8FC),
    outline = Color(0xFFD0D6E4),
    error = DskRed
)

private val DskShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DskYouTubeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DskDarkColorScheme else DskLightColorScheme
    CompositionLocalProvider(LocalIsDarkGlass provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DskShapes,
            content = content
        )
    }
}
