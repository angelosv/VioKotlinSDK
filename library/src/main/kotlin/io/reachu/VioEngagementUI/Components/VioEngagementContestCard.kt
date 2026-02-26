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
fun VioEngagementContestCard(
    contest: Contest,
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
            text = contest.title,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
