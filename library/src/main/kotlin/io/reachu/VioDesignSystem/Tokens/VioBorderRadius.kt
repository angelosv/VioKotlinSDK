package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration

/**
 * Corner radius tokens mirroring Swift's `VioBorderRadius`.
 * Values adapt to the current theme configuration.
 */
object VioBorderRadius {

    private val scheme get() = VioConfiguration.shared.state.value.theme.borderRadius

    val none: Float = 0f
    val xs: Float get() = scheme.xs
    val small: Float get() = scheme.small
    val medium: Float get() = scheme.medium
    val large: Float get() = scheme.large
    val extraLarge: Float get() = 16f 
    val xl: Float get() = 24f
    val circle: Float get() = scheme.circle
    val pill: Float get() = scheme.circle
}
