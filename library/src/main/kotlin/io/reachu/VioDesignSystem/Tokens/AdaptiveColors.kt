package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioTheme

/**
 * Kotlin analogue of Swift's AdaptiveVioColors.
 * Provides theme-aware colors that adapt to light/dark modes.
 */
data class AdaptiveColors(
    val primary: String,
    val secondary: String,
    val success: String,
    val warning: String,
    val error: String,
    val info: String,
    val background: String,
    val surface: String,
    val surfaceSecondary: String,
    val textPrimary: String,
    val textSecondary: String,
    val textTertiary: String,
    val textOnPrimary: String,
    val priceColor: String,
    val border: String,
    val borderSecondary: String,
)

object AdaptiveVioColors {
    /**
     * Resolve adaptive colors for the current configured theme.
     * Caller decides if dark mode is active (e.g., via isSystemInDarkTheme()).
     */
    fun current(isDark: Boolean): AdaptiveColors = forTheme(VioConfiguration.shared.state.value.theme, isDark)

    /**
     * Resolve adaptive colors for a specific theme and mode.
     */
    fun forTheme(theme: VioTheme, isDark: Boolean): AdaptiveColors {
        val palette = if (isDark) theme.darkColors else theme.lightColors

        fun pick(candidate: String?, fallback: String) = candidate ?: fallback

        return AdaptiveColors(
            primary = pick(palette.primary, VioColors.primary),
            secondary = pick(palette.secondary, VioColors.secondary),
            success = pick(palette.success, VioColors.success),
            warning = pick(palette.warning, VioColors.warning),
            error = pick(palette.error, VioColors.error),
            info = pick(palette.info, VioColors.info),
            background = pick(palette.background, VioColors.background),
            surface = pick(palette.surface, VioColors.surface),
            surfaceSecondary = pick(palette.surfaceSecondary, VioColors.surfaceSecondary),
            textPrimary = pick(palette.textPrimary, VioColors.textPrimary),
            textSecondary = pick(palette.textSecondary, VioColors.textSecondary),
            textTertiary = pick(palette.textTertiary, VioColors.textTertiary),
            textOnPrimary = pick(palette.textOnPrimary, VioColors.textOnPrimary),
            priceColor = pick(palette.priceColor ?: palette.primary, VioColors.priceColor),
            border = pick(palette.border, VioColors.border),
            borderSecondary = pick(palette.borderSecondary, VioColors.borderSecondary),
        )
    }
}
