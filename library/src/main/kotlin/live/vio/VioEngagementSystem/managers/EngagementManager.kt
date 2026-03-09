package live.vio.VioEngagementSystem.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.configuration.VioEnvironment
import live.vio.VioCore.models.BroadcastContext
import live.vio.VioCore.models.VioSessionContext
import live.vio.VioEngagementSystem.BroadcastValidationService
import live.vio.VioEngagementSystem.models.*
import live.vio.VioEngagementSystem.repositories.EngagementRepository
import live.vio.VioEngagementSystem.repositories.EngagementRepositoryBackend
import live.vio.VioEngagementSystem.repositories.EngagementRepositoryDemo
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

    /**
     * Valida un [contentId] contra el endpoint `/v1/sdk/broadcast` y, si hay engagement
     * activo, carga polls y contests para el broadcastId devuelto.
     *
     * @param contentId Identificador del contenido (stream) a validar.
     * @param country Código de país ISO 3166-1 alpha-2 (e.g. "NO", "SE").
     */
    fun loadEngagementForContent(contentId: String, country: String) {
        scope.launch {
            val result = BroadcastValidationService.validate(contentId, country)
            if (!result.hasEngagement || result.broadcastId == null) {
                println("EngagementManager: no engagement for contentId=$contentId")
                return@launch
            }
            loadEngagement(BroadcastContext(broadcastId = result.broadcastId))
        }
    }

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
    fun voteInPoll(pollId: String, optionId: String, broadcastContext: BroadcastContext, session: VioSessionContext? = null) {
        val polls = _pollsByBroadcast.value[broadcastContext.broadcastId]
        val poll = polls?.find { it.id == pollId } ?: throw EngagementError.PollNotFound()

        if (!VideoSyncManager.shared.isPollActive(poll)) {
             throw EngagementError.PollClosed()
        }

        if (hasVotedInPoll(pollId)) {
            throw EngagementError.AlreadyVoted()
        }

        userParticipation.markPollVoted(pollId, optionId)
        
        // Optimistic update
        updatePollResultsOptimistically(pollId, optionId)
        
        scope.launch {
            try {
                repository?.voteInPoll(pollId, optionId, broadcastContext, session)
            } catch (e: Exception) {
                // Determine how to handle rollback or error notification
                 println("Error voting: ${e.message}")
            }
        }
    }

    @Throws(EngagementError::class)
    fun participateInContest(contestId: String, broadcastContext: BroadcastContext, answers: Map<String, String>? = null, session: VioSessionContext? = null) {
        val contests = _contestsByBroadcast.value[broadcastContext.broadcastId]
        val contest = contests?.find { it.id == contestId } ?: throw EngagementError.ContestNotFound()

        if (!VideoSyncManager.shared.isContestActive(contest)) {
             // throw EngagementError.ContestClosed() // Assuming such error exists or just generic logic
        }

        userParticipation.markContestParticipated(contestId)
        
        scope.launch {
            try {
                repository?.participateInContest(contestId, broadcastContext, answers, session)
            } catch (e: Exception) {
                 println("Error participating in contest: ${e.message}")
            }
        }
    }

    /**
     * Añade o actualiza un poll en el estado local.
     * Usado por el sistema de WebSocket para actualizaciones en tiempo real.
     */
    fun addOrUpdatePoll(poll: Poll) {
        val broadcastId = poll.broadcastId
        val currentMap = _pollsByBroadcast.value.toMutableMap()
        val polls = currentMap[broadcastId]?.toMutableList() ?: mutableListOf()

        val index = polls.indexOfFirst { it.id == poll.id }
        if (index >= 0) {
            polls[index] = poll
        } else {
            polls.add(poll)
        }

        currentMap[broadcastId] = polls
        _pollsByBroadcast.value = currentMap

        // Actualizar resultados
        val optionsMetrics = poll.options.map { PollOptionResults(it.id, it.voteCount, it.percentage) }
        updatePollResults(poll.id, PollResults(poll.id, poll.totalVotes, optionsMetrics))
    }

    /**
     * Añade o actualiza un contest en el estado local.
     */
    fun addOrUpdateContest(contest: Contest) {
        val broadcastId = contest.broadcastId
        val currentMap = _contestsByBroadcast.value.toMutableMap()
        val contests = currentMap[broadcastId]?.toMutableList() ?: mutableListOf()

        val index = contests.indexOfFirst { it.id == contest.id }
        if (index >= 0) {
            contests[index] = contest
        } else {
            contests.add(contest)
        }

        currentMap[broadcastId] = contests
        _contestsByBroadcast.value = currentMap
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

