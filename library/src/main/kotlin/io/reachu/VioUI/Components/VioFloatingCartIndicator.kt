package io.reachu.VioUI.Components

/** Floating cart indicator configuration. */
data class VioFloatingCartIndicator(
    val position: Position = Position.bottomRight,
    val displayMode: DisplayMode = DisplayMode.full,
    val size: Size = Size.medium,
    val customPadding: Padding? = null,
) {

    enum class Position {
        bottomRight, bottomLeft, bottomCenter,
        topRight, topLeft, topCenter,
        centerRight, centerLeft;

        val isTop: Boolean get() = this in setOf(topLeft, topCenter, topRight)
        val isCenter: Boolean get() = this in setOf(centerLeft, centerRight, topCenter, bottomCenter)
    }

    enum class DisplayMode {
        full,
        compact,
        minimal,
        iconOnly,
    }

    enum class Size {
        small,
        medium,
        large;
    }

    data class Padding(
        val top: Float = 0f,
        val bottom: Float = 0f,
        val start: Float = 0f,
        val end: Float = 0f,
    )

}
