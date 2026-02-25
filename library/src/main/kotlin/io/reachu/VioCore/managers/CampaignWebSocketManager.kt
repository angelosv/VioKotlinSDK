package io.reachu.VioCore.managers

import io.reachu.VioCore.models.CampaignEndedEvent
import io.reachu.VioCore.models.CampaignPausedEvent
import io.reachu.VioCore.models.CampaignResumedEvent
import io.reachu.VioCore.models.CampaignStartedEvent
import io.reachu.VioCore.models.ComponentConfigUpdatedEvent
import io.reachu.VioCore.models.ComponentStatusChangedEvent
import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.helpers.JsonUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Mirrors the Swift CampaignWebSocketManager using java.net.http.WebSocket.
 */
class CampaignWebSocketManager(
    private val campaignId: Int,
    private val baseUrl: String,
    private val apiKey: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WebSocketListener() {

    companion object {
        private const val COMPONENT = "CampaignWebSocket"
    }

    // Event callbacks
    var onCampaignStarted: ((CampaignStartedEvent) -> Unit)? = null
    var onCampaignEnded: ((CampaignEndedEvent) -> Unit)? = null
    var onCampaignPaused: ((CampaignPausedEvent) -> Unit)? = null
    var onCampaignResumed: ((CampaignResumedEvent) -> Unit)? = null
    var onComponentStatusChanged: ((ComponentStatusChangedEvent) -> Unit)? = null
    var onComponentConfigUpdated: ((ComponentConfigUpdatedEvent) -> Unit)? = null
    var onConnectionStatusChanged: ((Boolean) -> Unit)? = null

    private val client = OkHttpClient()
    private val connected = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    suspend fun connect() {
        val wsUrl = buildSocketUrl()
        println("[$COMPONENT] Connecting to $wsUrl (campaignId=$campaignId)")
        VioLogger.debug("Connecting to $wsUrl (campaignId=$campaignId)", COMPONENT)
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(wsUrl)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                requestBuilder.header("X-API-Key", apiKey)
            }
            val socket = client.newWebSocket(requestBuilder.build(), this@CampaignWebSocketManager)
            webSocket = socket
            connected.set(true)
            reconnectAttempts = 0
        }
        onConnectionStatusChanged?.invoke(true)
    }

    fun disconnect() {
        println("[$COMPONENT] Disconnecting campaign socket ($campaignId)")
        VioLogger.debug("Disconnecting campaign socket ($campaignId)", COMPONENT)
        connected.set(false)
        webSocket?.close(1000, "Client closing")
        webSocket = null
        reconnectAttempts = 0
        onConnectionStatusChanged?.invoke(false)
    }

    private fun buildSocketUrl(): String {
        val normalized = baseUrl.trimEnd('/')
        val wsBase = normalized
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        return "$wsBase/ws/$campaignId"
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("[$COMPONENT] WebSocket opened successfully for campaign $campaignId")
        VioLogger.debug("WebSocket opened for campaign $campaignId", COMPONENT)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("[$COMPONENT] Raw message received: $text")
        scope.launch { handleMessage(text) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("[$COMPONENT] WebSocket closed: Code $code, Reason: $reason")
        VioLogger.debug("WebSocket closed ($code) reason=$reason", COMPONENT)
        connected.set(false)
        onConnectionStatusChanged?.invoke(false)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("[$COMPONENT] WebSocket FAILURE: ${t.message}")
        VioLogger.error("WebSocket error: ${t.message}", COMPONENT)
        if (connected.getAndSet(false)) {
            onConnectionStatusChanged?.invoke(false)
            scope.launch { attemptReconnect() }
        }
    }

    private suspend fun handleMessage(text: String) {
        VioLogger.debug("🔌 [WebSocket] Raw message received (${text.length} chars): $text", COMPONENT)
        val mapper = JsonUtils.mapper
        val node = runCatching { mapper.readTree(text) }.getOrElse {
            println("[$COMPONENT] Error parsing JSON: ${it.message}")
            VioLogger.error("Failed to parse message: ${it.message}", COMPONENT)
            return
        }
        val eventType = node.get("type")?.asText()
        VioLogger.debug("🔌 [WebSocket] Event type detected: '$eventType'", COMPONENT)
        println("[$COMPONENT] Processing event type: $eventType")

        if (eventType.isNullOrBlank()) {
            VioLogger.warning("Missing event type in payload", COMPONENT)
            return
        }

        try {
            when (eventType) {
                "campaign_started" ->
                    onCampaignStarted?.invoke(mapper.treeToValue(node, CampaignStartedEvent::class.java))
                "campaign_ended" ->
                    onCampaignEnded?.invoke(mapper.treeToValue(node, CampaignEndedEvent::class.java))
                "campaign_paused" ->
                    onCampaignPaused?.invoke(mapper.treeToValue(node, CampaignPausedEvent::class.java))
                "campaign_resumed" ->
                    onCampaignResumed?.invoke(mapper.treeToValue(node, CampaignResumedEvent::class.java))
                "component_status_changed" ->
                    onComponentStatusChanged?.invoke(mapper.treeToValue(node, ComponentStatusChangedEvent::class.java))
                "component_config_updated" ->
                    onComponentConfigUpdated?.invoke(mapper.treeToValue(node, ComponentConfigUpdatedEvent::class.java))
                else -> {
                    println("[$COMPONENT] Unknown event type received: $eventType")
                    VioLogger.warning("Unknown event type: $eventType (full message: $text)", COMPONENT)
                }
            }
        } catch (error: Exception) {
            println("[$COMPONENT] Error decoding $eventType: ${error.message}")
            VioLogger.error("Failed to decode $eventType: ${error.message}", COMPONENT)
        }
    }

    private suspend fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            println("[$COMPONENT] STOPPING reconnection: Max attempts reached ($maxReconnectAttempts)")
            VioLogger.error("Max reconnection attempts reached", COMPONENT)
            onConnectionStatusChanged?.invoke(false)
            return
        }
        reconnectAttempts += 1
        val delaySeconds = min(30.0, 2.0.pow(reconnectAttempts.toDouble()))
        println("[$COMPONENT] Reconnection attempt $reconnectAttempts/$maxReconnectAttempts in ${delaySeconds}s")
        VioLogger.debug("Reconnecting in ${delaySeconds}s (attempt $reconnectAttempts/$maxReconnectAttempts)", COMPONENT)
        delay((delaySeconds * 1_000).toLong())
        runCatching { connect() }.onFailure {
            println("[$COMPONENT] Reconnect attempt failed: ${it.message}")
            VioLogger.error("Reconnect attempt failed: ${it.message}", COMPONENT)
        }
    }
}