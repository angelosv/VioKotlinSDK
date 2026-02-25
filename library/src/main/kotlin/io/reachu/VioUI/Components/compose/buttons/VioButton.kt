package io.reachu.VioUI.Components.compose.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Components.VioButtonModel
import io.reachu.VioDesignSystem.Components.VioButton
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.compose.utils.toComposeTextStyle

private fun String.toColor(): Color = toVioColor()

/**
 * Abstraction that resolves icon names (mirroring the SwiftUI implementation).
 * The demo can provide a painter backed by Android resources without coupling
 * the library to the Android framework.
 */
fun interface VioIconPainterProvider {
    @Composable
    fun resolve(name: String): Painter?
}

@Composable
fun VioButton(
    model: VioButtonModel,
    modifier: Modifier = Modifier,
    iconPainterProvider: VioIconPainterProvider? = null,
    shape: Shape = RoundedCornerShape(VioBorderRadius.medium.dp),
) {
    val container = when (model.style) {
        VioButtonModel.Style.Primary -> VioColors.primary.toColor()
        VioButtonModel.Style.Secondary -> VioColors.secondary.toColor()
        VioButtonModel.Style.Tertiary -> VioColors.surface.toColor()
        VioButtonModel.Style.Destructive -> VioColors.error.toColor()
        VioButtonModel.Style.Ghost -> Color.Transparent
    }

    val content = when (model.style) {
        VioButtonModel.Style.Primary,
        VioButtonModel.Style.Secondary,
        VioButtonModel.Style.Destructive -> Color.White
        VioButtonModel.Style.Tertiary -> VioColors.textPrimary.toColor()
        VioButtonModel.Style.Ghost -> VioColors.primary.toColor()
    }

    val (borderColor, borderWidth) = when (model.style) {
        VioButtonModel.Style.Primary,
        VioButtonModel.Style.Secondary,
        VioButtonModel.Style.Destructive -> Color.Transparent to 0.dp
        VioButtonModel.Style.Tertiary -> VioColors.border.toColor() to 1.dp
        VioButtonModel.Style.Ghost -> VioColors.primary.toColor() to 1.dp
    }
    val border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null

    val padding = when (model.size) {
        VioButtonModel.Size.Small -> PaddingValues(
            horizontal = model.size.horizontalPadding.dp,
            vertical = VioSpacing.xs.dp,
        )
        VioButtonModel.Size.Medium -> PaddingValues(
            horizontal = model.size.horizontalPadding.dp,
            vertical = VioSpacing.sm.dp,
        )
        VioButtonModel.Size.Large -> PaddingValues(
            horizontal = model.size.horizontalPadding.dp,
            vertical = VioSpacing.md.dp,
        )
    }

    Button(
        onClick = { model.click() },
        enabled = !model.isDisabled && !model.isLoading,
        modifier = modifier.height(model.size.height.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.6f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
        contentPadding = padding,
        border = border,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (model.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = content,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                val resolvedIcon = model.icon?.let { iconName ->
                    iconPainterProvider?.resolve(iconName)
                }
                if (resolvedIcon != null) {
                    Icon(
                        painter = resolvedIcon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(VioSpacing.xs.dp))
                }
                Text(
                    text = model.title,
                    style = model.size.textStyle.toComposeTextStyle(),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun VioButtonModel.Size.textStyle(): TypographyToken = when (this) {
    VioButtonModel.Size.Small -> VioTypography.caption1
    VioButtonModel.Size.Medium -> VioTypography.callout
    VioButtonModel.Size.Large -> VioTypography.body
}
