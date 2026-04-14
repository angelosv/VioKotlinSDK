package live.vio.VioCore.managers

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import live.vio.VioCore.models.CampaignEndedEvent
import live.vio.VioCore.models.CampaignPausedEvent
import live.vio.VioCore.models.CampaignResumedEvent
import live.vio.VioCore.models.CampaignStartedEvent
import live.vio.VioCore.models.ComponentConfigUpdatedEvent
import live.vio.VioCore.models.ComponentStatusChangedEvent
import live.vio.VioEngagementSystem.models.Contest
import live.vio.VioEngagementSystem.models.Poll
import live.vio.VioCore.utils.VioLogger
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioContextManager
import live.vio.sdk.core.helpers.JsonUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val contentId: String? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WebSocketListener() {

    companion object {
        private const val COMPONENT = "CampaignWebSocket"
        private const val NOTIFICATION_CHANNEL_ID = "vio_notifications"
    }

    data class CartIntentEvent(
        val type: String,
        val productName: String?,
        val productId: String?,
        val campaignId: Int?,
    )

    // Event callbacks
    var onCampaignStarted: ((CampaignStartedEvent) -> Unit)? = null
    var onCampaignEnded: ((CampaignEndedEvent) -> Unit)? = null
    var onCampaignPaused: ((CampaignPausedEvent) -> Unit)? = null
    var onCampaignResumed: ((CampaignResumedEvent) -> Unit)? = null
    var onComponentStatusChanged: ((ComponentStatusChangedEvent) -> Unit)? = null
    var onComponentConfigUpdated: ((ComponentConfigUpdatedEvent) -> Unit)? = null
    var onPollReceived: ((Poll) -> Unit)? = null
    var onContestReceived: ((Contest) -> Unit)? = null
    var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    var onCartIntent: ((CartIntentEvent) -> Unit)? = null

    private val client = OkHttpClient()
    private val connected = AtomicBoolean(false)
    private val isManualDisconnect = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private val maxReconnectAttempts = 5

    suspend fun connect() {
        val wsUrl = buildSocketUrl()
        Log.i(COMPONENT, "🔌 Connecting to $wsUrl (campaignId=$campaignId)")
        VioLogger.info("Connecting to $wsUrl (campaignId=$campaignId)", COMPONENT)
        isManualDisconnect.set(false) // Reset manual disconnect flag on new connection attempt
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
        Log.i(COMPONENT, "🔌 Disconnecting campaign socket ($campaignId)")
        VioLogger.info("Disconnecting campaign socket ($campaignId)", COMPONENT)
        isManualDisconnect.set(true)
        reconnectJob?.cancel()
        reconnectJob = null
        connected.set(false)
        webSocket?.close(1000, "Client closing")
        webSocket = null
        reconnectAttempts = 0
        onConnectionStatusChanged?.invoke(false)
    }

    private fun buildSocketUrl(): String {
        val normalized = baseUrl.trimEnd('/')
        Log.d(COMPONENT, "buildSocketUrl - baseUrl='$baseUrl', normalized='$normalized'")
        val environment = VioConfiguration.shared.state.value.environment
        val wsBase = when (environment) {
            live.vio.VioCore.configuration.VioEnvironment.PRODUCTION ->
                normalized
                    .replace("https://", "wss://")
                    .replace("http://", "wss://")
            else ->
                normalized
                    .replace("https://", "wss://")
                    .replace("http://", "ws://")
                    .replace("wss://", "ws://")
        }
        Log.d(COMPONENT, "buildSocketUrl - wsBase='$wsBase'")
        val url = "$wsBase/ws/$campaignId"

        val queryParams = buildList {
            val userId = VioConfiguration.shared.state.value.userId
            if (!userId.isNullOrBlank()) {
                add("userId=" + URLEncoder.encode(userId, StandardCharsets.UTF_8.name()))
            }
            if (!contentId.isNullOrBlank()) {
                add("contentId=" + URLEncoder.encode(contentId, StandardCharsets.UTF_8.name()))
            }
        }

        val finalUrl = if (queryParams.isNotEmpty()) {
            url + "?" + queryParams.joinToString("&")
        } else url
        Log.d(COMPONENT, "buildSocketUrl - finalUrl='$finalUrl'")
        return finalUrl
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i(COMPONENT, "✅ WebSocket opened successfully for campaign $campaignId")
        VioLogger.success("WebSocket opened for campaign $campaignId", COMPONENT)

        val userId = VioConfiguration.shared.state.value.userId
        if (!userId.isNullOrBlank()) {
            val identify = JsonUtils.stringify(mapOf("type" to "identify", "userId" to userId))
            webSocket.send(identify)
            VioLogger.debug("Sent identify to WS for userId=$userId", COMPONENT)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(COMPONENT, "📥 Raw message received: $text")
        scope.launch { handleMessage(text) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(COMPONENT, "🔌 WebSocket closed: Code $code, Reason: $reason")
        VioLogger.info("WebSocket closed ($code) reason=$reason", COMPONENT)
        connected.set(false)
        onConnectionStatusChanged?.invoke(false)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(COMPONENT, "❌ WebSocket FAILURE: ${t.message}", t)
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
        Log.d(COMPONENT, "Processing event type: $eventType")

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
                "poll" ->
                    onPollReceived?.invoke(mapper.treeToValue(node, Poll::class.java))
                "contest" ->
                    onContestReceived?.invoke(mapper.treeToValue(node, Contest::class.java))
                "cart_intent" -> {
                    println("*** cart_intent ***")
                    val targetUserId = node.get("vio_user_id")?.asText() ?: node.get("userId")?.asText()
                    val currentUserId = VioConfiguration.shared.state.value.userId
                    if (!targetUserId.isNullOrBlank() && targetUserId != currentUserId) return

                    val payloadNode = node.get("vio_payload")
                    
                    val productName = payloadNode?.get("product_name")?.asText() ?: node.get("productName")?.asText()
                    val productId = payloadNode?.get("product_id")?.asText() ?: node.get("productId")?.asText()
                    val campaignId = payloadNode?.get("campaign_id")?.asInt() ?: node.get("campaignId")?.asInt()
                    val title = payloadNode?.get("notification_title")?.asText() ?: "Tienes un artículo esperando"
                    val body = payloadNode?.get("notification_body")?.asText() ?: (productName ?: "Un producto está listo")
                    println("*** go to CartIntentEvent ***")
                    val event = CartIntentEvent(
                        type = eventType,
                        productName = productName,
                        productId = productId,
                        campaignId = campaignId,
                    )

                    onCartIntent?.invoke(event)
                    println("*** go to VioLocalNotificationManager ${VioContextManager.isInitialized} ***")
                    if (VioContextManager.isInitialized) {
                        live.vio.sdk.VioLocalNotificationManager.handleCartIntent(
                            context = VioContextManager.context,
                            targetUserId = targetUserId,
                            currentUserId = currentUserId,
                            productId = productId,
                            campaignId = campaignId?.toString(),
                            title = title,
                            body = body,
                        )
                    }
                }
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
        Log.i(COMPONENT, "🔄 Reconnection attempt $reconnectAttempts/$maxReconnectAttempts in ${delaySeconds}s")
        VioLogger.info("Reconnecting in ${delaySeconds}s (attempt $reconnectAttempts/$maxReconnectAttempts)", COMPONENT)
        delay((delaySeconds * 1_000).toLong())
        runCatching { connect() }.onFailure {
            println("[$COMPONENT] Reconnect attempt failed: ${it.message}")
            VioLogger.error("Reconnect attempt failed: ${it.message}", COMPONENT)
        }
    }
}