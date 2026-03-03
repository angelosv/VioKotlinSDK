package live.vio.VioEngagementSystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.vio.VioCore.managers.CampaignManager
import live.vio.VioCore.managers.CampaignNotification
import live.vio.VioEngagementSystem.managers.EngagementManager

/**
 * Bridge between Campaign WebSocket events and EngagementManager.
 */
object BroadcastContextSetup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun setup() {
        scope.launch {
            CampaignManager.shared.events.collect { notification ->
                when (notification) {
                    is CampaignNotification.PollReceived -> {
                        EngagementManager.shared.addOrUpdatePoll(notification.poll)
                    }
                    is CampaignNotification.ContestReceived -> {
                        EngagementManager.shared.addOrUpdateContest(notification.contest)
                    }
                    else -> {}
                }
            }
        }
    }
}
