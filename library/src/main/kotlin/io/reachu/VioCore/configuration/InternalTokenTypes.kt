package io.reachu.VioCore.configuration

/**
 * Type aliases and internal definitions used as bridge types between 
 * Swift's VioCore and Kotlin's VioDesignSystem tokens.
 */
typealias ShadowStyle = Int

data class TypographyToken(
    val fontSize: Float,
    val lineHeight: Float,
    val fontWeight: String
)
