package io.reachu.VioCore.analytics

import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.helpers.JsonUtils
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal HTTP client that sends events to the Mixpanel REST API.
 * It mirrors the behaviour of the Swift Mixpanel SDK usage without
 * requiring Android context or the official library.
 */
internal class MixpanelClient(
    private val token: String,
    apiHost: String?,
) {
    private val baseUrl = (apiHost ?: "https://api.mixpanel.com").trimEnd('/')
    private val trackUrl = "$baseUrl/track"
    private val engageUrl = "$baseUrl/engage"
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    suspend fun track(event: String, properties: Map<String, Any?>) {
        val payload = mapOf(
            "event" to event,
            "properties" to properties,
        )
        post(trackUrl, payload, "track")
    }

    suspend fun setPeopleProperties(distinctId: String, properties: Map<String, Any?>) {
        if (properties.isEmpty()) return
        val payload = mapOf(
            "\$token" to token,
            "\$distinct_id" to distinctId,
            "\$set" to properties,
        )
        post(engageUrl, payload, "people_set")
    }

    suspend fun appendTransaction(
        distinctId: String,
        amount: Double,
        currency: String?,
        metadata: Map<String, Any?> = emptyMap(),
    ) {
        val transaction = mutableMapOf<String, Any?>(
            "\$amount" to amount,
            "\$time" to isoFormatter.format(Instant.now()),
        )
        if (!currency.isNullOrBlank()) transaction["currency"] = currency
        metadata.forEach { (key, value) -> if (value != null) transaction[key] = value }

        val payload = mapOf(
            "\$token" to token,
            "\$distinct_id" to distinctId,
            "\$append" to mapOf("\$transactions" to transaction),
        )
        post(engageUrl, payload, "people_append")
    }

    private suspend fun post(url: String, body: Any, operation: String) {
        val json = JsonUtils.mapper.writeValueAsString(body)
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        val data = URLEncoder.encode(encoded, "UTF-8")
        val payload = "data=$data&verbose=1".toByteArray(Charsets.UTF_8)

        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            try {
                connection.outputStream.use { it.write(payload) }
                val status = connection.responseCode
                if (status !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    VioLogger.warning("Mixpanel $operation failed ($status): $errorBody", "AnalyticsManager")
                } else {
                    connection.inputStream?.bufferedReader()?.use { it.readText() }
                }
            } catch (t: Throwable) {
                VioLogger.error("Mixpanel $operation error: ${t.message}", "AnalyticsManager")
            } finally {
                connection.disconnect()
            }
        }
    }
}
