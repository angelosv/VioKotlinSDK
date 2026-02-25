package io.reachu.VioEngagementSystem.managers

import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.helpers.JsonUtils
import java.util.prefs.Preferences
import com.fasterxml.jackson.core.type.TypeReference

/**
 * Manages user participation state for polls and contests.
 * Kotlin port of the Swift `UserParticipationManager`.
 */
class UserParticipationManager private constructor(
    private val preferences: Preferences = Preferences.userRoot().node("reachu.participation")
) {
    companion object {
        val shared = UserParticipationManager()
    }

    private val pollsKey = "reachu.participated.polls"
    private val contestsKey = "reachu.participated.contests"
    private val votesKey = "reachu.poll.votes"

    var participatedPolls: Set<String> = emptySet()
        private set
    var participatedContests: Set<String> = emptySet()
        private set
    var pollVotes: Map<String, String> = emptyMap()
        private set

    init {
        loadState()
    }

    // MARK: - Poll Participation

    fun hasVotedInPoll(pollId: String): Boolean {
        return participatedPolls.contains(pollId)
    }

    fun getVote(pollId: String): String? {
        return pollVotes[pollId]
    }

    fun recordPollVote(pollId: String, optionId: String) {
        participatedPolls = participatedPolls + pollId
        pollVotes = pollVotes + (pollId to optionId)
        saveState()
    }

    // MARK: - Contest Participation

    fun hasParticipatedInContest(contestId: String): Boolean {
        return participatedContests.contains(contestId)
    }

    fun recordContestParticipation(contestId: String) {
        participatedContests = participatedContests + contestId
        saveState()
    }

    // MARK: - Persistence

    private fun loadState() {
        try {
            val pollsJson = preferences.get(pollsKey, null)
            if (pollsJson != null) {
                participatedPolls = JsonUtils.mapper.readValue(pollsJson, object : TypeReference<Set<String>>() {})
            }

            val contestsJson = preferences.get(contestsKey, null)
            if (contestsJson != null) {
                participatedContests = JsonUtils.mapper.readValue(contestsJson, object : TypeReference<Set<String>>() {})
            }

            val votesJson = preferences.get(votesKey, null)
            if (votesJson != null) {
                pollVotes = JsonUtils.mapper.readValue(votesJson, object : TypeReference<Map<String, String>>() {})
            }
        } catch (e: Exception) {
            VioLogger.error("Failed to load participation state: ${e.message}", "UserParticipationManager")
        }
    }

    private fun saveState() {
        try {
            preferences.put(pollsKey, JsonUtils.stringify(participatedPolls))
            preferences.put(contestsKey, JsonUtils.stringify(participatedContests))
            preferences.put(votesKey, JsonUtils.stringify(pollVotes))
        } catch (e: Exception) {
            VioLogger.error("Failed to save participation state: ${e.message}", "UserParticipationManager")
        }
    }

    // MARK: - Reset (for testing)

    fun resetAll() {
        participatedPolls = emptySet()
        participatedContests = emptySet()
        pollVotes = emptyMap()
        saveState()
    }
}
