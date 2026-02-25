package io.reachu.VioDesignSystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Components.VioButtonModel
import io.reachu.VioDesignSystem.Components.VioButton
import io.reachu.VioDesignSystem.Components.VioLoader
import io.reachu.VioDesignSystem.Components.VioToastManager
import io.reachu.VioDesignSystem.Components.VioToastNotification
import io.reachu.VioDesignSystem.Components.VioToastOverlay
import io.reachu.VioDesignSystem.Components.ToastMessage
import io.reachu.VioDesignSystem.Components.ToastType
import kotlinx.coroutines.flow.StateFlow

/**
 * Kotlin entry point mirroring the Swift `VioDesignSystem`.
 */
object VioDesignSystem {
    fun configure() {
        println("🎨 Vio Design System initialized")
    }
}

// Public aliases to match the Swift API surface
typealias VioButtonModel = io.reachu.VioDesignSystem.Components.VioButtonModel
typealias VioToastManager = io.reachu.VioDesignSystem.Components.VioToastManager
typealias VioToastMessage = io.reachu.VioDesignSystem.Components.ToastMessage
typealias VioToastType = io.reachu.VioDesignSystem.Components.ToastType

@Composable
fun VioToastNotification(
    message: io.reachu.VioDesignSystem.Components.ToastMessage,
    modifier: Modifier = Modifier,
    onDismiss: (String) -> Unit = { io.reachu.VioDesignSystem.Components.VioToastManager.dismiss(it) },
) = io.reachu.VioDesignSystem.Components.VioToastNotification(
    message = message,
    modifier = modifier,
    onDismiss = onDismiss,
)

@Composable
fun VioToastOverlay(
    modifier: Modifier = Modifier,
    messages: StateFlow<List<io.reachu.VioDesignSystem.Components.ToastMessage>> = io.reachu.VioDesignSystem.Components.VioToastManager.messages,
    onDismiss: (String) -> Unit = { id -> io.reachu.VioDesignSystem.Components.VioToastManager.dismiss(id) },
) = io.reachu.VioDesignSystem.Components.VioToastOverlay(
    modifier = modifier,
    messages = messages,
    onDismiss = onDismiss,
)

@Composable
fun VioCustomLoader(
    size: Dp = 48.dp,
    color: Color = Color.Gray.copy(alpha = 0.4f),
) = VioLoader(
    size = size,
    color = color,
)
