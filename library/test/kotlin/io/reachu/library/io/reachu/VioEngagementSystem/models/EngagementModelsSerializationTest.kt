package io.reachu.VioEngagementSystem.models

import io.reachu.VioCore.models.BroadcastContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EngagementModelsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `BroadcastContext se serializa con campos legacy matchId y matchStartTime`() {
        val context = BroadcastContext(
            broadcastId = "b1",
            channelId = "ch1",
            startTime = "2024-01-01T00:00:00Z",
            endTime = "2024-01-01T01:00:00Z",
            legacyMatchId = "legacy-match",
            legacyMatchStartTime = "2023-12-31T23:59:00Z",
        )

        val encoded = json.encodeToString(context)
        // Verificamos que los campos legacy estén presentes en el JSON
        assert(encoded.contains("matchId"))
        assert(encoded.contains("matchStartTime"))
    }

    @Test
    fun `BroadcastContext puede deserializar JSON legacy con matchId y matchStartTime`() {
        val legacyJson = """
            {
              "broadcastId": "b1",
              "matchId": "legacy-id",
              "matchStartTime": "2023-01-01T10:00:00Z"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(BroadcastContext.serializer(), legacyJson)

        assertEquals("b1", decoded.broadcastId)
        assertEquals("legacy-id", decoded.legacyMatchId)
        assertEquals("2023-01-01T10:00:00Z", decoded.legacyMatchStartTime)
    }
}

