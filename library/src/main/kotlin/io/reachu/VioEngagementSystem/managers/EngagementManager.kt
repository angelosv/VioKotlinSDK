package io.reachu.VioEngagementSystem.managers

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioEnvironment
import io.reachu.VioCore.models.BroadcastContext
import io.reachu.VioEngagementSystem.models.*
import io.reachu.VioEngagementSystem.repositories.EngagementRepository
import io.reachu.VioEngagementSystem.repositories.EngagementRepositoryBackend
import io.reachu.VioEngagementSystem.repositories.EngagementRepositoryDemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EngagementManager private constructor() {

    companion object {
        val shared = EngagementManager()
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var repository: EngagementRepository? = null

    private val _pollsByBroadcast = MutableStateFlow<Map<String, List<Poll>>>(emptyMap())
    val pollsByBroadcast: StateFlow<Map<String, List<Poll>>> = _pollsByBroadcast.asStateFlow()

    private val _contestsByBroadcast = MutableStateFlow<Map<String, List<Contest>>>(emptyMap())
    val contestsByBroadcast: StateFlow<Map<String, List<Contest>>> = _contestsByBroadcast.asStateFlow()

    private val _pollResults = MutableStateFlow<Map<String, PollResults>>(emptyMap())
    val pollResults: StateFlow<Map<String, PollResults>> = _pollResults.asStateFlow()

    private val userParticipation = UserParticipationManager.shared

    fun loadEngagement(context: BroadcastContext) {
        val config = VioConfiguration.shared.state.value
        
        // Initialize repository if needed or if environment changed (simplified check)
        if (repository == null) {
             repository = if (config.environment == VioEnvironment.SANDBOX || config.environment == VioEnvironment.PRODUCTION) {
                 EngagementRepositoryBackend()
             } else {
                 EngagementRepositoryDemo()
             }
        }
        
        scope.launch {
            try {
                val polls = repository?.loadPolls(context) ?: emptyList()
                val currentPolls = _pollsByBroadcast.value.toMutableMap()
                currentPolls[context.broadcastId] = polls
                _pollsByBroadcast.value = currentPolls

                val contests = repository?.loadContests(context) ?: emptyList()
                val currentContests = _contestsByBroadcast.value.toMutableMap()
                currentContests[context.broadcastId] = contests
                _contestsByBroadcast.value = currentContests
                
                // Initialize poll results for loaded polls
                polls.forEach { poll ->
                    val optionsMetrics = poll.options.map { PollOptionResults(it.id, it.voteCount, it.percentage) }
                     updatePollResults(poll.id, PollResults(poll.id, poll.totalVotes, optionsMetrics))
                }

            } catch (e: Exception) {
                println("Error loading engagement: ${e.message}")
            }
        }
    }

    fun getActivePolls(context: BroadcastContext): List<Poll> {
        val allPolls = _pollsByBroadcast.value[context.broadcastId] ?: emptyList()
        return VideoSyncManager.shared.getActivePolls(allPolls)
    }

    fun getActiveContests(context: BroadcastContext): List<Contest> {
        val allContests = _contestsByBroadcast.value[context.broadcastId] ?: emptyList()
        return VideoSyncManager.shared.getActiveContests(allContests)
    }

    fun hasVotedInPoll(pollId: String): Boolean {
        return userParticipation.hasVotedInPoll(pollId)
    }

    fun hasParticipatedInContest(contestId: String): Boolean {
        return userParticipation.hasParticipatedInContest(contestId)
    }

    @Throws(EngagementError::class)
    fun voteInPoll(pollId: String, optionId: String, broadcastContext: BroadcastContext) {
        val polls = _pollsByBroadcast.value[broadcastContext.broadcastId]
        val poll = polls?.find { it.id == pollId } ?: throw EngagementError.PollNotFound()

        if (!VideoSyncManager.shared.isPollActive(poll)) {
             throw EngagementError.PollClosed()
        }

        if (hasVotedInPoll(pollId)) {
            throw EngagementError.AlreadyVoted()
        }

        userParticipation.recordPollVote(pollId, optionId)
        
        // Optimistic update
        updatePollResultsOptimistically(pollId, optionId)
        
        scope.launch {
            try {
                repository?.voteInPoll(pollId, optionId, broadcastContext)
            } catch (e: Exception) {
                // Determine how to handle rollback or error notification
                 println("Error voting: ${e.message}")
            }
        }
    }

    @Throws(EngagementError::class)
    fun participateInContest(contestId: String, broadcastContext: BroadcastContext, answers: Map<String, String>? = null) {
        val contests = _contestsByBroadcast.value[broadcastContext.broadcastId]
        val contest = contests?.find { it.id == contestId } ?: throw EngagementError.ContestNotFound()

        if (!VideoSyncManager.shared.isContestActive(contest)) {
             // throw EngagementError.ContestClosed() // Assuming such error exists or just generic logic
        }

        userParticipation.recordContestParticipation(contestId)
        
        scope.launch {
            try {
                repository?.participateInContest(contestId, broadcastContext, answers)
            } catch (e: Exception) {
                 println("Error participating in contest: ${e.message}")
            }
        }
    }
    
    fun updatePollResults(pollId: String, results: PollResults) {
        val currentResults = _pollResults.value.toMutableMap()
        currentResults[pollId] = results
        _pollResults.value = currentResults
    }

    private fun updatePollResultsOptimistically(pollId: String, optionId: String) {
        val currentResultsMap = _pollResults.value.toMutableMap()
        val existingResults = currentResultsMap[pollId]

        if (existingResults == null) {
             // If no results yet, cannot optimistically update easily without more info
             return
        }

        val updatedOptions = existingResults.options.map { option ->
            if (option.optionId == optionId) {
                option.copy(voteCount = option.voteCount + 1)
            } else {
                option
            }
        }
        
        val newTotalVotes = existingResults.totalVotes + 1
        
        // Recalculate percentages
        val recalculatedOptions = updatedOptions.map { option ->
            // Avoid division by zero, though totalVotes + 1 should be > 0
            val percentage = if (newTotalVotes > 0) (option.voteCount.toDouble() / newTotalVotes.toDouble()) * 100.0 else 0.0
            option.copy(percentage = percentage)
        }

        val updatedResults = existingResults.copy(
            totalVotes = newTotalVotes,
            options = recalculatedOptions
        )

        currentResultsMap[pollId] = updatedResults
        _pollResults.value = currentResultsMap
    }
}

