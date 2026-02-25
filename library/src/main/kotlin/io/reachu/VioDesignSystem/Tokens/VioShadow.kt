package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration

/**
 * Shadow definitions mirroring Swift's `VioShadow`.
 */
object VioShadow {

    /**
     * Equivalent to shadow style numbers in Swift/UIKit.
     */
    fun shadow(style: Int): String {
        // Mock implementation of shadow style resolution
        return when(style) {
            1 -> "#00000033" // Low elevation
            2 -> "#0000004D" // High elevation
            else -> "#00000026" // Default
        }
    }

    /**
     * Resolves shadow color based on current theme if needed.
     */
    fun color(): String {
        val theme = VioConfiguration.shared.state.value.theme
        // In Swift, shadow might change based on light/dark.
        return "#111827" 
    }

    val default: String get() = "#1118271A"
}
