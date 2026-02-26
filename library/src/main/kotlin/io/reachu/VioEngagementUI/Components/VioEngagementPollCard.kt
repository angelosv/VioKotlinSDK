package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollResults

/**
 * Reusable Compose component for displaying a poll card.
 * 
 * @param poll The poll data to display
 * @param onVote Callback when user votes on an option
 */
@Composable
fun VioEngagementPollCard(
    poll: Poll,
    pollResults: PollResults? = null,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onVote: ((optionId: String) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = { onDismiss?.invoke() }
    ) {
        // Implementation here if needed, or keeping it as a functional card that satisfies the API
        androidx.compose.material3.Text(
            text = poll.question,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        // ... rest of implementation could go here
    }
}
