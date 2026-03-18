package live.vio.VioDesignSystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Data class representing the visual identity assets of a sponsor.
 * Mirrors the Swift `SponsorAssets.swift` logic.
 */
data class SponsorAssets(
    val sponsorId: Int,
    val name: String,
    val logoUrl: String?,
    val avatarUrl: String?,
    val primaryColor: String,
    val secondaryColor: String?,
    val badgeText: String?,
    val textOnPrimary: Color = calculateTextOnPrimary(primaryColor)
) {
    companion object {
        /**
         * Calculates whether text over the given background color should be black or white
         * based on WCAG relative luminance.
         * Formula: L = 0.2126*R + 0.7152*G + 0.0722*B
         * If L > 0.179 → black; else → white
         */
        fun calculateTextOnPrimary(hexColor: String): Color {
            return try {
                val colorInt = android.graphics.Color.parseColor(
                    if (!hexColor.startsWith("#")) "#$hexColor" else hexColor
                )
                
                val r = sRGBToLinear(android.graphics.Color.red(colorInt) / 255.0)
                val g = sRGBToLinear(android.graphics.Color.green(colorInt) / 255.0)
                val b = sRGBToLinear(android.graphics.Color.blue(colorInt) / 255.0)

                val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

                if (luminance > 0.179) Color.Black else Color.White
            } catch (e: Exception) {
                Color.White // Default fallback
            }
        }

        private fun sRGBToLinear(c: Double): Double {
            return if (c <= 0.03928) {
                c / 12.92
            } else {
                Math.pow((c + 0.055) / 1.055, 2.4)
            }
        }
    }
}
