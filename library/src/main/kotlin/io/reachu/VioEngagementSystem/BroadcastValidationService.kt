package io.reachu.VioEngagementSystem

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.models.BroadcastValidationDto
import io.reachu.VioCore.models.BroadcastValidationResult
import io.reachu.VioCore.utils.VioLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Servicio que valida si un contentId tiene engagement activo en la plataforma Vio.
 *
 * Llama al endpoint REST `GET /v1/sdk/broadcast?contentId=<id>&country=<code>` con el
 * header `X-Api-Key`. Usa `campaignAdminApiKey` de [io.reachu.VioCore.configuration.CampaignConfiguration]
 * como llave primaria, con fallback al `apiKey` general del SDK.
 *
 * Uso:
 * ```kotlin
 * val result = BroadcastValidationService.validate("my-content-id", "NO")
 * if (result.hasEngagement && result.broadcastId != null) {
 *     EngagementManager.shared.loadEngagement(BroadcastContext(broadcastId = result.broadcastId))
 * }
 * ```
 */
object BroadcastValidationService {

    private const val COMPONENT = "BroadcastValidation"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Valida si el [contentId] dado tiene engagement activo para el [country] especificado.
     *
     * @param contentId Identificador del contenido (stream) a validar.
     * @param country Código de país ISO 3166-1 alpha-2 (e.g. "NO", "SE", "US").
     * @return [BroadcastValidationResult] con `hasEngagement = false` si no hay engagement
     *         o si ocurre un error de red.
     */
    suspend fun validate(contentId: String, country: String): BroadcastValidationResult {
        val config = VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')

        // campaignAdminApiKey tiene precedencia; fallback al apiKey general
        val apiKey = config.campaign.campaignAdminApiKey
            ?.takeIf { it.isNotBlank() }
            ?: config.apiKey

        val encodedContentId = URLEncoder.encode(contentId, "UTF-8")
        val encodedCountry = URLEncoder.encode(country, "UTF-8")
        val url = "$baseUrl/v1/sdk/broadcast?contentId=$encodedContentId&country=$encodedCountry"

        VioLogger.debug("Validating broadcast for contentId=$contentId country=$country", COMPONENT)

        return try {
            val response = httpGet(url, apiKey)
            when {
                response == null -> {
                    VioLogger.warning("No response from broadcast validation endpoint", COMPONENT)
                    noEngagementResult()
                }
                response.statusCode == 404 -> {
                    VioLogger.debug("No broadcast found for contentId=$contentId (404)", COMPONENT)
                    noEngagementResult()
                }
                response.statusCode !in 200..299 -> {
                    VioLogger.warning(
                        "Broadcast validation returned ${response.statusCode} for contentId=$contentId",
                        COMPONENT,
                    )
                    noEngagementResult()
                }
                else -> {
                    val dto = json.decodeFromString<BroadcastValidationDto>(response.body)
                    val result = dto.toDomain()
                    if (result.hasEngagement) {
                        VioLogger.success(
                            "Engagement active for contentId=$contentId → broadcastId=${result.broadcastId}",
                            COMPONENT,
                        )
                    } else {
                        VioLogger.debug("No engagement for contentId=$contentId", COMPONENT)
                    }
                    result
                }
            }
        } catch (e: Exception) {
            VioLogger.error("Error validating broadcast: ${e.message}", COMPONENT)
            noEngagementResult()
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun noEngagementResult() = BroadcastValidationResult(
        hasEngagement = false,
        broadcastId = null,
        broadcastName = null,
        status = null,
        campaignId = null,
        websocketChannel = null,
    )

    private data class HttpResponse(val statusCode: Int, val body: String)

    private suspend fun httpGet(url: String, apiKey: String): HttpResponse? =
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-Api-Key", apiKey)
                }
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                connection.disconnect()
                HttpResponse(statusCode, body)
            } catch (e: Exception) {
                null
            }
        }
}
