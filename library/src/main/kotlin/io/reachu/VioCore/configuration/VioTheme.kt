package io.reachu.VioCore.configuration

data class VioTheme(
    val name: String,
    val mode: ThemeMode,
    val lightColors: ThemeColors,
    val darkColors: ThemeColors,
    val typography: TypographyScheme = TypographyScheme.default(),
    val spacing: SpacingScheme = SpacingScheme.default(),
    val borderRadius: BorderRadiusScheme = BorderRadiusScheme.default(),
) {
    companion object {
        fun defaultTheme() = VioTheme(
            name = "Default SDK Theme",
            mode = ThemeMode.AUTOMATIC,
            lightColors = ThemeColors.vio(),
            darkColors = ThemeColors.vioDark(),
        )
    }
}

enum class ThemeMode { LIGHT, DARK, AUTOMATIC }

data class ThemeColors(
    val primary: String? = null,
    val secondary: String? = null,
    val success: String? = null,
    val warning: String? = null,
    val error: String? = null,
    val info: String? = null,
    val background: String? = null,
    val surface: String? = null,
    val surfaceSecondary: String? = null,
    val textPrimary: String? = null,
    val textSecondary: String? = null,
    val textTertiary: String? = null,
    val textOnPrimary: String? = null,
    val border: String? = null,
    val borderSecondary: String? = null,
    val priceColor: String? = null,
) {
    companion object {
        fun vio() = ThemeColors(
            primary = "#007AFF",
            secondary = "#5856D6",
            success = "#34C759",
            warning = "#FF9500",
            error = "#FF3B30",
            info = "#007AFF",
            background = "#F2F2F7",
            surface = "#FFFFFF",
            surfaceSecondary = "#F9F9F9",
            textPrimary = "#000000",
            textSecondary = "#8E8E93",
            textTertiary = "#C7C7CC",
            textOnPrimary = "#FFFFFF",
            border = "#E5E5EA",
            borderSecondary = "#D1D1D6",
            priceColor = "#007AFF"
        )

        fun vioDark() = ThemeColors(
            primary = "#0A84FF",
            secondary = "#5E5CE6",
            success = "#32D74B",
            warning = "#FF9F0A",
            error = "#FF453A",
            info = "#0A84FF",
            background = "#000000",
            surface = "#1C1C1E",
            surfaceSecondary = "#2C2C2E",
            textPrimary = "#FFFFFF",
            textSecondary = "#8E8E93",
            textTertiary = "#48484A",
            textOnPrimary = "#FFFFFF",
            border = "#38383A",
            borderSecondary = "#48484A",
            priceColor = "#0A84FF"
        )
    }
}

// Typography/Spacing/Radius schemes (analogies to Swift types)

data class TypographyScheme(
    val headline: Float = 18f,
    val body: Float = 16f,
    val callout: Float = 14f,
    val caption: Float = 12f,
) {
    companion object { fun default() = TypographyScheme() }
}

data class SpacingScheme(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f,
    val xxl: Float = 48f,
) {
    companion object { fun default() = SpacingScheme() }
}

data class BorderRadiusScheme(
    val none: Float = 0f,
    val xs: Float = 2f,
    val small: Float = 4f,
    val medium: Float = 8f,
    val large: Float = 12f,
    val xl: Float = 16f,
    val circle: Float = 999f,
) {
    val pill: Float get() = circle

    companion object { fun default() = BorderRadiusScheme() }
}
