package com.vio.demoapp.viaplay.components

import androidx.compose.runtime.Composable
import com.vio.demoapp.viaplay.models.CastingContestEvent
import live.vio.VioEngagementUI.Components.VioEngagementContestCard
import live.vio.VioEngagementSystem.models.Contest
import live.vio.VioEngagementSystem.models.ContestType

/**
 * Wrapper for VioEngagementContestCard specifically for the casting demo.
 * Converts CastingContestEvent to SDK Contest format.
 */
@Composable
fun CastingContestCardWrapper(
    contestEvent: CastingContestEvent,
    onParticipate: () -> Unit,
    onDismiss: () -> Unit
) {
    // Map demo model to SDK model
    val contest = Contest(
        id = contestEvent.id,
        broadcastId = "demo_broadcast",
        title = contestEvent.title,
        description = contestEvent.description,
        prize = contestEvent.prize,
        contestType = if (contestEvent.contestType.lowercase() == "quiz") ContestType.quiz else ContestType.giveaway,
        isActive = contestEvent.isActive
    )

    VioEngagementContestCard(
        contest = contest,
        sponsorLogoUrl = contestEvent.metadata?.get("sponsorLogoUrl"),
        onJoin = onParticipate,
        onDismiss = onDismiss
    )
}
