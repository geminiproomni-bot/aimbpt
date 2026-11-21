package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF003B34),
    onPrimaryContainer = NeonCyan,
    secondary = CrimsonLock,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0E29),
    onSecondaryContainer = Color(0xFFFFB1C8),
    tertiary = TelemetryAmber,
    onTertiary = Color(0xFF2A1800),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TacticalCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
