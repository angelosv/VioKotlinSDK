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
fun ReachuEngagementContestOverlay(
    contest: Contest,
    isVisible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onParticipate: (() -> Unit)? = null
) {
    // Deprecated: Use VioEngagementContestOverlay instead
    // This stub is kept for backward compatibility
}
