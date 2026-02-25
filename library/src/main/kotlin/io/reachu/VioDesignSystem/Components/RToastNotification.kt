package io.reachu.VioDesignSystem.Components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToastMessage(
    val id: String,
    val type: ToastType,
    val title: String,
    val description: String? = null,
    val durationMillis: Long = 3_000,
)

enum class ToastType(val backgroundColor: String, val icon: String) {
    Success(VioColors.success, "✓"),
    Error(VioColors.error, "✕"),
    Info(VioColors.info, "i"),
    Warning(VioColors.warning, "!"),
}

/**
 * Basic observable toast manager that mirrors the Swift singleton.
 */
object VioToastManager {
    private val internalFlow = MutableStateFlow<List<ToastMessage>>(emptyList())
    val messages: StateFlow<List<ToastMessage>> = internalFlow.asStateFlow()

    fun showSuccess(message: String, description: String? = null) =
        enqueue(ToastType.Success, message, description)

    fun showError(message: String, description: String? = null) =
        enqueue(ToastType.Error, message, description)

    fun showInfo(message: String, description: String? = null) =
        enqueue(ToastType.Info, message, description)

    fun showWarning(message: String, description: String? = null) =
        enqueue(ToastType.Warning, message, description)

    fun dismiss(id: String) {
        internalFlow.value = internalFlow.value.filterNot { it.id == id }
    }

    private fun enqueue(type: ToastType, title: String, description: String?) {
        val toast = ToastMessage(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            title = title,
            description = description,
        )
        internalFlow.value = internalFlow.value + toast
    }
}

data class ToastStyle(
    val type: ToastType,
    val titleStyle: TypographyToken = VioTypography.bodyBold,
    val descriptionStyle: TypographyToken = VioTypography.caption1,
)

@Composable
fun VioToastNotification(
    message: ToastMessage,
    modifier: Modifier = Modifier,
    onDismiss: (String) -> Unit,
) {
    LaunchedEffect(message.id) {
        delay(message.durationMillis)
        onDismiss(message.id)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VioSpacing.lg.dp, vertical = VioSpacing.xs.dp),
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VioBorderRadius.large.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp),
        ) {
            Text(
                text = message.type.icon,
                color = message.type.backgroundColor.toColor(),
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.title,
                    style = messageStyle(true),
                    color = VioColors.textPrimary.toColor(),
                )
                message.description?.let {
                    Text(
                        text = it,
                        style = messageStyle(false),
                        color = VioColors.textSecondary.toColor(),
                    )
                }
            }
            IconButton(onClick = { onDismiss(message.id) }) {
                Text("×", color = VioColors.textSecondary.toColor())
            }
        }
    }
}

@Composable
fun VioToastOverlay(
    modifier: Modifier = Modifier,
    messages: StateFlow<List<ToastMessage>> = VioToastManager.messages,
    onDismiss: (String) -> Unit = { VioToastManager.dismiss(it) },
) {
    val toasts by messages.collectAsState()
    Column(
        modifier = modifier.fillMaxWidth().padding(top = VioSpacing.sm.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        toasts.takeLast(3).forEach { toast ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                VioToastNotification(message = toast, onDismiss = onDismiss)
            }
        }
    }
}

private fun messageStyle(isTitle: Boolean): TextStyle {
    val token = if (isTitle) VioTypography.bodyBold else VioTypography.caption1
    val weight = when (token.fontWeight.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    return TextStyle(fontSize = token.fontSize.sp, fontWeight = weight, lineHeight = token.lineHeight.sp)
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
