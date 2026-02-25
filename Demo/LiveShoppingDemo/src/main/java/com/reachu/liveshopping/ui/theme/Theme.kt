package com.reachu.liveshopping.ui.theme

import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LiveShoppingTheme {

    object Colors {
        val background = Color(0xFFFFFFFF)
        val surface = Color(0xFFF5F5F5)
        val surfaceLight = Color(0xFFFFFFFF)
        val primary = Color(0xFF6200EE)
        val secondary = Color(0xFF03DAC6)
        val accent = Color(0xFF018786)
        val textPrimary = Color.Black
        val textSecondary = Color.Black.copy(alpha = 0.7f)
        val textTertiary = Color.Black.copy(alpha = 0.5f)
        val live = Color(0xFFFF0000)
    }

    object Typography {
        val largeTitle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
        val title = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        val headline = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        val body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
        val caption = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        val small = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    }

    object Spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
    }

    object CornerRadius {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val extraLarge: Dp = 20.dp
    }
}
