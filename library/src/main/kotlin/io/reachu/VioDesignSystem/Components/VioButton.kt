package io.reachu.VioDesignSystem.Components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken

/**
 * Compose-facing data structure matching the Swift component intent.
 */
data class VioButtonModel(
    val title: String,
    val style: Style = Style.Primary,
    val size: Size = Size.Medium,
    val isLoading: Boolean = false,
    val isDisabled: Boolean = false,
    val icon: String? = null,
    val onClick: (() -> Unit)? = null,
) {
    enum class Style { Primary, Secondary, Tertiary, Destructive, Ghost }

    enum class Size(
        val height: Float,
        val horizontalPadding: Float,
        val verticalPadding: Float,
        val textStyle: TypographyToken,
    ) {
        Small(
            height = 32f,
            horizontalPadding = VioSpacing.sm,
            verticalPadding = VioSpacing.xs,
            textStyle = VioTypography.footnote,
        ),
        Medium(
            height = 44f,
            horizontalPadding = VioSpacing.md,
            verticalPadding = VioSpacing.sm,
            textStyle = VioTypography.body,
        ),
        Large(
            height = 52f,
            horizontalPadding = VioSpacing.lg,
            verticalPadding = VioSpacing.md,
            textStyle = VioTypography.headline,
        ),
    }

    fun click() {
        if (!isDisabled && !isLoading) {
            onClick?.invoke()
        }
    }
}

/**
 * Compose implementation mirroring the Swift `VioButton` view.
 */
@Composable
fun VioButton(
    title: String,
    modifier: Modifier = Modifier,
    style: VioButtonModel.Style = VioButtonModel.Style.Primary,
    size: VioButtonModel.Size = VioButtonModel.Size.Medium,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val enabled = !isDisabled && !isLoading
    val background = backgroundColor(style)
    val contentColor = contentColor(style)
    val border = borderStroke(style)
    val shape: Shape = RoundedCornerShape(VioBorderRadius.medium.dp)

    Button(
        onClick = { if (enabled) onClick() },
        modifier = modifier.height(size.height.dp),
        enabled = enabled,
        border = border,
        shape = shape,
        contentPadding = PaddingValues(
            horizontal = size.horizontalPadding.dp,
            vertical = size.verticalPadding.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = contentColor,
            disabledContainerColor = background.copy(alpha = 0.6f),
            disabledContentColor = contentColor.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                VioLoader(
                    style = LoaderStyle.Rotate,
                    size = when (size) {
                        VioButtonModel.Size.Small -> 16.dp
                        VioButtonModel.Size.Medium -> 20.dp
                        VioButtonModel.Size.Large -> 24.dp
                    },
                    color = contentColor,
                    speed = 1.5f,
                )
            } else icon?.let {
                it()
            }

            if (!isLoading || title.isNotBlank()) {
                Text(
                    text = title,
                    style = size.textStyle.toTextStyle(),
                )
            }
        }
    }
}

@Composable
fun VioButton(
    model: VioButtonModel,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val iconContent = icon ?: model.icon?.let { iconText ->
        @Composable {
            Text(iconText)
        }
    }
    VioButton(
        title = model.title,
        modifier = modifier,
        style = model.style,
        size = model.size,
        isLoading = model.isLoading,
        isDisabled = model.isDisabled,
        icon = iconContent,
        onClick = { model.onClick?.invoke() },
    )
}

private fun TypographyToken.toTextStyle(): TextStyle {
    val weight = when (fontWeight.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    return TextStyle(fontSize = fontSize.sp, fontWeight = weight, lineHeight = lineHeight.sp)
}

private fun backgroundColor(style: VioButtonModel.Style): Color = when (style) {
    VioButtonModel.Style.Primary -> VioColors.primary.toColor()
    VioButtonModel.Style.Secondary -> VioColors.secondary.toColor()
    VioButtonModel.Style.Tertiary -> VioColors.surface.toColor()
    VioButtonModel.Style.Destructive -> VioColors.error.toColor()
    VioButtonModel.Style.Ghost -> Color.Transparent
}

private fun contentColor(style: VioButtonModel.Style): Color = when (style) {
    VioButtonModel.Style.Primary,
    VioButtonModel.Style.Secondary,
    VioButtonModel.Style.Destructive -> VioColors.textOnPrimary.toColor()
    VioButtonModel.Style.Tertiary -> VioColors.textPrimary.toColor()
    VioButtonModel.Style.Ghost -> VioColors.primary.toColor()
}

private fun borderStroke(style: VioButtonModel.Style): BorderStroke? = when (style) {
    VioButtonModel.Style.Primary,
    VioButtonModel.Style.Secondary,
    VioButtonModel.Style.Destructive -> null
    VioButtonModel.Style.Tertiary -> BorderStroke(1.dp, VioColors.border.toColor())
    VioButtonModel.Style.Ghost -> BorderStroke(1.dp, VioColors.primary.toColor())
}

private fun String.toColor(): Color {
    val normalized = trim()
    if (!normalized.startsWith("#")) return Color.Black
    val hex = normalized.removePrefix("#")
    val argb = when (hex.length) {
        6 -> (0xFF000000L or hex.toLong(16))
        8 -> hex.toLong(16)
        else -> return Color.Black
    }
    val alpha = ((argb shr 24) and 0xFF).toInt().coerceIn(0, 255)
    val red = ((argb shr 16) and 0xFF).toInt().coerceIn(0, 255)
    val green = ((argb shr 8) and 0xFF).toInt().coerceIn(0, 255)
    val blue = (argb and 0xFF).toInt().coerceIn(0, 255)
    return Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = alpha / 255f,
    )
}
