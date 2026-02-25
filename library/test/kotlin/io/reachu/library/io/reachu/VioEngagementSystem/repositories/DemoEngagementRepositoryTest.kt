package io.reachu.VioEngagementSystem.repositories

import io.reachu.VioCore.models.BroadcastContext
import io.reachu.VioEngagementSystem.models.Contest
import io.reachu.VioEngagementSystem.models.ContestType
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoEngagementRepositoryTest {

    @Test
    fun `loadPolls y loadContests devuelven listas construidas por los conversores`() = runTest {
        val context = BroadcastContext(broadcastId = "b1")

        // Configurar proveedor de eventos y conversores
        DemoEngagementRepository.timelineEventsProvider = {
            listOf(
                mapOf("type" to "poll", "id" to "p1"),
                mapOf("type" to "contest", "id" to "c1"),
            )
        }

        DemoEngagementRepository.pollConverter = { event, ctx ->
            val map = event as Map<*, *>
            if (map["type"] == "poll") {
                Poll(
                    id = map["id"] as String,
                    broadcastId = ctx.broadcastId,
                    question = "Demo?",
                    options = listOf(PollOption("opt-1", "Yes")),
                    isActive = true,
                )
            } else {
                null
            }
        }

        DemoEngagementRepository.contestConverter = { event, ctx ->
            val map = event as Map<*, *>
            if (map["type"] == "contest") {
                Contest(
                    id = map["id"] as String,
                    broadcastId = ctx.broadcastId,
                    title = "Daily",
                    description = "",
                    prize = "Prize",
                    contestType = ContestType.giveaway,
                    isActive = true,
                )
            } else {
                null
            }
        }

        val repository: EngagementRepository = DemoEngagementRepository()

        val polls = repository.loadPolls(context)
        val contests = repository.loadContests(context)

        assertEquals(1, polls.size)
        assertEquals("p1", polls.first().id)
        assertEquals(1, contests.size)
        assertEquals("c1", contests.first().id)

        // Los métodos de voto y participación no lanzan errores en el modo demo
        repository.voteInPoll("p1", "opt-1", context)
        repository.participateInContest("c1", context, answers = mapOf("q1" to "a1"))

        // Simple smoke test: el flujo completo no debe lanzar excepciones
        assertTrue(true)
    }
}

