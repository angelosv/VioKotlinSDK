package io.reachu.VioUI.Components.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioTheme
import io.reachu.VioCore.configuration.ThemeMode
import io.reachu.VioDesignSystem.Tokens.AdaptiveColors
import io.reachu.VioDesignSystem.Tokens.AdaptiveVioColors
import io.reachu.VioUI.Components.compose.utils.toVioColor

private fun String.toColor(): Color = toVioColor()

/**
 * Compose-friendly snapshot of the adaptive Reachu color system.
 */
data class AdaptiveVioColorsCompose(
    val primary: Color,
    val secondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnPrimary: Color,
    val priceColor: Color,
    val border: Color,
    val borderSecondary: Color,
) {
    companion object {
        fun from(adaptive: AdaptiveColors): AdaptiveVioColorsCompose =
            AdaptiveVioColorsCompose(
                primary = adaptive.primary.toColor(),
                secondary = adaptive.secondary.toColor(),
                success = adaptive.success.toColor(),
                warning = adaptive.warning.toColor(),
                error = adaptive.error.toColor(),
                info = adaptive.info.toColor(),
                background = adaptive.background.toColor(),
                surface = adaptive.surface.toColor(),
                surfaceSecondary = adaptive.surfaceSecondary.toColor(),
                textPrimary = adaptive.textPrimary.toColor(),
                textSecondary = adaptive.textSecondary.toColor(),
                textTertiary = adaptive.textTertiary.toColor(),
                textOnPrimary = adaptive.textOnPrimary.toColor(),
                priceColor = adaptive.priceColor.toColor(),
                border = adaptive.border.toColor(),
                borderSecondary = adaptive.borderSecondary.toColor(),
            )
    }
}

val LocalAdaptiveVioColors = staticCompositionLocalOf {
    val theme = VioConfiguration.shared.state.value.theme
    AdaptiveVioColorsCompose.from(AdaptiveVioColors.forTheme(theme, false))
}

@Composable
fun ProvideAdaptiveVioColors(
    themeOverride: VioTheme? = null,
    content: @Composable () -> Unit,
) {
    val configState by VioConfiguration.shared.state.collectAsState()
    val theme = themeOverride ?: configState.theme

    val isDark = when (theme.mode) {
        ThemeMode.AUTOMATIC -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    io.reachu.VioDesignSystem.Tokens.VioColors.overrideColorScheme(isDark)

    val adaptive = AdaptiveVioColors.forTheme(theme, isDark)
    val composeColors = AdaptiveVioColorsCompose.from(adaptive)

    CompositionLocalProvider(LocalAdaptiveVioColors provides composeColors) {
        content()
    }
}

@Composable
fun adaptiveVioColors(): AdaptiveVioColorsCompose = LocalAdaptiveVioColors.current
