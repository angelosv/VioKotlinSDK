package com.vio.viaplaydemo.services

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.vio.VioCore.configuration.VioConfiguration
import org.json.JSONObject

/**
 * Servicio simple para obtener el marcador de un broadcast desde el backend.
 *
 * Endpoint esperado:
 *   GET /v1/sdk/broadcasts/:id/score
 * con header:
 *   X-Api-Key: <apiKey>
 *
 * El `baseUrl` y el apiKey se leen de `VioConfiguration.shared.state.value.campaign`.
 */
class BackendMatchDataService {

    private data class ScoreDto(
        val home: Int,
        val away: Int,
        val minute: Int? = null,
        val period: String? = null,
    )

    data class MatchScore(
        val home: Int,
        val away: Int,
        val minute: Int? = null,
        val period: String? = null,
    )

    private data class HttpResponse(val statusCode: Int, val body: String)

    /**
     * Fetch único del marcador para un [broadcastId] concreto.
     *
     * Devuelve `null` si hay cualquier error de red o de parseo.
     */
    suspend fun fetchScore(broadcastId: String): MatchScore? {
        val config = VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')

        // campaignApiKey tiene prioridad; luego campaignAdminApiKey; fallback al apiKey general
        val apiKey = config.campaign.campaignApiKey
            ?.takeIf { it.isNotBlank() }
            ?: config.campaign.campaignAdminApiKey?.takeIf { it.isNotBlank() }
            ?: config.apiKey

        if (broadcastId.isBlank() || baseUrl.isBlank() || apiKey.isBlank()) return null

        val url = "$baseUrl/v1/sdk/broadcasts/$broadcastId/score"

        return try {
            val response = httpGet(url, apiKey)
            if (response == null || response.statusCode !in 200..299) {
                null
            } else {
                runCatching {
                    val obj = JSONObject(response.body)
                    val dto = ScoreDto(
                        home = obj.optInt("home"),
                        away = obj.optInt("away"),
                        minute = if (obj.has("minute") && !obj.isNull("minute")) obj.optInt("minute") else null,
                        period = obj.optString("period").takeIf { it.isNotBlank() },
                    )
                    MatchScore(
                        home = dto.home,
                        away = dto.away,
                        minute = dto.minute,
                        period = dto.period,
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

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
            } catch (_: Exception) {
                null
            }
        }
}

