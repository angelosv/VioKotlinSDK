package io.reachu.VioEngagementSystem.managers

import io.reachu.VioEngagementSystem.models.Contest
import io.reachu.VioEngagementSystem.models.ContestType
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoSyncManagerTest {

    private val manager = VideoSyncManager.shared

    @Test
    fun `updateVideoTime actualiza el StateFlow`() {
        manager.reset()
        assertEquals(null, manager.currentVideoTime.value)

        manager.updateVideoTime(42)

        assertEquals(42, manager.currentVideoTime.value)
    }

    @Test
    fun `isPollActive respeta ventana de videoStartTime y videoEndTime`() {
        manager.reset()
        val poll = Poll(
            id = "poll",
            broadcastId = "b1",
            question = "q",
            options = listOf(PollOption("opt", "txt")),
            videoStartTime = 10,
            videoEndTime = 20,
            isActive = true,
        )

        assertFalse(manager.isPollActive(poll, videoTime = 5))
        assertTrue(manager.isPollActive(poll, videoTime = 10))
        assertTrue(manager.isPollActive(poll, videoTime = 15))
        assertTrue(manager.isPollActive(poll, videoTime = 20))
        assertFalse(manager.isPollActive(poll, videoTime = 21))
    }

    @Test
    fun `getActivePolls filtra por tiempo de video`() {
        manager.reset()
        val polls = listOf(
            Poll(
                id = "p1",
                broadcastId = "b1",
                question = "q1",
                options = emptyList(),
                videoStartTime = 0,
                videoEndTime = 10,
                isActive = true,
            ),
            Poll(
                id = "p2",
                broadcastId = "b1",
                question = "q2",
                options = emptyList(),
                videoStartTime = 20,
                videoEndTime = 30,
                isActive = true,
            ),
        )

        val activeAt5 = manager.getActivePolls(polls, videoTime = 5)
        assertEquals(listOf("p1"), activeAt5.map { it.id })

        val activeAt25 = manager.getActivePolls(polls, videoTime = 25)
        assertEquals(listOf("p2"), activeAt25.map { it.id })
    }

    @Test
    fun `isContestActive usa videoStartTime y videoEndTime`() {
        manager.reset()
        val contest = Contest(
            id = "c1",
            broadcastId = "b1",
            title = "Contest",
            description = "",
            prize = "Prize",
            contestType = ContestType.giveaway,
            videoStartTime = 100,
            videoEndTime = 200,
            isActive = true,
        )

        assertFalse(manager.isContestActive(contest, videoTime = 50))
        assertTrue(manager.isContestActive(contest, videoTime = 150))
        assertFalse(manager.isContestActive(contest, videoTime = 250))
    }
}

