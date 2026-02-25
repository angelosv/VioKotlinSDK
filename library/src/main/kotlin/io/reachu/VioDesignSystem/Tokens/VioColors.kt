package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.ThemeMode
import io.reachu.VioCore.configuration.ThemeColors

/**
 * Static color tokens for the Vio Design System.
 * Values are resolved dynamically based on the current theme configured in [io.reachu.VioCore.configuration.VioConfiguration].
 * Use these tokens to ensure visual consistency across custom UI elements.
 */
object VioColors {

    // Default fallbacks for when the theme hasn't specified a color.
    private const val DEFAULT_PRIMARY = "#007AFF"
    private const val DEFAULT_SECONDARY = "#5856D6"
    private const val DEFAULT_SURFACE = "#FFFFFF"
    private const val DEFAULT_SURFACE_SECONDARY = "#F9F9F9"
    private const val DEFAULT_BORDER = "#E5E5EA"
    private const val DEFAULT_BORDER_SECONDARY = "#D1D1D6"
    private const val DEFAULT_TEXT_PRIMARY = "#000000"
    private const val DEFAULT_TEXT_SECONDARY = "#8E8E93"

    // Allow manual overrides for testing or specific screen requirements.
    var overrideIsDark: Boolean? = null

    fun overrideColorScheme(isDark: Boolean) {
        overrideIsDark = isDark
    }

    private val theme get() = VioConfiguration.shared.state.value.theme

    private fun palette(): ThemeColors {
        val isDark = overrideIsDark ?: (theme.mode == ThemeMode.DARK)
        return if (isDark) theme.darkColors else theme.lightColors
    }

    private fun pick(value: String?, fallback: String): String = value ?: fallback

    val primary: String get() = pick(palette().primary, DEFAULT_PRIMARY)
    val secondary: String get() = pick(palette().secondary, DEFAULT_SECONDARY)
    val success: String get() = pick(palette().success, "#34C759")
    val warning: String get() = pick(palette().warning, "#FF9500")
    val error: String get() = pick(palette().error, "#FF3B30")
    val info: String get() = pick(palette().info, "#007AFF")

    val background: String get() = pick(palette().background, "#F2F2F7")
    val surface: String get() = pick(palette().surface, DEFAULT_SURFACE)
    val surfaceSecondary: String get() = pick(palette().surfaceSecondary, DEFAULT_SURFACE_SECONDARY)
    val backgroundMuted: String get() = pick(palette().surfaceSecondary, "#F9F9FB")

    val border: String get() = pick(palette().border, DEFAULT_BORDER)
    val borderSecondary: String get() = pick(palette().borderSecondary, DEFAULT_BORDER_SECONDARY)

    val shadow: String get() = "#1118271A"

    val textPrimary: String get() = pick(palette().textPrimary, DEFAULT_TEXT_PRIMARY)
    val textSecondary: String get() = pick(palette().textSecondary, DEFAULT_TEXT_SECONDARY)
    val textTertiary: String get() = pick(palette().textTertiary, "#C7C7CC")
    val textOnPrimary: String get() = pick(palette().textOnPrimary, "#FFFFFF")

    val priceColor: String get() = pick(palette().priceColor, primary)
}
