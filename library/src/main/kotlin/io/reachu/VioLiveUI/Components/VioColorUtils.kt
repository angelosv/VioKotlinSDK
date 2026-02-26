package io.reachu.liveui.components

import androidx.compose.ui.graphics.Color

internal fun String.toVioColor(): Color {
    val normalized = trim().removePrefix("#")
    val value = normalized.toLongOrNull(16) ?: return Color.Unspecified
    val argb = when (normalized.length) {
        6 -> 0xFF000000L or value
        8 -> value
        else -> value
    }
    return Color(argb.toULong())
}
