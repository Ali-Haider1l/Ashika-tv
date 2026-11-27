package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AshikaDarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = ObsidianNight,
    primaryContainer = AmethystAccent,
    onPrimaryContainer = TextPrimary,
    secondary = SoftLilac,
    onSecondary = ObsidianNight,
    secondaryContainer = DeepPlumSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = LiveCrimson,
    onTertiary = TextPrimary,
    tertiaryContainer = ElevatedPlum,
    onTertiaryContainer = FavoriteRose,
    background = ObsidianNight,
    onBackground = TextPrimary,
    surface = IndigoSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepPlumSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianNight,
    surfaceContainerLow = MidnightHeader,
    surfaceContainer = IndigoSurface,
    surfaceContainerHigh = DeepPlumSurface,
    surfaceContainerHighest = ElevatedPlum,
    outline = BorderSlate,
    outlineVariant = BorderViolet,
    error = FavoriteRose,
    onError = ObsidianNight
)

private val AshikaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AshikaLiveTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AshikaDarkColorScheme,
        typography = AshikaTypography,
        shapes = AshikaShapes,
        content = content
    )
}
