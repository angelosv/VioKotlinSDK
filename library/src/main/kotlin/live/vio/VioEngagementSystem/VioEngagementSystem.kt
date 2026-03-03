package live.vio.VioEngagementSystem

import live.vio.VioEngagementSystem.managers.EngagementManager

object VioEngagementSystem {
    fun configure() {
        BroadcastContextSetup.setup()
    }
}

typealias VioEngagementManager = EngagementManager
