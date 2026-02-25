package com.reachu.tv2demo.services

import com.reachu.tv2demo.services.events.ContestEventData
import com.reachu.tv2demo.services.events.PollEventData
import com.reachu.tv2demo.services.events.PollOption
import com.reachu.tv2demo.services.events.ProductEventData
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject

class WebSocketManager(
    private val url: String = DEFAULT_URL,
    private val client: OkHttpClient = defaultClient,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentPoll = MutableStateFlow<PollEventData?>(null)
    val currentPoll: StateFlow<PollEventData?> = _currentPoll.asStateFlow()

    private val _currentProduct = MutableStateFlow<ProductEventData?>(null)
    val currentProduct: StateFlow<ProductEventData?> = _currentProduct.asStateFlow()

    private val _currentContest = MutableStateFlow<ContestEventData?>(null)
    val currentContest: StateFlow<ContestEventData?> = _currentContest.asStateFlow()

    fun dismissPoll() { _currentPoll.value = null }
    fun dismissProduct() { _currentProduct.value = null }
    fun dismissContest() { _currentContest.value = null }

    fun connect() {
        if (_isConnected.value) return
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(NORMAL_CLOSURE_STATUS, null)
        webSocket = null
        _isConnected.value = false
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _isConnected.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { handleMessage(text) }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _isConnected.value = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _isConnected.value = false
        }
    }

    private fun handleMessage(payload: String) {
        try {
            val root = JSONObject(payload)
            when (root.optString("type")) {
                "product" -> parseProduct(root)?.let { _currentProduct.value = it }
                "poll" -> parsePoll(root)?.let { _currentPoll.value = it }
                "contest" -> parseContest(root)?.let { _currentContest.value = it }
            }
        } catch (_: Exception) {
            // Ignore malformed payloads in the demo environment.
        }
    }

    private fun parseProduct(json: JSONObject): ProductEventData? {
        val data = json.optJSONObject("data") ?: return null
        val campaignLogo = data.optString("campaignLogo")
            .takeIf { it.isNotBlank() }
            ?: json.optString("campaignLogo").takeIf { it.isNotBlank() }

        return ProductEventData(
            id = data.optString("id"),
            productId = data.optString("productId"),
            name = data.optString("name"),
            description = data.optString("description"),
            price = data.optString("price"),
            currency = data.optString("currency"),
            imageUrl = data.optString("imageUrl"),
            campaignLogo = campaignLogo,
        )
    }

    private fun parsePoll(json: JSONObject): PollEventData? {
        val data = json.optJSONObject("data") ?: return null
        val campaignLogo = data.optString("campaignLogo")
            .takeIf { it.isNotBlank() }
            ?: json.optString("campaignLogo").takeIf { it.isNotBlank() }

        val optionsArray = data.optJSONArray("options") ?: JSONArray()
        val options = mutableListOf<PollOption>()
        for (index in 0 until optionsArray.length()) {
            val optionObj = optionsArray.optJSONObject(index) ?: continue
            val avatarUrl = optionObj.optString("avatarUrl")
                .takeIf { it.isNotBlank() }
                ?: optionObj.optString("imageUrl").takeIf { it.isNotBlank() }
            options += PollOption(
                text = optionObj.optString("text"),
                avatarUrl = avatarUrl,
            )
        }

        return PollEventData(
            id = data.optString("id"),
            question = data.optString("question"),
            options = options,
            duration = data.optInt("duration"),
            imageUrl = data.optString("imageUrl").takeIf { it.isNotBlank() },
            campaignLogo = campaignLogo,
        )
    }

    private fun parseContest(json: JSONObject): ContestEventData? {
        val data = json.optJSONObject("data") ?: return null
        val campaignLogo = data.optString("campaignLogo")
            .takeIf { it.isNotBlank() }
            ?: json.optString("campaignLogo").takeIf { it.isNotBlank() }

        return ContestEventData(
            id = data.optString("id"),
            name = data.optString("name"),
            prize = data.optString("prize"),
            deadline = data.optString("deadline"),
            maxParticipants = data.optInt("maxParticipants"),
            campaignLogo = campaignLogo,
        )
    }

    companion object {
        private const val DEFAULT_URL = "wss://event-streamer-angelo100.replit.app/ws/3"
        private const val NORMAL_CLOSURE_STATUS = 1000

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
