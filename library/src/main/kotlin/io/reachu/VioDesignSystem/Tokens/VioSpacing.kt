package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration

/**
 * Spacing scale equivalent to the Swift design system.
 * Values are expressed in density-independent pixels (dp).
 */
object VioSpacing {
    private val scheme get() = VioConfiguration.shared.state.value.theme.spacing

    val xs: Float get() = scheme.xs
    val sm: Float get() = scheme.sm
    val md: Float get() = scheme.md
    val lg: Float get() = scheme.lg
    val xl: Float get() = scheme.xl
    val xxl: Float get() = scheme.xxl
}
