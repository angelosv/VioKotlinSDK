package io.reachu.VioUI.Components.compose.feedback

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.reachu.VioDesignSystem.Components.VioToastManager
import io.reachu.VioDesignSystem.Components.ToastMessage
import io.reachu.VioDesignSystem.Components.ToastType
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.compose.utils.toComposeTextStyle

private fun String.toColor(): Color = toVioColor()

fun interface ToastIconProvider {
    @Composable
    fun resolve(type: ToastType): Painter?
}

@Composable
fun VioToastNotification(
    message: String,
    type: ToastType = ToastType.Success,
    durationMillis: Long = 3_000,
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    iconProvider: ToastIconProvider? = null,
) {
    val containerColor = Color.White.copy(alpha = 0.92f)

    val tint = when (type) {
        ToastType.Success -> VioColors.success.toColor()
        ToastType.Error -> VioColors.error.toColor()
        ToastType.Info -> VioColors.info.toColor()
        ToastType.Warning -> VioColors.warning.toColor()
    }

    val offsetY by animateDpAsState(targetValue = if (isPresented) 0.dp else (-100).dp, animationSpec = spring())
    val alpha by animateFloatAsState(targetValue = if (isPresented) 1f else 0f)

    LaunchedEffect(isPresented, message) {
        if (isPresented) {
            delay(durationMillis)
            onDismissRequest()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .alpha(alpha),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(VioBorderRadius.large.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp),
        ) {
            val icon = iconProvider?.resolve(type)
            if (icon != null) {
                Icon(painter = icon, contentDescription = null, tint = tint)
            }

            Text(
                text = message,
                style = VioTypography.body.toComposeTextStyle(),
                color = VioColors.textPrimary.toColor()
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "✕",
                color = VioColors.textSecondary.toColor(),
                fontSize = 18.sp,
                modifier = Modifier.clickable { onDismissRequest() }
            )
        }
    }
}

@Composable
fun VioToastOverlay(
    messages: Flow<List<ToastMessage>> = VioToastManager.messages,
    iconProvider: ToastIconProvider? = null,
    onDismiss: (String) -> Unit = { VioToastManager.dismiss(it) },
) {
    val currentMessages by messages.collectAsState(initial = emptyList())
    val current = currentMessages.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().zIndex(1000f)) {
        if (current != null) {
            VioToastNotification(
                message = current.title,
                type = current.type,
                durationMillis = current.durationMillis,
                isPresented = true,
                onDismissRequest = { onDismiss(current.id) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = VioSpacing.lg.dp, vertical = VioSpacing.sm.dp),
                iconProvider = iconProvider,
            )
        }
    }
}
