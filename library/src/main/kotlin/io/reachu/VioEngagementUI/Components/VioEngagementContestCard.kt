package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.VioEngagementSystem.models.Contest

/**
 * Reusable Compose component for displaying a contest card.
 * 
 * @deprecated Use VioEngagementContestCard instead, which uses VioEngagementCardBase
 * @param contest The contest data to display
 * @param onParticipate Callback when user participates in the contest
 */
@Deprecated("Use VioEngagementContestCard instead", ReplaceWith("VioEngagementContestCard(contest, sponsorLogoUrl, modifier, onJoin, onDismiss)"))
@Composable
fun VioEngagementContestCardInternal(
    contest: Contest,
    onParticipate: (() -> Unit)? = null
) {
    // Deprecated: Use VioEngagementContestCard instead
    // This stub is kept for backward compatibility
}
