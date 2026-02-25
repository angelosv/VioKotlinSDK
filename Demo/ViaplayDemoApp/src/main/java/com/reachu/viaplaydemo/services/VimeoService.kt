package com.reachu.viaplaydemo.services

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Kotlin port of `Services/VimeoService.swift`.
 *
 * The demo relies on public Vimeo player pages instead of the full OAuth flow,
 * so we replicate the same HTML-scraping approach to extract the HLS stream URL.
 */
class VimeoService private constructor(
    private val client: OkHttpClient = defaultClient,
) {

    suspend fun getVideoStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val playerUrl = "https://player.vimeo.com/video/$videoId?h=b3793bd327"
        val request = Request.Builder()
            .url(playerUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
            )
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw VimeoException("Empty response body")
        if (!response.isSuccessful) {
            throw VimeoException("Failed to fetch player page (${response.code})")
        }

        val configJson = extractPlayerConfig(body)
            ?: throw VimeoException("Could not extract player config")

        val streamUrl = extractHlsUrl(configJson)
            ?: extractMp4Url(configJson)
            ?: throw VimeoException("Stream URL not found in player config")

        streamUrl
    }

    private fun extractPlayerConfig(html: String): String? {
        val patterns = listOf(
            Regex("var\\s+config\\s*=\\s*(\\{[\\s\\S]*?\\});"),
            Regex("window\\.playerConfig\\s*=\\s*(\\{[\\s\\S]*?\\});"),
            Regex("\\\\\"config\\\\\":(\\{[^}]+\\})"),
        )
        for (regex in patterns) {
            val match = regex.find(html)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractHlsUrl(config: String): String? =
        Regex("https://[^\"\\\\]+\\.m3u8[^\"\\\\]*")
            .find(config)
            ?.value
            ?.normalizeUrl()

    private fun extractMp4Url(config: String): String? =
        Regex("https://[^\"\\\\]+\\.mp4\\?[^\"\\\\]+")
            .find(config)
            ?.value
            ?.normalizeUrl()

    private fun String.normalizeUrl(): String =
        replace("\\u0026", "&")
            .replace("\\/", "/")

    class VimeoException(message: String, cause: Throwable? = null) : IOException(message, cause)

    companion object {
        val shared: VimeoService = VimeoService()

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
