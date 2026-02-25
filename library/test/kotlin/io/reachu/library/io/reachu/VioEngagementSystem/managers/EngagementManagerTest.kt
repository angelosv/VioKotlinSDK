package io.reachu.VioEngagementSystem.managers

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioEnvironment
import io.reachu.VioCore.models.BroadcastContext
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption
import io.reachu.VioEngagementSystem.models.PollOptionResults
import io.reachu.VioEngagementSystem.models.PollResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EngagementManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        // Configurar el SDK en entorno DEVELOPMENT para forzar el uso de repositorio demo
        VioConfiguration.configure(
            apiKey = "test-api-key",
            environment = VioEnvironment.DEVELOPMENT,
        )
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `voteInPoll actualiza resultados de forma optimista`() = runTest {
        val context = BroadcastContext(broadcastId = "broadcast-1")

        // Configurar estado interno mínimo para poder votar
        val poll = Poll(
            id = "poll-1",
            broadcastId = context.broadcastId,
            question = "Pregunta demo",
            options = listOf(
                PollOption(id = "opt-1", text = "Sí"),
                PollOption(id = "opt-2", text = "No"),
            ),
            isActive = true,
            totalVotes = 0,
            broadcastContext = context,
        )

        // Inyectar polls y resultados iniciales en el manager mediante reflexión
        val manager = EngagementManager.shared

        val pollsField = EngagementManager::class.java.getDeclaredField("_pollsByBroadcast")
        pollsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val pollsState = pollsField.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<Poll>>>
        pollsState.value = mapOf(context.broadcastId to listOf(poll))

        val resultsField = EngagementManager::class.java.getDeclaredField("_pollResults")
        resultsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val resultsState = resultsField.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<Map<String, PollResults>>
        resultsState.value = mapOf(
            poll.id to PollResults(
                pollId = poll.id,
                totalVotes = 0,
                options = listOf(
                    PollOptionResults(optionId = "opt-1", voteCount = 0, percentage = 0.0),
                    PollOptionResults(optionId = "opt-2", voteCount = 0, percentage = 0.0),
                ),
            ),
        )

        // Ejecutar voto
        manager.voteInPoll(
            pollId = poll.id,
            optionId = "opt-1",
            broadcastContext = context,
        )

        // Verificar que se marca la participación y que los resultados se actualizan en memoria
        assertTrue(manager.hasVotedInPoll(poll.id))

        val updatedResults = manager.pollResults.value[poll.id]!!
        assertEquals(1, updatedResults.totalVotes, "El total de votos debe incrementarse optimistamente")

        val votedOption = updatedResults.options.first { it.optionId == "opt-1" }
        assertEquals(1, votedOption.voteCount, "El conteo de la opción votada debe incrementarse")
        assertTrue(votedOption.percentage > 0.0, "La opción votada debe tener porcentaje mayor que 0")
    }
}

