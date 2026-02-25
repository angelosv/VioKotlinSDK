package io.reachu.liveui.components

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object DynamicComponentsService {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetch(streamId: String): List<DynamicComponent> = withContext(Dispatchers.IO) {
        val url = URL("https://api-ecom-dev.vio.live/api/components/stream/$streamId")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "THVXN06-MGB4D4P-KCPRCKP-RHGT6VJ")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val status = connection.responseCode
            val responseText = (connection.inputStream ?: connection.errorStream).bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                throw IllegalStateException("Dynamic component request failed ($status): $responseText")
            }

            val dtoList = json.decodeFromString(ListSerializer(DynamicComponentRemoteDto.serializer()), responseText)
            dtoList.mapNotNull { it.toDomain() }
        } finally {
            connection.disconnect()
        }
    }
}
