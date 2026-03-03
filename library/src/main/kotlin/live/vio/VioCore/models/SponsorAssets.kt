package live.vio.VioCore.models

import android.graphics.Color
import live.vio.VioCore.utils.VioLogger

/**
 * Helper object for providing centralized access to sponsor branding assets.
 * Automatically handles parsing colors and determining appropriate text colors
 * based on WCAG relative luminance guidelines.
 */
object SponsorAssets {
    private const val COMPONENT = "SponsorAssets"

    var name: String = ""
        private set

    var logoUrl: String? = null
        private set

    var avatarUrl: String? = null
        private set

    var badgeText: String? = null
        private set

    var primaryColor: Int? = null
        private set

    var secondaryColor: Int? = null
        private set

    /**
     * Text color to use on top of primaryColor.
     * Calculated using WCAG relative luminance (black or white).
     */
    var textOnPrimary: Int? = null
        private set

    /**
     * Updates the sponsor assets based on the provided configuration.
     */
    fun update(config: SponsorConfig?) {
        if (config == null) {
            clear()
            return
        }

        name = config.name
        logoUrl = config.logoUrl
        avatarUrl = config.avatarUrl
        badgeText = config.badgeText
        
        primaryColor = parseColorSafely(config.primaryColor)
        secondaryColor = parseColorSafely(config.secondaryColor)

        textOnPrimary = primaryColor?.let { calculateTextOnPrimary(it) }

        VioLogger.debug("Updated SponsorAssets: name=$name, primaryColor=${config.primaryColor}", COMPONENT)
    }

    /**
     * Clears the current sponsor assets.
     */
    fun clear() {
        name = ""
        logoUrl = null
        avatarUrl = null
        badgeText = null
        primaryColor = null
        secondaryColor = null
        textOnPrimary = null
    }

    private fun parseColorSafely(hexStr: String?): Int? {
        if (hexStr.isNullOrBlank()) return null
        return try {
            Color.parseColor(if (!hexStr.startsWith("#")) "#$hexStr" else hexStr)
        } catch (e: IllegalArgumentException) {
            VioLogger.warning("Invalid color format: $hexStr", COMPONENT)
            null
        }
    }

    /**
     * Calculates whether text over the given background color should be black or white
     * based on WCAG relative luminance.
     * Formula: L = 0.2126*R + 0.7152*G + 0.0722*B
     * If L > 0.179 → black; else → white
     */
    internal fun calculateTextOnPrimary(color: Int): Int {
        // Convert to linear sRGB for accurate luminance calculation
        val r = sRGBToLinear(Color.red(color) / 255.0)
        val g = sRGBToLinear(Color.green(color) / 255.0)
        val b = sRGBToLinear(Color.blue(color) / 255.0)

        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

        return if (luminance > 0.179) Color.BLACK else Color.WHITE
    }

    private fun sRGBToLinear(c: Double): Double {
        return if (c <= 0.03928) {
            c / 12.92
        } else {
            Math.pow((c + 0.055) / 1.055, 2.4)
        }
    }
}
