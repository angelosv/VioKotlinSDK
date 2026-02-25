package io.reachu.VioEngagementSystem.models

import io.reachu.VioCore.models.BroadcastContext
import kotlinx.serialization.Serializable

@Serializable
data class Poll(
    val id: String,
    val broadcastId: String,
    val question: String,
    val options: List<PollOption>,
    val startTime: String? = null,
    val endTime: String? = null,
    val videoStartTime: Int? = null,
    val videoEndTime: Int? = null,
    val broadcastStartTime: String? = null,
    val isActive: Boolean = true,
    val totalVotes: Int = 0,
    val broadcastContext: BroadcastContext? = null
)

@Serializable
data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Int = 0,
    val percentage: Double = 0.0
)

@Serializable
data class Contest(
    val id: String,
    val broadcastId: String,
    val title: String,
    val description: String,
    val prize: String,
    val contestType: ContestType,
    val startTime: String? = null,
    val endTime: String? = null,
    val videoStartTime: Int? = null,
    val videoEndTime: Int? = null,
    val broadcastStartTime: String? = null,
    val isActive: Boolean = true,
    val broadcastContext: BroadcastContext? = null
)

@Serializable
enum class ContestType {
    quiz,
    giveaway
}

@Serializable
data class PollResults(
    val pollId: String,
    val totalVotes: Int,
    val options: List<PollOptionResults>
)

@Serializable
data class PollOptionResults(
    val optionId: String,
    val voteCount: Int,
    val percentage: Double
)

sealed class EngagementError : Exception() {
    class PollNotFound : EngagementError()
    class ContestNotFound : EngagementError()
    class PollClosed : EngagementError()
    class AlreadyVoted : EngagementError()
    class VoteFailed : EngagementError()
    class ParticipationFailed : EngagementError()
    class InvalidURL : EngagementError()
    
    override val message: String?
        get() = when (this) {
            is PollNotFound -> "Poll not found for this match"
            is ContestNotFound -> "Contest not found for this match"
            is PollClosed -> "Poll is no longer active"
            is AlreadyVoted -> "You have already voted in this poll"
            is VoteFailed -> "Failed to submit vote"
            is ParticipationFailed -> "Failed to participate in contest"
            is InvalidURL -> "Invalid URL"
        }
}
