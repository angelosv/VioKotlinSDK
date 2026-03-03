package live.vio.VioDesignSystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import live.vio.VioDesignSystem.Components.VioButtonModel
import live.vio.VioDesignSystem.Components.VioButton
import live.vio.VioDesignSystem.Components.VioLoader
import live.vio.VioDesignSystem.Components.VioToastManager
import live.vio.VioDesignSystem.Components.VioToastNotification
import live.vio.VioDesignSystem.Components.VioToastOverlay
import live.vio.VioDesignSystem.Components.ToastMessage
import live.vio.VioDesignSystem.Components.ToastType
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
typealias VioButtonModel = live.vio.VioDesignSystem.Components.VioButtonModel
typealias VioToastManager = live.vio.VioDesignSystem.Components.VioToastManager
typealias VioToastMessage = live.vio.VioDesignSystem.Components.ToastMessage
typealias VioToastType = live.vio.VioDesignSystem.Components.ToastType

@Composable
fun VioToastNotification(
    message: live.vio.VioDesignSystem.Components.ToastMessage,
    modifier: Modifier = Modifier,
    onDismiss: (String) -> Unit = { live.vio.VioDesignSystem.Components.VioToastManager.dismiss(it) },
) = live.vio.VioDesignSystem.Components.VioToastNotification(
    message = message,
    modifier = modifier,
    onDismiss = onDismiss,
)

@Composable
fun VioToastOverlay(
    modifier: Modifier = Modifier,
    messages: StateFlow<List<live.vio.VioDesignSystem.Components.ToastMessage>> = live.vio.VioDesignSystem.Components.VioToastManager.messages,
    onDismiss: (String) -> Unit = { id -> live.vio.VioDesignSystem.Components.VioToastManager.dismiss(id) },
) = live.vio.VioDesignSystem.Components.VioToastOverlay(
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
@Composable
fun CampaignSponsorBadge(
    modifier: Modifier = Modifier,
) = live.vio.VioDesignSystem.Components.CampaignSponsorBadge(
    modifier = modifier,
)
