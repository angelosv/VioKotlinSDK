package io.reachu.VioEngagementSystem.managers

import io.reachu.VioEngagementSystem.models.Contest
import io.reachu.VioEngagementSystem.models.Poll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class VideoSyncManager private constructor() {

    companion object {
        val shared = VideoSyncManager()
    }

    private val _currentVideoTime = MutableStateFlow<Int?>(null)
    val currentVideoTime: StateFlow<Int?> = _currentVideoTime.asStateFlow()

    private val broadcastStartTimeByBroadcast = mutableMapOf<String, Date>()

    fun updateVideoTime(time: Int) {
        _currentVideoTime.value = time
    }

    fun setBroadcastStartTime(date: Date, broadcastId: String) {
        broadcastStartTimeByBroadcast[broadcastId] = date
    }

    fun getBroadcastStartTime(broadcastId: String): Date? {
        return broadcastStartTimeByBroadcast[broadcastId]
    }

    fun clearBroadcastStartTime(broadcastId: String) {
        broadcastStartTimeByBroadcast.remove(broadcastId)
    }

    fun reset() {
        _currentVideoTime.value = null
        broadcastStartTimeByBroadcast.clear()
    }

    fun isPollActive(poll: Poll, videoTime: Int? = null): Boolean {
        // If poll is manually deactivated, it's not active
        if (!poll.isActive) return false

        // Determine effective video time
        val time = videoTime ?: _currentVideoTime.value

        // If we have video start/end times defined, check against video time
        if (poll.videoStartTime != null && poll.videoEndTime != null) {
            val validTime = time ?: return false // If time is required but null, consider inactive
            return validTime >= poll.videoStartTime && validTime <= poll.videoEndTime
        }

        // TODO: Handle absolute timestamp checks if video times are not present but start/end timestamps are
        // and we have a broadcast start time to map video time to absolute time.
        
        return poll.isActive
    }

    fun isContestActive(contest: Contest, videoTime: Int? = null): Boolean {
        if (!contest.isActive) return false
        
        val time = videoTime ?: _currentVideoTime.value

        if (contest.videoStartTime != null && contest.videoEndTime != null) {
            val validTime = time ?: return false
            return validTime >= contest.videoStartTime && validTime <= contest.videoEndTime
        }
        
        return contest.isActive
    }

    fun getActivePolls(polls: List<Poll>, videoTime: Int? = null): List<Poll> {
        val time = videoTime ?: _currentVideoTime.value
        return polls.filter { isPollActive(it, time) }
    }

    fun getActiveContests(contests: List<Contest>, videoTime: Int? = null): List<Contest> {
        val time = videoTime ?: _currentVideoTime.value
        return contests.filter { isContestActive(it, time) }
    }
}
