package io.reachu.liveshow.network

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.liveshow.models.TipioApiError
import io.reachu.liveshow.models.TipioLiveStream
import io.reachu.liveshow.models.TipioStatusResponse
import io.reachu.liveshow.models.TipioViewerCountData
import io.reachu.sdk.core.helpers.JsonUtils
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_BASE = "https://stg-dev-microservices.tipioapp.com"
private const val DEFAULT_API_KEY = "KCXF10Y-W5T4PCR-GG5119A-Z64SQ9S"

/**
 * Mirror of the Swift TipioApiClient backed by HttpURLConnection.
 */
class TipioApiClient(
    private val baseUrl: String = DEFAULT_BASE,
    private val apiKey: String = DEFAULT_API_KEY,
) {

    private val mapper get() = JsonUtils.mapper

    constructor(configuration: VioConfiguration.State) : this(
        baseUrl = configuration.liveShow.tipioBaseUrl.ifBlank { DEFAULT_BASE },
        apiKey = configuration.liveShow.tipioApiKey.ifBlank { DEFAULT_API_KEY },
    )

    suspend fun getLiveStream(id: Int): TipioLiveStream {
        val endpoint = "/api/livestreams/$id"
        println("🔗 [Tipio] Fetching livestream: $id")
        val body = request(endpoint, "GET")
        return runCatching {
            mapper.readValue(body, TipioLiveStream::class.java)
        }.onSuccess {
            println("✅ [Tipio] Successfully fetched livestream: ${it.title}")
        }.getOrElse {
            println("❌ [Tipio] Failed to decode livestream: ${it.message}")
            throw TipioApiError("DECODE_ERROR", "Failed to decode livestream data")
        }
    }

    suspend fun getActiveLiveStreams(): List<TipioLiveStream> {
        val endpoint = "/api/stg/livestreams/active"
        println("🔗 [Tipio] Fetching active livestreams")
        val body = request(endpoint, "GET")
        val type = mapper.typeFactory.constructCollectionType(List::class.java, TipioLiveStream::class.java)
        val streams: List<TipioLiveStream> = runCatching {
            mapper.readValue<List<TipioLiveStream>>(body, type)
        }.onSuccess { list ->
            println("✅ [Tipio] Successfully fetched ${list.size} active livestreams")
            list.forEachIndexed { index, stream ->
                println("🔍 [Tipio] Stream ${index + 1}:")
                println("   - ID: ${stream.id}")
                println("   - Title: '${stream.title}'")
                println("   - LiveStreamId: ${stream.liveStreamId}")
                println("   - Broadcasting: ${stream.broadcasting}")
                println("   - Thumbnail: ${stream.thumbnail ?: "nil"}")
            }
        }.getOrElse {
            println("❌ [Tipio] Failed to decode active livestreams: ${it.message}")
            println("❌ [Tipio] Raw response data: $body")
            throw TipioApiError("DECODE_ERROR", "Failed to decode active livestreams")
        }
        return streams
    }

    suspend fun startLiveStream(id: Int): TipioStatusResponse {
        val endpoint = "/api/livestreams/$id/start"
        println("🚀 [Tipio] Starting livestream: $id")
        val body = request(endpoint, "POST")
        return runCatching {
            mapper.readValue(body, TipioStatusResponse::class.java)
        }.onSuccess {
            println("✅ [Tipio] Successfully started livestream: $id")
        }.getOrElse {
            println("❌ [Tipio] Failed to start livestream: ${it.message}")
            throw TipioApiError("START_ERROR", "Failed to start livestream")
        }
    }

    suspend fun stopLiveStream(id: Int): TipioStatusResponse {
        val endpoint = "/api/livestreams/$id/stop"
        println("⏹️ [Tipio] Stopping livestream: $id")
        val body = request(endpoint, "POST")
        return runCatching {
            mapper.readValue(body, TipioStatusResponse::class.java)
        }.onSuccess {
            println("✅ [Tipio] Successfully stopped livestream: $id")
        }.getOrElse {
            println("❌ [Tipio] Failed to stop livestream: ${it.message}")
            throw TipioApiError("STOP_ERROR", "Failed to stop livestream")
        }
    }

    suspend fun getViewerCount(id: Int): Int {
        val endpoint = "/api/livestreams/$id/viewers"
        val body = request(endpoint, "GET")
        return runCatching {
            mapper.readValue(body, TipioViewerCountData::class.java)
        }.map {
            it.count
        }.getOrElse {
            println("❌ [Tipio] Failed to get viewer count: ${it.message}")
            throw TipioApiError("VIEWER_COUNT_ERROR", "Failed to get viewer count")
        }
    }

    private suspend fun request(endpoint: String, method: String, body: String? = null): String =
        withContext(Dispatchers.IO) {
            val url = URL(baseUrl.trimEnd('/') + endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 30_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", apiKey)
                setRequestProperty("User-Agent", "VioSDK/1.0")
                doOutput = body != null
            }
            try {
                if (body != null) {
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val response = stream.bufferedReader().use { it.readText() }
                if (status !in 200..299) {
                    handleHttpError(status)
                }
                response
            } finally {
                connection.disconnect()
            }
        }

    private fun handleHttpError(status: Int): Nothing = when {
        status in 200..299 -> error("handleHttpError should not be called for success codes")
        status == 401 -> throw TipioApiError("UNAUTHORIZED", "Invalid API key")
        status == 404 -> throw TipioApiError("NOT_FOUND", "Resource not found")
        status == 429 -> throw TipioApiError("RATE_LIMITED", "Rate limit exceeded")
        status in 500..599 -> throw TipioApiError("SERVER_ERROR", "Server error")
        else -> throw TipioApiError("HTTP_ERROR", "HTTP error: $status")
    }
}
