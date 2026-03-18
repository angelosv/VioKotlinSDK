package live.vio.VioCore.models

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import live.vio.VioCore.utils.VioLogger
import live.vio.VioDesignSystem.SponsorAssets as DesignAssets

/**
 * Helper object for providing centralized access to sponsor branding assets.
 * Automatically handles parsing colors and determining appropriate text colors
 * based on WCAG relative luminance guidelines.
 */
object SponsorAssets {
    private const val COMPONENT = "SponsorAssets"

    var current: DesignAssets? = null
        private set

    // Legacy properties for backward compatibility
    val name: String get() = current?.name ?: ""
    val logoUrl: String? get() = current?.logoUrl
    val avatarUrl: String? get() = current?.avatarUrl
    val badgeText: String? get() = current?.badgeText
    val primaryColor: Int? get() = current?.primaryColor?.let { parseColorToInt(it) }
    val textOnPrimary: Int? get() = current?.textOnPrimary?.toArgb()

    /**
     * Updates the sponsor assets based on the provided configuration.
     */
    fun update(config: SponsorConfig?) {
        if (config == null) {
            clear()
            return
        }

        current = DesignAssets(
            sponsorId = 0, // Not available in config currently
            name = config.name,
            logoUrl = config.logoUrl,
            avatarUrl = config.avatarUrl,
            primaryColor = config.primaryColor ?: "#FFFFFF",
            secondaryColor = config.secondaryColor,
            badgeText = config.badgeText
        )

        VioLogger.debug("Updated SponsorAssets: name=$name, primaryColor=${config.primaryColor}", COMPONENT)
    }

    /**
     * Clears the current sponsor assets.
     */
    fun clear() {
        current = null
    }

    private fun parseColorToInt(hexStr: String): Int? {
        return try {
            Color.parseColor(if (!hexStr.startsWith("#")) "#$hexStr" else hexStr)
        } catch (e: Exception) {
            null
        }
    }
}
