package live.vio.VioEngagementSystem.repositories

import live.vio.VioCore.models.BroadcastContext
import live.vio.VioCore.models.VioSessionContext
import live.vio.VioEngagementSystem.models.*
import kotlinx.coroutines.delay

/**
 * Demo implementation of EngagementRepository that uses timeline events
 * provided via configurable lambdas instead of making real backend calls.
 * 
 * Useful for development and testing with mock data.
 * 
 * Usage:
 * ```
 * // Configure from demo app
 * DemoEngagementRepository.timelineEventsProvider = { getTimelineEvents() }
 * DemoEngagementRepository.pollConverter = { event, context -> convertToPoll(event, context) }
 * DemoEngagementRepository.contestConverter = { event, context -> convertToContest(event, context) }
 * ```
 */
class DemoEngagementRepository : EngagementRepository {
    
    companion object {
        /**
         * Lambda to provide timeline events.
         * Should be configured from the demo app.
         */
        var timelineEventsProvider: (() -> List<Any>)? = null
        
        /**
         * Lambda to convert timeline events to Poll objects.
         * Should be configured from the demo app.
         */
        var pollConverter: ((Any, BroadcastContext) -> Poll?)? = null
        
        /**
         * Lambda to convert timeline events to Contest objects.
         * Should be configured from the demo app.
         */
        var contestConverter: ((Any, BroadcastContext) -> Contest?)? = null
    }
    
    override suspend fun loadPolls(context: BroadcastContext): List<Poll> {
        val eventsProvider = timelineEventsProvider
        val converter = pollConverter
        
        if (eventsProvider == null || converter == null) {
            println("⚠️ [DemoEngagementRepository] timelineEventsProvider or pollConverter not configured, returning empty list")
            return emptyList()
        }
        
        val events = eventsProvider()
        println("🎯 [DemoEngagementRepository] Loading polls from ${events.size} timeline events")
        
        val polls = events.mapNotNull { event ->
            converter(event, context)
        }
        
        println("✅ [DemoEngagementRepository] Loaded ${polls.size} polls for broadcast ${context.broadcastId}")
        return polls
    }
    
    override suspend fun loadContests(context: BroadcastContext): List<Contest> {
        val eventsProvider = timelineEventsProvider
        val converter = contestConverter
        
        if (eventsProvider == null || converter == null) {
            println("⚠️ [DemoEngagementRepository] timelineEventsProvider or contestConverter not configured, returning empty list")
            return emptyList()
        }
        
        val events = eventsProvider()
        println("🎯 [DemoEngagementRepository] Loading contests from ${events.size} timeline events")
        
        val contests = events.mapNotNull { event ->
            converter(event, context)
        }
        
        println("✅ [DemoEngagementRepository] Loaded ${contests.size} contests for broadcast ${context.broadcastId}")
        return contests
    }
    
    override suspend fun voteInPoll(
        pollId: String,
        optionId: String,
        broadcastContext: BroadcastContext,
        session: VioSessionContext?
    ) {
        // Simulate network delay (500ms)
        delay(500)
        println("🗳️ [DemoEngagementRepository] Simulated vote in poll $pollId for option $optionId (broadcast: ${broadcastContext.broadcastId})")
    }
    
    override suspend fun participateInContest(
        contestId: String,
        broadcastContext: BroadcastContext,
        answers: Map<String, String>?,
        session: VioSessionContext?
    ) {
        // Simulate network delay (500ms)
        delay(500)
        println("🎉 [DemoEngagementRepository] Simulated participation in contest $contestId (broadcast: ${broadcastContext.broadcastId})")
    }
}
