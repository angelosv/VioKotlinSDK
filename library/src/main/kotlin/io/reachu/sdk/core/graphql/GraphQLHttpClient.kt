package io.reachu.sdk.core.graphql

import io.reachu.sdk.core.errors.GraphQLFailure
import io.reachu.sdk.core.errors.NetworkError
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.helpers.JsonUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private typealias JsonMap = Map<String, Any?>

data class GraphQLHttpResponse(
    val data: JsonMap?,
    val errors: List<JsonMap>?,
    val status: Int,
)

/**
 * Replica ligera del cliente GraphQL de Swift utilizando HttpURLConnection y Jackson.
 */
open class GraphQLHttpClient(
    private val baseUrl: String,
    private val apiKey: String,
) {
    var timeoutMillis: Int = 30_000

    open suspend fun runQuerySafe(query: String, variables: Map<String, Any?>): GraphQLHttpResponse {
        return runOperationSafe(query, variables)
    }

    open suspend fun runMutationSafe(query: String, variables: Map<String, Any?>): GraphQLHttpResponse {
        return runOperationSafe(query, variables)
    }

    protected open suspend fun runOperationSafe(
        query: String,
        variables: Map<String, Any?>,
    ): GraphQLHttpResponse {
        val payload = mapOf(
            "query" to query,
            "variables" to variables,
        )

        return withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMillis
                readTimeout = timeoutMillis
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", apiKey)
                doOutput = true
            }

            try {
                connection.outputStream.use { out ->
                    val body = JsonUtils.stringify(payload)
                    out.write(body.toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode
                val responseStream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }

                val text = responseStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } ?: "{}"

                val root: JsonMap = JsonUtils.parseMap(text)
                val errors = root["errors"] as? List<Map<String, Any?>>
                val data = root["data"] as? JsonMap

                if (!errors.isNullOrEmpty()) {
                    val first = errors.first()
                    val message = (first["message"] as? String) ?: "GraphQL error"
                    val details = buildMap<String, Any?> {
                        val messages = errors.mapNotNull { it["message"] as? String }
                        if (messages.isNotEmpty()) put("messages", messages)
                        val codes = errors.mapNotNull {
                            (it["extensions"] as? Map<*, *>)?.get("code") as? String
                        }
                        if (codes.isNotEmpty()) put("codes", codes)
                    }.takeIf { it.isNotEmpty() }
                    val code = (first["extensions"] as? Map<*, *>)?.get("code") as? String
                    if (code != null) {
                        throw GraphQLErrorMapper.fromGqlCode(code, message, details)
                    } else {
                        throw GraphQLFailure(message, details = details)
                    }
                }

                if (status !in 200..299) {
                    throw GraphQLErrorMapper.fromStatus(
                        status,
                        "HTTP error",
                        mapOf("body" to text),
                    )
                }

                GraphQLHttpResponse(data, errors, status)
            } catch (ex: SdkException) {
                throw ex
            } catch (ex: Exception) {
                throw NetworkError("Network failure", details = mapOf("cause" to ex.message))
            } finally {
                connection.disconnect()
            }
        }
    }
}
