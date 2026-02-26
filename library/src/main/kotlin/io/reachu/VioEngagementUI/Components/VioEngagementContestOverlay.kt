package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.VioEngagementSystem.models.Contest

/**
 * Reusable Compose component for displaying a contest in an overlay/modal.
 * 
 * @deprecated Use VioEngagementContestOverlay instead, which includes prize wheel animation
 * @param contest The contest data to display
 * @param isVisible Whether the overlay is visible
 * @param onDismiss Callback when user dismisses the overlay
 * @param onParticipate Callback when user participates in the contest
 */
@Deprecated("Use VioEngagementContestOverlay instead", ReplaceWith("VioEngagementContestOverlay(name, prize, deadline, maxParticipants, prizes, isChatExpanded, sponsorLogoUrl, modifier, onJoin, onDismiss)"))
@Composable
fun VioEngagementContestOverlay(
    name: String,
    prize: String,
    deadline: String,
    maxParticipants: Int? = null,
    prizes: List<String>? = null,
    isChatExpanded: Boolean = false,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onJoin: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = { onDismiss?.invoke() }
    ) {
        androidx.compose.material3.Text(
            text = name,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
