package com.openblockit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.openblockit.core.Prefs

data class BlockitPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceBright: Color,
    val surfaceSheet: Color,
    val blockTile: Color,
    val barSurface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val tileMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val outline: Color,
    val timerTile: Color,
    val dark: Boolean,
)

val DarkPalette = BlockitPalette(
    background = Color(0xFF201D1A),
    surface = Color(0xFF201D1A),
    surfaceVariant = Color(0xFF272320),
    surfaceBright = Color(0xFF332A27),
    surfaceSheet = Color(0xFF2E2A26),
    blockTile = Color(0xFF1E1C1A),
    barSurface = Color(0xFF2E2A26),
    onSurface = Color(0xFFFBECE1),
    onSurfaceVariant = Color(0xFFFAE4D3),
    tileMuted = Color(0xFF6E6862),
    primary = Color(0xFFFFE4CF),
    onPrimary = Color(0xFF1B110D),
    primaryContainer = Color(0xFFF47442),
    onPrimaryContainer = Color(0xFF471602),
    outline = Color(0xFF807A7A),
    timerTile = Color(0xFFF5E8DA),
    dark = true,
)

val LightPalette = BlockitPalette(
    background = Color(0xFFE0E0E0),
    surface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFFDBDBDB),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceSheet = Color(0xFFE0E0E0),
    blockTile = Color(0xFF1E1C1A),
    barSurface = Color(0xFF2E2A26),
    onSurface = Color(0xFF2C2A29),
    onSurfaceVariant = Color(0xFF787170),
    tileMuted = Color(0xFF787170),
    primary = Color(0xFF252323),
    onPrimary = Color(0xFFFBF1EC),
    primaryContainer = Color(0xFFF55314),
    onPrimaryContainer = Color(0xFF471602),
    outline = Color(0xFF615B5A),
    timerTile = Color(0xFF1E1C1A),
    dark = false,
)

val LocalPal = staticCompositionLocalOf { DarkPalette }

@Composable
fun OpenBlockitTheme(content: @Composable () -> Unit) {
    val dark = when (Prefs.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val pal = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = pal.primary,
            onPrimary = pal.onPrimary,
            primaryContainer = pal.primaryContainer,
            onPrimaryContainer = pal.onPrimaryContainer,
            background = pal.background,
            onBackground = pal.onSurface,
            surface = pal.surface,
            onSurface = pal.onSurface,
            surfaceVariant = pal.surfaceVariant,
            onSurfaceVariant = pal.onSurfaceVariant,
            outline = pal.outline,
        )
    } else {
        lightColorScheme(
            primary = pal.primary,
            onPrimary = pal.onPrimary,
            primaryContainer = pal.primaryContainer,
            onPrimaryContainer = pal.onPrimaryContainer,
            background = pal.background,
            onBackground = pal.onSurface,
            surface = pal.surface,
            onSurface = pal.onSurface,
            surfaceVariant = pal.surfaceVariant,
            onSurfaceVariant = pal.onSurfaceVariant,
            outline = pal.outline,
        )
    }
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
