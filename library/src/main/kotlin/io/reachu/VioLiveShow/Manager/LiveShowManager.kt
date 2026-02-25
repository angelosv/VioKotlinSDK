package io.reachu.liveshow

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.sdk.core.helpers.JsonUtils
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.reachu.liveshow.chat.LiveChatManager
import io.reachu.liveshow.models.LiveChatMessage
import io.reachu.liveshow.models.LiveChatUser
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.LiveStreamLayout
import io.reachu.liveshow.models.LiveStreamSocketEvent
import io.reachu.liveshow.models.MiniPlayerPosition
import io.reachu.liveshow.models.TipioChatMessageData
import io.reachu.liveshow.models.TipioComponentData
import io.reachu.liveshow.models.TipioDeletePinnedMessageData
import io.reachu.liveshow.models.TipioEvent
import io.reachu.liveshow.models.TipioProductHighlightData
import io.reachu.liveshow.models.TipioStreamLifecycleData
import io.reachu.liveshow.models.TipioStreamStatusData
import io.reachu.liveshow.models.TipioViewerCountData
import io.reachu.liveshow.models.asProduct
import io.reachu.liveshow.models.toLiveStream
import io.reachu.liveshow.network.TipioApiClient
import io.reachu.liveshow.network.TipioWebSocketClient

/**
 * Kotlin singleton equivalent to the Swift `@MainActor LiveShowManager`.
 */
class LiveShowManager private constructor(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    companion object {
        val shared: LiveShowManager = LiveShowManager()
    }

    private val configuration = VioConfiguration.shared.state.value.liveShow
    private val prefs: Preferences = Preferences.userRoot().node("io.reachu.liveshow.manager")
    private val heartApiBase = "https://stg-dev-microservices.tipioapp.com/stg-hearts"
    private val userTrackingKey = "reachu.userTrackingId"
    private val tipioIdToLiveStreamId = ConcurrentHashMap<Int, String>()

    private val tipioApiClient = TipioApiClient(VioConfiguration.shared.state.value)
    private val tipioWebSocketClient = TipioWebSocketClient(
        baseUrl = "https://stg-dev-microservices.tipioapp.com/stg-stream",
    )

    private val _isLiveShowVisible = MutableStateFlow(false)
    val isLiveShowVisible: StateFlow<Boolean> = _isLiveShowVisible.asStateFlow()

    private val _currentStream = MutableStateFlow<LiveStream?>(null)
    val currentStream: StateFlow<LiveStream?> = _currentStream.asStateFlow()

    private val _layout = MutableStateFlow(LiveStreamLayout.FULL_SCREEN_OVERLAY)
    val layout: StateFlow<LiveStreamLayout> = _layout.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _miniPlayerPosition = MutableStateFlow(MiniPlayerPosition.BOTTOM_RIGHT)
    val miniPlayerPosition: StateFlow<MiniPlayerPosition> = _miniPlayerPosition.asStateFlow()

    private val _isIndicatorVisible = MutableStateFlow(true)
    val isIndicatorVisible: StateFlow<Boolean> = _isIndicatorVisible.asStateFlow()

    private val _activeStreams = MutableStateFlow<List<LiveStream>>(emptyList())
    val activeStreams: StateFlow<List<LiveStream>> = _activeStreams.asStateFlow()
    val featuredLiveStream: LiveStream?
        get() = _activeStreams.value.firstOrNull { it.isLive } ?: _activeStreams.value.firstOrNull()

    private val _isConnectedToTipio = MutableStateFlow(false)
    val isConnectedToTipio: StateFlow<Boolean> = _isConnectedToTipio.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _currentViewerCount = MutableStateFlow(0)
    val currentViewerCount: StateFlow<Int> = _currentViewerCount.asStateFlow()
    val heartEvents: SharedFlow<Unit> = tipioWebSocketClient.heartEvents

    init {
        setupTipioIntegration()
        scope.launch {
            fetchActiveTipioStreams()
        }
    }

    fun updateCurrentStream(stream: LiveStream) {
        _currentStream.value = stream
        LiveChatManager.shared.configure(channel = stream.id, role = "USER")
        scope.launch {
            LiveChatManager.shared.loadChatMessages(channel = stream.id, migrated = !stream.isLive)
        }
    }

    fun showLiveStream(stream: LiveStream, layout: LiveStreamLayout = LiveStreamLayout.FULL_SCREEN_OVERLAY) {
        _currentStream.value = stream
        _layout.value = layout
        _isLiveShowVisible.value = true
        _isMiniPlayerVisible.value = false
        LiveChatManager.shared.configure(channel = stream.id, role = "USER")
        scope.launch {
            LiveChatManager.shared.loadChatMessages(channel = stream.id, migrated = !stream.isLive)
        }
    }

    fun showLiveStream(id: String, layout: LiveStreamLayout = LiveStreamLayout.FULL_SCREEN_OVERLAY) {
        val stream = _activeStreams.value.firstOrNull { it.id == id } ?: return
        showLiveStream(stream, layout)
    }

    fun hideLiveStream() {
        _isLiveShowVisible.value = false
        _isMiniPlayerVisible.value = false
        _currentStream.value = null
    }

    fun showMiniPlayer() {
        if (_currentStream.value == null) return
        _isLiveShowVisible.value = false
        _isMiniPlayerVisible.value = true
    }

    fun expandFromMiniPlayer() {
        if (_currentStream.value == null) return
        _isMiniPlayerVisible.value = false
        _isLiveShowVisible.value = true
    }

    fun toggleIndicator() {
        _isIndicatorVisible.value = !_isIndicatorVisible.value
    }

    fun hideIndicator() {
        _isIndicatorVisible.value = false
    }

    fun showIndicator() {
        _isIndicatorVisible.value = true
    }

    fun addProductToCart(liveProduct: LiveProduct, cartManager: LiveShowCartManaging) {
        scope.launch {
            cartManager.addProduct(liveProduct.asProduct, quantity = 1)
        }
    }

    fun quickBuyProduct(liveProduct: LiveProduct, cartManager: LiveShowCartManaging) {
        addProductToCart(liveProduct, cartManager)
        cartManager.showCheckout()
    }

    val hasActiveLiveStreams: Boolean
        get() = _activeStreams.value.any { it.isLive }

    val totalViewerCount: Int
        get() = _activeStreams.value.sumOf { it.viewerCount }

    val isWatchingLiveStream: Boolean
        get() = _isLiveShowVisible.value || _isMiniPlayerVisible.value

    fun connectToTipio() {
        println("🔌 [LiveShow] Connecting to Tipio...")
        tipioWebSocketClient.connect()
    }

    fun disconnectFromTipio() {
        println("🔌 [LiveShow] Disconnecting from Tipio...")
        tipioWebSocketClient.disconnect()
    }

    suspend fun fetchTipioLiveStream(id: Int) {
        runCatching {
            println("📡 [LiveShow] Fetching Tipio livestream: $id")
            val tipioStream = tipioApiClient.getLiveStream(id)
            tipioIdToLiveStreamId[tipioStream.id] = tipioStream.liveStreamId
            val liveStream = tipioStream.toLiveStream()
            updateActiveStream(liveStream)
            tipioWebSocketClient.subscribeToStream(id)
        }.onFailure {
            println("❌ [LiveShow] Failed to fetch Tipio livestream: ${it.message}")
        }
    }

    suspend fun fetchActiveTipioStreams() {
        runCatching {
            println("📡 [LiveShow] Fetching active Tipio livestreams")
            val tipioStreams = tipioApiClient.getActiveLiveStreams()
            tipioStreams.forEach { tipioIdToLiveStreamId[it.id] = it.liveStreamId }
            val liveStreams = tipioStreams.map { it.toLiveStream() }
            _activeStreams.value = liveStreams
            tipioStreams.forEach { tipioWebSocketClient.subscribeToStream(it.id) }
            println("✅ [LiveShow] Successfully fetched ${liveStreams.size} active streams from Tipio")
        }.onFailure {
            println("❌ [LiveShow] Failed to fetch active Tipio livestreams: ${it.message}")
        }
    }

    val userTrackingId: String
        get() {
            val existing = prefs.get(userTrackingKey, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = "ios-" + UUID.randomUUID().toString()
            prefs.put(userTrackingKey, generated)
            return generated
        }

    fun sendHeartForCurrentStream(isVideoLive: Boolean) {
        val current = _currentStream.value ?: run {
            println("❌ [LiveShow] sendHeart: no current stream")
            return
        }
        val tipioInt = current.id.toIntOrNull() ?: run {
            println("❌ [LiveShow] sendHeart: invalid stream id ${current.id}")
            return
        }
        val liveStreamId = tipioIdToLiveStreamId[tipioInt] ?: run {
            println("❌ [LiveShow] sendHeart: missing mapping for $tipioInt")
            return
        }
        val clientId = userTrackingId
        val emojiChannel = "emojiChannel-$liveStreamId"
        val hasHeartedKey = "reachu.hasHearted.$liveStreamId"
        val hasHeartedBefore = prefs.getBoolean(hasHeartedKey, false)

        val url = when {
            isVideoLive && hasHeartedBefore -> "$heartApiBase/heart/socket-sdk/$liveStreamId"
            isVideoLive -> "$heartApiBase/heart/livestream/$liveStreamId?origin=sdk"
            else -> "$heartApiBase/heart/livestream/$liveStreamId/after-show?origin=sdk"
        }
        println("🚀 [LiveShow] Sending HEART to $url")

        scope.launch(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val body = JsonUtils.stringify(
                    mapOf(
                        "channel" to emojiChannel,
                        "clientId" to clientId,
                    ),
                )
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                println("❤️ [LiveShow] HEART status=$status")
                if (status in 200..299 && !hasHeartedBefore) {
                    prefs.putBoolean(hasHeartedKey, true)
                }
            }.onFailure {
                println("❌ [LiveShow] HEART request error: ${it.message}")
            }
        }
    }

    suspend fun startTipioLiveStream(id: Int) {
        runCatching {
            println("🚀 [LiveShow] Starting Tipio livestream: $id")
            val status = tipioApiClient.startLiveStream(id)
            println("✅ [LiveShow] Successfully started livestream: ${status.status}")
            fetchTipioLiveStream(id)
        }.onFailure {
            println("❌ [LiveShow] Failed to start Tipio livestream: ${it.message}")
        }
    }

    suspend fun stopTipioLiveStream(id: Int) {
        runCatching {
            println("⏹️ [LiveShow] Stopping Tipio livestream: $id")
            val status = tipioApiClient.stopLiveStream(id)
            println("✅ [LiveShow] Successfully stopped livestream: ${status.status}")
            fetchTipioLiveStream(id)
        }.onFailure {
            println("❌ [LiveShow] Failed to stop Tipio livestream: ${it.message}")
        }
    }

    private fun setupTipioIntegration() {
        scope.launch {
            tipioWebSocketClient.connectionStatus.collect { status ->
                _isConnectedToTipio.value = status is TipioWebSocketClient.ConnectionStatus.Connected
                _connectionStatus.value = status.displayName
            }
        }
        scope.launch {
            tipioWebSocketClient.eventFlow.collect { event ->
                handleTipioEvent(event)
            }
        }
        scope.launch {
            tipioWebSocketClient.liveEventFlow.collect { liveEvent ->
                when (liveEvent) {
                    is LiveStreamSocketEvent.Started -> updateActiveStream(liveEvent.stream)
                    is LiveStreamSocketEvent.Ended -> updateActiveStream(liveEvent.stream)
                }
            }
        }
        tipioWebSocketClient.connect()
        println("🔧 [LiveShow] Tipio integration setup completed")
    }

    private fun handleTipioEvent(event: TipioEvent) {
        println("📡 [LiveShow] Handling Tipio event: ${event.type} for stream ${event.streamId}")
        when (val data = event.data) {
            is TipioStreamStatusData -> handleStreamStatusUpdate(event.streamId, data)
            is TipioChatMessageData -> handleChatMessage(event.streamId, data)
            is TipioViewerCountData -> handleViewerCountUpdate(event.streamId, data)
            is TipioProductHighlightData -> handleProductHighlight(event.streamId, data)
            is TipioComponentData -> handleComponentEvent(event.streamId, data)
            is TipioDeletePinnedMessageData -> handleDeletePinnedMessage(event.streamId, data)
            is TipioStreamLifecycleData -> updateActiveStream(data.stream)
            else -> println("⚠️ [LiveShow] Unknown TipioEvent data type: $data")
        }
    }

    private fun handleStreamStatusUpdate(streamId: Int, status: TipioStreamStatusData) {
        val index = _activeStreams.value.indexOfFirst { it.id == streamId.toString() }
        if (index == -1) {
            println("⚠️ [LiveShow] Stream not found for status update: $streamId")
            return
        }
        val hlsUrl = status.hlsUrl?.takeIf { it.isNotBlank() } ?: return
        val current = _activeStreams.value[index]
        val updated = current.copy(
            videoUrl = hlsUrl,
            isLive = status.broadcasting,
        )
        updateActiveStream(updated)
        println("✅ [LiveShow] Updated stream status for: $streamId")
    }

    private fun handleChatMessage(streamId: Int, chatData: TipioChatMessageData) {
        LiveChatManager.shared.processIncomingMessage(chatData)
        val index = _activeStreams.value.indexOfFirst { it.id == streamId.toString() }
        if (index == -1) return
        val updatedMessages = (_activeStreams.value[index].chatMessages + chatData.toLiveChatMessage()).takeLast(100)
        val updatedStream = _activeStreams.value[index].copy(chatMessages = updatedMessages)
        updateActiveStream(updatedStream)
        println("💬 [LiveShow] New chat message in stream $streamId: ${chatData.message}")
    }

    private fun handleDeletePinnedMessage(streamId: Int, deleteData: TipioDeletePinnedMessageData) {
        LiveChatManager.shared.processDeletePinnedMessage(deleteData)
        println("🗑️ [LiveShow] Delete pinned message event for stream $streamId")
    }

    private fun handleViewerCountUpdate(streamId: Int, viewerData: TipioViewerCountData) {
        val index = _activeStreams.value.indexOfFirst { it.id == streamId.toString() }
        if (index == -1) return
        val updatedStream = _activeStreams.value[index].copy(viewerCount = viewerData.count)
        updateActiveStream(updatedStream)
        if (_currentStream.value?.id == updatedStream.id) {
            _currentViewerCount.value = viewerData.count
        }
        println("👥 [LiveShow] Viewer count updated for stream $streamId: ${viewerData.count}")
    }

    private fun handleProductHighlight(streamId: Int, data: TipioProductHighlightData) {
        println("🛍️ [LiveShow] Product highlighted in stream $streamId: ${data.productId}")
    }

    private fun handleComponentEvent(streamId: Int, data: TipioComponentData) {
        println("🧩 [LiveShow] Component event in stream $streamId: ${data.type} - Active: ${data.active}")
    }

    private fun updateActiveStream(stream: LiveStream) {
        val currentList = _activeStreams.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == stream.id }
        if (index >= 0) {
            currentList[index] = stream
        } else {
            currentList.add(stream)
        }
        _activeStreams.value = currentList
        if (_currentStream.value?.id == stream.id) {
            _currentStream.value = stream
        }
    }

    fun simulateNewChatMessage() {
        val current = _currentStream.value ?: return
        val newMessage = LiveChatMessage(
            user = LiveChatUser(id = "user_new", username = "live_viewer"),
            message = listOf("Amazing!", "Love it!", "Want this! 😍", "How much?", "Gorgeous! ✨").random(),
        )
        val updated = current.copy(
            viewerCount = current.viewerCount + (1..5).random(),
            chatMessages = listOf(newMessage) + current.chatMessages.take(20),
        )
        _currentStream.value = updated
        updateActiveStream(updated)
    }
}
