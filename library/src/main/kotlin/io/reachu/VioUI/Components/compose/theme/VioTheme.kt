package io.reachu.VioUI.Components.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.AdaptiveVioColors
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.ThemeMode
import io.reachu.VioUI.Components.compose.utils.toVioColor

private fun String.toColor(): Color = toVioColor()

private fun vioColorScheme(isDark: Boolean): ColorScheme {
    val colors = AdaptiveVioColors.current(isDark)
    
    return if (isDark) {
        darkColorScheme(
            primary = colors.primary.toColor(),
            onPrimary = colors.textOnPrimary.toColor(),
            secondary = colors.secondary.toColor(),
            background = colors.background.toColor(),
            onBackground = colors.textPrimary.toColor(),
            surface = colors.surface.toColor(),
            onSurface = colors.textPrimary.toColor(),
            surfaceVariant = colors.surfaceSecondary.toColor(),
            onSurfaceVariant = colors.textSecondary.toColor(),
            error = colors.error.toColor(),
            outline = colors.border.toColor(),
            outlineVariant = colors.borderSecondary.toColor(),
        )
    } else {
        lightColorScheme(
            primary = colors.primary.toColor(),
            onPrimary = colors.textOnPrimary.toColor(),
            secondary = colors.secondary.toColor(),
            background = colors.background.toColor(),
            onBackground = colors.textPrimary.toColor(),
            surface = colors.surface.toColor(),
            onSurface = colors.textPrimary.toColor(),
            surfaceVariant = colors.surfaceSecondary.toColor(),
            onSurfaceVariant = colors.textSecondary.toColor(),
            error = colors.error.toColor(),
            outline = colors.border.toColor(),
            outlineVariant = colors.borderSecondary.toColor(),
        )
    }
}

@Composable
fun VioTheme(content: @Composable () -> Unit) {
    val themeMode = VioConfiguration.shared.state.value.theme.mode
    val systemDark = isSystemInDarkTheme()
    
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTOMATIC -> systemDark
    }

    // Sync static color tokens
    SideEffect {
        VioColors.overrideIsDark = isDark
    }

    MaterialTheme(
        colorScheme = vioColorScheme(isDark),
        content = content,
    )
}
