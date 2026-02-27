package io.reachu.VioEngagementSystem

import io.reachu.VioCore.configuration.CampaignConfiguration
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioEnvironment
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Pruebas unitarias para [BroadcastValidationService].
 *
 * Los tests verifican el parsing de JSON y la lógica de selección del API key.
 * Las llamadas de red reales no se ejecutan en las pruebas unitarias.
 */
class BroadcastValidationServiceTest {

    @BeforeEach
    fun setUp() {
        VioConfiguration.configure(
            apiKey = "general-api-key",
            environment = VioEnvironment.SANDBOX,
        )
    }

    // ── Tests de parseo de JSON a través del DTO ──────────────────────────────

    @Test
    fun `BroadcastValidationDto con hasEngagement true se convierte correctamente`() = runTest {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val jsonString = """
            {
                "hasEngagement": true,
                "broadcastId": "broadcast-123",
                "broadcastName": "El Clásico",
                "status": "active",
                "campaignId": "campaign-456",
                "websocketChannel": "channel-789",
                "campaignComponents": ["polls", "contests"],
                "broadcastComponents": ["chat"]
            }
        """.trimIndent()

        val dto = json.decodeFromString<io.reachu.VioCore.models.BroadcastValidationDto>(jsonString)
        val result = dto.toDomain()

        assertTrue(result.hasEngagement)
        assertEquals("broadcast-123", result.broadcastId)
        assertEquals("El Clásico", result.broadcastName)
        assertEquals("active", result.status)
        assertEquals("campaign-456", result.campaignId)
        assertEquals("channel-789", result.websocketChannel)
        assertEquals(listOf("polls", "contests"), result.campaignComponents)
        assertEquals(listOf("chat"), result.broadcastComponents)
    }

    @Test
    fun `BroadcastValidationDto con hasEngagement false se convierte correctamente`() = runTest {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val jsonString = """{"hasEngagement": false}"""

        val dto = json.decodeFromString<io.reachu.VioCore.models.BroadcastValidationDto>(jsonString)
        val result = dto.toDomain()

        assertFalse(result.hasEngagement)
        assertNull(result.broadcastId)
        assertNull(result.broadcastName)
        assertTrue(result.campaignComponents.isEmpty())
        assertTrue(result.broadcastComponents.isEmpty())
    }

    @Test
    fun `BroadcastValidationDto ignora campos desconocidos en el JSON`() = runTest {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val jsonString = """
            {
                "hasEngagement": true,
                "broadcastId": "b1",
                "unknownFutureField": "some-value",
                "anotherUnknown": 42
            }
        """.trimIndent()

        // No debe lanzar excepción
        val dto = json.decodeFromString<io.reachu.VioCore.models.BroadcastValidationDto>(jsonString)
        val result = dto.toDomain()

        assertTrue(result.hasEngagement)
        assertEquals("b1", result.broadcastId)
    }

    // ── Tests de configuración del API Key ────────────────────────────────────

    @Test
    fun `configuración con campaignAdminApiKey usa esa clave como prioritaria`() {
        // Configurar con campaignAdminApiKey específico
        VioConfiguration.configure(
            apiKey = "general-key",
            environment = VioEnvironment.SANDBOX,
            campaignConfig = CampaignConfiguration(
                campaignAdminApiKey = "campaign-specific-key",
            ),
        )

        val config = VioConfiguration.shared.state.value
        val effectiveKey = config.campaign.campaignAdminApiKey
            ?.takeIf { it.isNotBlank() }
            ?: config.apiKey

        assertEquals("campaign-specific-key", effectiveKey)
    }

    @Test
    fun `sin campaignAdminApiKey hace fallback al apiKey general`() {
        // CampaignConfiguration sin campaignAdminApiKey (null por defecto)
        VioConfiguration.configure(
            apiKey = "general-key",
            environment = VioEnvironment.SANDBOX,
            campaignConfig = CampaignConfiguration(
                campaignAdminApiKey = null,
            ),
        )

        val config = VioConfiguration.shared.state.value
        val effectiveKey = config.campaign.campaignAdminApiKey
            ?.takeIf { it.isNotBlank() }
            ?: config.apiKey

        assertEquals("general-key", effectiveKey)
    }

    @Test
    fun `campaignAdminApiKey en blanco hace fallback al apiKey general`() {
        VioConfiguration.configure(
            apiKey = "general-key",
            environment = VioEnvironment.SANDBOX,
            campaignConfig = CampaignConfiguration(
                campaignAdminApiKey = "   ",
            ),
        )

        val config = VioConfiguration.shared.state.value
        val effectiveKey = config.campaign.campaignAdminApiKey
            ?.takeIf { it.isNotBlank() }
            ?: config.apiKey

        assertEquals("general-key", effectiveKey)
    }
}
