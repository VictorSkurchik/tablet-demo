package by.vsdev.tablet.demo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalHapticFeedback
import by.vsdev.tablet.demo.ui.haptics.ComposeAppHaptics
import by.vsdev.tablet.demo.ui.haptics.LocalAppHaptics

internal val LocalCellColors =
    staticCompositionLocalOf<CellColors> {
        error("LocalCellColors not provided — wrap content in AppTheme")
    }

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val cellColors = if (useDarkTheme) DarkCellColors else LightCellColors
    val hapticFeedback = LocalHapticFeedback.current
    val appHaptics = remember(hapticFeedback) { ComposeAppHaptics(hapticFeedback) }

    CompositionLocalProvider(
        LocalCellColors provides cellColors,
        LocalAppHaptics provides appHaptics,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
