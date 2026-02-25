package io.reachu.liveshow.network

import io.reachu.liveshow.models.LiveStreamSocketEvent
import io.reachu.liveshow.models.TipioChatMessage
import io.reachu.liveshow.models.TipioEvent
import io.reachu.liveshow.models.TipioEventDecoder
import io.reachu.liveshow.models.TipioEventType
import io.reachu.liveshow.models.TipioLiveStream
import io.reachu.liveshow.models.toLiveStream
import io.reachu.sdk.core.helpers.JsonUtils
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Socket.IO based client mirroring the Swift implementation.
 */
private const val EVENT_ERROR = "error"
private const val EVENT_RECONNECT = "reconnect"
private const val EVENT_RECONNECT_ATTEMPT = "reconnect_attempt"

class TipioWebSocketClient(
    baseUrl: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    private val originalBase = baseUrl
    private val socketConfig = buildSocketConfig(baseUrl)
    private var socket: Socket? = null
    private val subscribedStreams = ConcurrentHashMap.newKeySet<Int>()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _eventFlow = MutableSharedFlow<TipioEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val eventFlow: SharedFlow<TipioEvent> = _eventFlow.asSharedFlow()

    private val _liveEventFlow = MutableSharedFlow<LiveStreamSocketEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val liveEventFlow: SharedFlow<LiveStreamSocketEvent> = _liveEventFlow.asSharedFlow()

    private val _heartEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val heartEvents: SharedFlow<Unit> = _heartEvents.asSharedFlow()

    fun connect() {
        if (_connectionStatus.value == ConnectionStatus.Connecting ||
            _connectionStatus.value == ConnectionStatus.Connected
        ) return

        val config = socketConfig ?: run {
            println("❌ [TipioWS] Invalid Socket.IO URL: $originalBase")
            updateStatus(ConnectionStatus.Error("Invalid Socket.IO URL"))
            return
        }

        updateStatus(ConnectionStatus.Connecting)

        val options = IO.Options.builder()
            .setReconnection(true)
            .setForceNew(false)
            .build().apply {
                path = config.path
            }

        println("🔌 [TipioWS] Connecting (Socket.IO) to: ${config.origin}, path: ${config.path}")

        val client = IO.socket(config.origin, options)
        socket = client

        client.on(Socket.EVENT_CONNECT) {
            println("✅ [TipioWS] Socket.IO connected")
            updateStatus(ConnectionStatus.Connected)
            subscribedStreams.forEach { subscribeToStream(it) }
        }

        client.on(EVENT_ERROR) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            println("❌ [TipioWS] Socket.IO error: $reason")
            updateStatus(ConnectionStatus.Error(reason))
        }

        client.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            println("❌ [TipioWS] Socket.IO connect error: $reason")
            updateStatus(ConnectionStatus.Error(reason))
        }

        client.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            println("🔌 [TipioWS] Socket.IO disconnected: $reason")
            updateStatus(ConnectionStatus.Disconnected)
        }

        client.on(EVENT_RECONNECT) {
            println("🔄 [TipioWS] Socket.IO reconnect")
            updateStatus(ConnectionStatus.Reconnecting)
        }

        client.on(EVENT_RECONNECT_ATTEMPT) { args ->
            val attempt = args.firstOrNull()?.toString() ?: "unknown"
            println("🔄 [TipioWS] Socket.IO reconnect attempt: $attempt")
            updateStatus(ConnectionStatus.Reconnecting)
        }

        registerDomainHandlers(client)
        client.connect()
    }

    fun disconnect() {
        println("🔌 [TipioWS] Disconnecting...")
        socket?.disconnect()
        socket?.off()
        socket = null
        updateStatus(ConnectionStatus.Disconnected)
    }

    fun subscribeToStream(streamId: Int) {
        subscribedStreams.add(streamId)
        val payload = mapOf(
            "stream_id" to streamId,
            "timestamp" to Instant.now().toString(),
        )
        socket?.emit("subscribe", JSONObject(payload))
        println("📺 [TipioWS] Subscribed to stream: $streamId")
    }

    fun unsubscribeFromStream(streamId: Int) {
        subscribedStreams.remove(streamId)
        val payload = mapOf(
            "stream_id" to streamId,
            "timestamp" to Instant.now().toString(),
        )
        socket?.emit("unsubscribe", JSONObject(payload))
        println("📺 [TipioWS] Unsubscribed from stream: $streamId")
    }

    private fun registerDomainHandlers(client: Socket) {
        client.on("live-event-started") { args ->
            handleLiveEvent(args, isStarting = true)
        }
        client.on("live-event-ended") { args ->
            handleLiveEvent(args, isStarting = false)
        }
        client.on("tipio-event") { args ->
            dispatchTipioEvent(args, fallbackStreamId = null)
        }
        listOf("stream-status", "chat-message", "viewer-count", "product-highlight", "component").forEach { name ->
            client.on(name) { args -> dispatchTipioEvent(args, fallbackStreamId = null) }
        }
        client.on("HEART") {
            println("🟢 [TipioWS] HEART event received")
            _heartEvents.tryEmit(Unit)
        }
        client.on("CHAT") { args ->
            val payload = extractPayload(args.firstOrNull()) ?: return@on
            val json = payload.toJsonString() ?: return@on
            runCatching {
                val message = JsonUtils.mapper.readValue(json, TipioChatMessage::class.java)
                val event = TipioEvent(
                    type = TipioEventType.CHAT_MESSAGE,
                    streamId = 0,
                    data = message.toRealtimeData(),
                )
                _eventFlow.tryEmit(event)
                println("📡 [TipioWS] Chat message received: ${message.text}")
            }.onFailure {
                println("❌ [TipioWS] Failed to decode CHAT payload: ${it.message}")
            }
        }
        client.on("DELETE-PINNED-MESSAGE") { args ->
            dispatchTipioEvent(args, fallbackStreamId = null)
        }
    }

    private fun dispatchTipioEvent(args: Array<Any>, fallbackStreamId: Int?) {
        val payload = extractPayload(args.firstOrNull()) ?: return
        val json = payload.toJsonString() ?: return
        val event = TipioEventDecoder.decode(json) ?: return
        val resolved = if (event.streamId == 0 && fallbackStreamId != null) {
            event.copy(streamId = fallbackStreamId)
        } else event
        _eventFlow.tryEmit(resolved)
        println("📡 [TipioWS] Event ${resolved.type} (stream=${resolved.streamId})")
    }

    private fun handleLiveEvent(args: Array<Any>, isStarting: Boolean) {
        val payload = extractPayload(args.firstOrNull()) ?: return
        val json = payload.toJsonString() ?: return
        runCatching {
            val stream = JsonUtils.mapper.readValue(json, TipioLiveStream::class.java).toLiveStream()
            val event = if (isStarting) {
                LiveStreamSocketEvent.Started(stream)
            } else {
                LiveStreamSocketEvent.Ended(stream)
            }
            _liveEventFlow.tryEmit(event)
            val label = if (isStarting) "started" else "ended"
            println("📺 [TipioWS] Live event $label: ${stream.id}")
        }.onFailure {
            println("❌ [TipioWS] Failed to decode live event payload: ${it.message}")
        }
    }

    private fun extractPayload(raw: Any?): Any? = when (raw) {
        is JSONObject -> if (raw.has("payload")) raw.opt("payload") else raw
        is Map<*, *> -> raw["payload"] ?: raw
        is JSONArray -> if (raw.length() > 0) raw.get(0) else null
        else -> raw
    }

    private fun Any?.toJsonString(): String? = when (this) {
        null -> null
        is JSONObject -> this.toString()
        is JSONArray -> this.toString()
        is String -> this
        is Map<*, *> -> JsonUtils.stringify(this)
        else -> JsonUtils.stringify(this)
    }

    private fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
        println("🔌 [TipioWS] Connection status: ${status.displayName}")
    }

    private data class SocketConfig(val origin: URI, val path: String)

    private fun buildSocketConfig(base: String): SocketConfig? {
        val sanitized = sanitizeBase(base)
        val uri = runCatching { URI.create(sanitized) }.getOrElse { return null }
        if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null

        val origin = URI(
            uri.scheme,
            uri.userInfo,
            uri.host,
            uri.port,
            null,
            null,
            null,
        )
        val customPath = uri.path
            ?.takeIf { it.isNotBlank() && it != "/" }
            ?.trimEnd('/')
            ?: ""
        val socketPath = "${customPath}/socket.io"
        val normalizedPath = if (socketPath.startsWith("/")) socketPath else "/$socketPath"
        return SocketConfig(origin = origin, path = normalizedPath)
    }

    private fun sanitizeBase(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("wss://", ignoreCase = true) -> "https://" + trimmed.substring(6)
            trimmed.startsWith("ws://", ignoreCase = true) -> "http://" + trimmed.substring(5)
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    sealed class ConnectionStatus(val displayName: String) {
        object Disconnected : ConnectionStatus("Disconnected")
        object Connecting : ConnectionStatus("Connecting")
        object Connected : ConnectionStatus("Connected")
        object Reconnecting : ConnectionStatus("Reconnecting")
        data class Error(val reason: String) : ConnectionStatus("Error: $reason")
    }
}
