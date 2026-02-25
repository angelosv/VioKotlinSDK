package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import io.reachu.VioEngagementSystem.models.Poll

/**
 * Reusable Compose component for displaying a poll card.
 * 
 * @param poll The poll data to display
 * @param onVote Callback when user votes on an option
 */
@Composable
fun ReachuEngagementPollCard(
    poll: Poll,
    onVote: ((optionId: String) -> Unit)? = null
) {
    // TODO: Implement poll card UI
    // - Display poll question
    // - Show poll options with vote counts/percentages
    // - Handle vote interaction
    // - Show active/inactive state
}
