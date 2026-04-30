package live.vio.VioCore.managers

import android.content.Context
import android.util.Log
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
    }

    data class CartIntentEvent(
        val type: String,
        val productName: String?,
        val productId: String?,
        val campaignId: Int?,
        val notificationTitle: String?,
        val notificationBody: String?,
        val vioUserId: String?,
        val source: String?,
        val deeplink: String?,
        val activationId: Int? = null,
        val sponsorId: Int? = null,
    ) {
        companion object {
            fun parse(json: String): CartIntentEvent? {
                return runCatching {
                    val mapper = JsonUtils.mapper
                    val node = mapper.readTree(json)
                    
                    val targetUserId = node.get("vio_user_id")?.asText() ?: node.get("userId")?.asText()
                    val currentUserId = VioConfiguration.shared.state.value.userId
                    if (!targetUserId.isNullOrBlank() && targetUserId != currentUserId) return null

                    val payloadNode = node.get("vio_payload")
                    
                    val productName = payloadNode?.get("product_name")?.asText() ?: node.get("productName")?.asText()
                    val productId = payloadNode?.get("product_id")?.asText() ?: node.get("productId")?.asText()
                    val campaignId = payloadNode?.get("campaign_id")?.asInt() ?: node.get("campaignId")?.asInt()
                    val title = payloadNode?.get("notification_title")?.asText() ?: node.get("notificationTitle")?.asText()
                    val body = payloadNode?.get("notification_body")?.asText() ?: node.get("notificationBody")?.asText()
                    val source = payloadNode?.get("source")?.asText() ?: node.get("source")?.asText()
                    val deeplink = payloadNode?.get("deeplink")?.asText() ?: node.get("deeplink")?.asText()
                    val activationId = payloadNode?.get("activation_id")?.asInt() ?: node.get("activationId")?.asInt()
                    val sponsorId = payloadNode?.get("sponsor_id")?.asInt() ?: node.get("sponsorId")?.asInt()
                    
                    CartIntentEvent(
                        type = node.get("type")?.asText() ?: "cart_intent",
                        productName = productName,
                        productId = productId,
                        campaignId = campaignId,
                        notificationTitle = title,
                        notificationBody = body ?: productName,
                        vioUserId = targetUserId,
                        source = source,
                        deeplink = deeplink,
                        activationId = activationId,
                        sponsorId = sponsorId,
                    )
                }.getOrNull()
            }

            /**
             * Parse FCM push notification data into CartIntentEvent.
             */
            fun from(data: Map<String, Any>): CartIntentEvent? {
                return runCatching {
                    // Handle canonical format: vio_payload with nested data
                    val vioPayload = data["vio_payload"] as? Map<String, Any>
                    
                    // Extract fields from canonical payload or fallback to top-level keys
                    val productName = vioPayload?.get("product_name") as? String ?: data["productName"] as? String
                    val productId = vioPayload?.get("product_id") as? String ?: data["productId"] as? String ?: data["vio_cartIntent_productId"] as? String
                    val campaignId = vioPayload?.get("campaign_id") as? Int ?: (data["campaignId"] as? String)?.toIntOrNull() ?: (data["vio_cartIntent_campaignId"] as? String)?.toIntOrNull()
                    val title = vioPayload?.get("notification_title") as? String ?: data["notificationTitle"] as? String ?: data["vio_cartIntent_title"] as? String
                    val body = vioPayload?.get("notification_body") as? String ?: data["notificationBody"] as? String ?: data["vio_cartIntent_body"] as? String
                    val source = vioPayload?.get("source") as? String ?: data["source"] as? String
                    val deeplink = vioPayload?.get("deeplink") as? String ?: data["deeplink"] as? String ?: data["vio_cartIntent_deeplink"] as? String
                    val activationId = vioPayload?.get("activation_id") as? Int ?: (data["activationId"] as? String)?.toIntOrNull() ?: (data["vio_cartIntent_activationId"] as? String)?.toIntOrNull()
                    val sponsorId = vioPayload?.get("sponsor_id") as? Int ?: (data["sponsorId"] as? String)?.toIntOrNull() ?: (data["vio_cartIntent_sponsorId"] as? String)?.toIntOrNull()
                    
                    // Extract user ID for validation
                    val targetUserId = vioPayload?.get("vio_user_id") as? String ?: data["userId"] as? String ?: data["vio_cartIntent_userId"] as? String
                    val currentUserId = VioConfiguration.shared.state.value.userId
                    if (!targetUserId.isNullOrBlank() && targetUserId != currentUserId) return null

                    CartIntentEvent(
                        type = "cart_intent",
                        productName = productName,
                        productId = productId,
                        campaignId = campaignId,
                        notificationTitle = title,
                        notificationBody = body ?: productName,
                        vioUserId = targetUserId,
                        source = source,
                        deeplink = deeplink,
                        activationId = activationId,
                        sponsorId = sponsorId,
                    )
                }.getOrNull()
            }
        }
    }

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
    // No limit on reconnect attempts, just exponential backoff with 30s cap

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

        // Backend keepalive is app-level JSON ping/pong, not WS protocol ping frame.
        if (eventType == "ping") {
            val pong = JsonUtils.stringify(mapOf("type" to "pong"))
            webSocket?.send(pong)
            Log.d(COMPONENT, "[WebSocket] Received ping, sent JSON pong")
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
                    val event = CartIntentEvent.parse(text)
                    if (event != null) {
                        // Dispatch to CampaignManager
                        // This will be handled by the CampaignManager that owns this WebSocketManager
                        onCartIntent?.invoke(event)
                    }
                }
                "component_status_changed" -> {
                    // Notify CampaignManager for activeComponents update
                    onComponentStatusChanged?.invoke(mapper.treeToValue(node, ComponentStatusChangedEvent::class.java))
                }
                "shoppable_ad", "product" -> {
                    Log.d(COMPONENT, "Received $eventType event (mobile doesn't consume)")
                    VioLogger.debug("Received $eventType event (mobile doesn't consume)", COMPONENT)
                }
            }
        } catch (error: Exception) {
            println("[$COMPONENT] Error decoding $eventType: ${error.message}")
            VioLogger.error("Failed to decode $eventType: ${error.message}", COMPONENT)
        }
    }

    private suspend fun attemptReconnect() {
        reconnectAttempts += 1
        val delaySeconds = min(30.0, 2.0.pow(reconnectAttempts.toDouble()))
        Log.i(COMPONENT, "🔄 Reconnection attempt $reconnectAttempts in ${delaySeconds}s")
        VioLogger.info("Reconnecting in ${delaySeconds}s (attempt $reconnectAttempts)", COMPONENT)
        delay((delaySeconds * 1_000).toLong())
        runCatching { connect() }.onFailure {
            println("[$COMPONENT] Reconnect attempt failed: ${it.message}")
            VioLogger.error("Reconnect attempt failed: ${it.message}", COMPONENT)
        }
    }
}