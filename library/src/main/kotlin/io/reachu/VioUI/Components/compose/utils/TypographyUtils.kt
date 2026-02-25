package io.reachu.VioUI.Components.compose.utils

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.configuration.TypographyToken

internal fun TypographyToken.toComposeTextStyle(): TextStyle {
    val weight = when (fontWeight.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    return TextStyle(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
    )
}
