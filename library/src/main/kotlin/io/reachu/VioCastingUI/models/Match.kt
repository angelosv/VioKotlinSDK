package io.reachu.VioCastingUI.models

import io.reachu.VioCore.models.BroadcastContext

data class Match(
    val id: String,
    val title: String,
    val subtitle: String,
    val backgroundImage: String,
    val startTime: String? = null
) {
    fun toBroadcastContext(channelId: String? = null): BroadcastContext {
        return BroadcastContext(
            broadcastId = id,
            channelId = channelId ?: "default",
            legacyMatchId = id,
            legacyMatchStartTime = startTime,
            startTime = startTime
        )
    }

    companion object {
        val barcelonaPSG = Match(
            id = "barcelona_psg",
            title = "Barcelona vs PSG",
            subtitle = "Champions League - Quarter Finals",
            backgroundImage = "bg_barcelona_psg",
            startTime = "2025-10-27T20:00:00Z"
        )
    }
}
