package io.reachu.liveshow.chat

import io.reachu.sdk.core.helpers.JsonUtils
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.reachu.liveshow.models.ChatUserRole
import io.reachu.liveshow.models.LiveChatMessage
import io.reachu.liveshow.models.LiveChatUser
import io.reachu.liveshow.models.TipioChatMessage
import io.reachu.liveshow.models.TipioChatMessageData
import io.reachu.liveshow.models.TipioDeletePinnedMessageData

private const val INTERACTIONS_BASE =
    "https://stg-dev-microservices.tipioapp.com/stg-interactions"

/**
 * Kotlin replica of the Swift `LiveChatManager`. Uses StateFlows instead of @Published
 * properties but keeps the public API surface identical.
 */
class LiveChatManager private constructor(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    companion object {
        val shared: LiveChatManager = LiveChatManager()
    }

    private val prefs: Preferences = Preferences.userRoot().node("io.reachu.liveshow.chat")

    private val _messages = MutableStateFlow<List<LiveChatMessage>>(emptyList())
    val messages: StateFlow<List<LiveChatMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentUser = MutableStateFlow(
        LiveChatUser(
            id = "current-user",
            username = "you",
            role = ChatUserRole.VIEWER,
        ),
    )
    val currentUser: StateFlow<LiveChatUser> = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _hasUserName = MutableStateFlow(false)
    val hasUserName: StateFlow<Boolean> = _hasUserName.asStateFlow()

    private val _pinnedMessage = MutableStateFlow<LiveChatMessage?>(null)
    val pinnedMessage: StateFlow<LiveChatMessage?> = _pinnedMessage.asStateFlow()

    private val _channel = MutableStateFlow<String?>(null)
    val channel: StateFlow<String?> = _channel.asStateFlow()

    private val _role = MutableStateFlow("USER")
    val role: StateFlow<String> = _role.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _userName.value = trimmed
        _hasUserName.value = true
        _currentUser.value = _currentUser.value.copy(username = trimmed)
        println("👤 [Chat] User name set: $trimmed")
    }

    fun clearUserName() {
        _userName.value = ""
        _hasUserName.value = false
        println("👤 [Chat] User name cleared")
    }

    fun configure(channel: String, role: String = "USER") {
        _channel.value = channel
        _role.value = role
    }

    fun connect() {
        _isConnected.value = true
        println("🔌 [Chat] Connected to live chat")
    }

    fun disconnect() {
        _isConnected.value = false
        println("🔌 [Chat] Disconnected from live chat")
    }

    fun clearMessages() {
        _messages.value = emptyList()
        println("🗑️ [Chat] Messages cleared")
    }

    fun addMessage(message: LiveChatMessage) {
        _messages.value = _messages.value + message
    }

    fun sendMessage(
        text: String,
        pinned: Boolean = false,
        father: Map<String, Any?>? = null,
    ) {
        if (!_hasUserName.value) {
            println("❌ [Chat] Cannot send message without user name")
            return
        }
        val channelId = _channel.value
        if (channelId.isNullOrBlank()) {
            println("⚠️ [Chat] Channel is not configured. Use configure() before sending.")
            val fallback = LiveChatMessage(
                user = _currentUser.value,
                message = text,
                isStreamerMessage = false,
                isPinned = pinned,
            )
            _messages.value = _messages.value + fallback
            return
        }

        val now = Instant.now()
        val clientId = getOrCreateClientId()
        val connectionId = "chat-$channelId"
        val uuid = UUID.randomUUID().toString()

        val dataBlock = mutableMapOf<String, Any?>(
            "type" to "chatMessage",
            "text" to text,
            "user" to _userName.value,
            "clientId" to clientId,
            "role" to _role.value,
            "pinned" to pinned,
            "userTime" to iso8601(now),
            "father" to father,
            "replies" to emptyList<Any>(),
        )
        if (father == null) dataBlock["father"] = null

        val chatBlock = mapOf(
            "clientId" to clientId,
            "connectionId" to connectionId,
            "data" to dataBlock,
            "encoding" to null,
            "messageid" to iso8601(now),
            "name" to _userName.value,
        )

        val payload = mapOf(
            "type" to "chatMessage",
            "text" to text,
            "user" to _userName.value,
            "clientId" to clientId,
            "role" to _role.value,
            "pinned" to pinned,
            "userTime" to iso8601(now),
            "father" to father,
            "replies" to emptyList<Any>(),
            "servicesData" to mapOf(
                "liveStreamId" to channelId,
                "uuid" to uuid,
                "chat" to chatBlock,
            ),
        )

        scope.launch {
            val ok = postChatMessage(payload)
            if (ok) {
                resendPendingMessagesIfAny()
            } else {
                addPendingMessage(payload, channelId)
            }
        }
    }

    suspend fun loadChatMessages(channel: String, migrated: Boolean = false) {
        val suffix = if (migrated) "?migratedChats=true" else ""
        val url = "$INTERACTIONS_BASE/chat/by-channel/chat-$channel$suffix"
        println(
            if (migrated) "🔄 [Chat] Loading migrated messages for channel $channel"
            else "🔄 [Chat] Loading messages for channel $channel",
        )
        println("🔗 [Chat] URL: $url")
        runCatching {
            withContext(ioDispatcher) {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    error("❌ [Chat] API error: $status")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                @Suppress("UNCHECKED_CAST")
                val messages = JsonUtils.mapper.readValue(
                    body,
                    JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, TipioChatMessage::class.java),
                ) as List<TipioChatMessage>
                val liveMessages: List<LiveChatMessage> = messages.map { it.toLiveChatMessage() }
                val pinned = liveMessages.lastOrNull { it.isPinned }
                _pinnedMessage.value = pinned

                val normalMessages = liveMessages.filterNot { it.isPinned }.sortedBy { it.timestamp }
                _messages.value = normalMessages
                println("📥 [Chat] Loaded ${normalMessages.size} normal messages from API")
            }
        }.onFailure {
            println("❌ [Chat] Failed to load messages: ${it.message}")
        }
    }

    fun processIncomingMessage(message: TipioChatMessageData) {
        val liveMessage = message.toLiveChatMessage()
        if (isDuplicate(liveMessage)) {
            println("⚠️ [Chat] Duplicate message ignored from ${liveMessage.user.username}")
            return
        }
        if (liveMessage.isPinned) {
            _pinnedMessage.value = liveMessage
            println("📌 [Chat] Pinned message updated: ${liveMessage.message}")
        } else {
            val updated = (_messages.value + liveMessage).takeLast(100)
            _messages.value = updated
        }
        println("💬 [Chat] Added incoming message from ${liveMessage.user.username}: ${liveMessage.message} ${liveMessage.isPinned}")
    }

    fun processDeletePinnedMessage(deleteData: TipioDeletePinnedMessageData) {
        val current = _pinnedMessage.value ?: return
        val isSameUser = current.user.id == deleteData.message.clientId
        val delta = kotlin.math.abs(
            java.time.Duration.between(current.timestamp, deleteData.message.messageid).seconds,
        )
        if (isSameUser && delta < 1) {
            _pinnedMessage.value = null
            println("🗑️ [Chat] Pinned message removed: ${current.message}")
        }
    }

    private fun isDuplicate(message: LiveChatMessage): Boolean {
        return _messages.value.any {
            it.user.id == message.user.id &&
                kotlin.math.abs(java.time.Duration.between(it.timestamp, message.timestamp).seconds) < 1 &&
                it.message == message.message
        }
    }

    private suspend fun postChatMessage(payload: Map<String, Any?>): Boolean {
        val url = "$INTERACTIONS_BASE/chat/send"
        return withContext(ioDispatcher) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val body = JsonUtils.stringify(payload)
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code in 200..299) {
                    true
                } else {
                    println("❌ [Chat] POST /chat/send failed status=$code")
                    false
                }
            }.getOrElse {
                println("❌ [Chat] POST /chat/send error: ${it.message}")
                false
            }
        }
    }

    private fun getOrCreateClientId(): String {
        val key = "pubnub_uuid"
        val current = prefs.get(key, null)
        if (!current.isNullOrBlank()) return current
        val newId = UUID.randomUUID().toString()
        prefs.put(key, newId)
        return newId
    }

    private fun pendingKey(channel: String) = "pendingMessages_$channel"

    private fun addPendingMessage(message: Map<String, Any?>, channel: String) {
        val key = pendingKey(channel)
        val array = getPendingMessages(channel).toMutableList()
        array.add(JsonUtils.stringify(message))
        prefs.put(key, JsonUtils.stringify(array))
        println("⏳ [Chat] Stored pending message (count=${array.size}) for channel $channel")
    }

    private fun getPendingMessages(channel: String): List<String> {
        val raw = prefs.get(pendingKey(channel), "[]")
        return runCatching<List<String>> {
            @Suppress("UNCHECKED_CAST")
            JsonUtils.mapper.readValue(
                raw,
                JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, String::class.java),
            ) as List<String>
        }.getOrDefault(emptyList())
    }

    private fun setPendingMessages(channel: String, messages: List<String>) {
        prefs.put(pendingKey(channel), JsonUtils.stringify(messages))
    }

    private suspend fun resendPendingMessagesIfAny() {
        val channelId = _channel.value ?: return
        val pending = getPendingMessages(channelId)
        if (pending.isEmpty()) return
        val remaining = mutableListOf<String>()
        pending.forEach { json ->
            val payload = JsonUtils.parseMap(json)
            val success = postChatMessage(payload)
            if (!success) remaining.add(json)
        }
        setPendingMessages(channelId, remaining)
        println("🔁 [Chat] Resent pending messages. Remaining: ${remaining.count()}")
    }

    private fun iso8601(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
}

// Demo fixtures kept for parity with Swift helpers --------------------------------

object DemoChatData {
    val demoUsers: List<LiveChatUser> = listOf(
        LiveChatUser(
            id = "user1",
            username = "@livehost",
            avatarUrl = "https://picsum.photos/50/50?random=1",
            isVerified = true,
            isModerator = true,
            role = ChatUserRole.STREAMER,
        ),
        LiveChatUser(
            id = "user2",
            username = "fashionlover23",
            avatarUrl = "https://picsum.photos/50/50?random=2",
        ),
        LiveChatUser(
            id = "user3",
            username = "styleinspo",
            avatarUrl = "https://picsum.photos/50/50?random=3",
            isVerified = true,
            role = ChatUserRole.SUBSCRIBER,
        ),
        LiveChatUser(
            id = "user4",
            username = "shoppingqueen",
            avatarUrl = "https://picsum.photos/50/50?random=4",
        ),
        LiveChatUser(
            id = "user5",
            username = "trendwatcher",
            avatarUrl = "https://picsum.photos/50/50?random=5",
        ),
        LiveChatUser(
            id = "user6",
            username = "casual_chic",
            avatarUrl = "https://picsum.photos/50/50?random=6",
        ),
    )

    val initialMessages: List<LiveChatMessage> = listOf(
        LiveChatMessage(
            user = demoUsers[0],
            message = "Welcome to our live beauty show! 💄✨",
            isStreamerMessage = true,
        ),
        LiveChatMessage(
            user = demoUsers[1],
            message = "Can you show it in black?",
        ),
        LiveChatMessage(
            user = demoUsers[2],
            message = "Just ordered! Can't wait ❤️",
        ),
        LiveChatMessage(
            user = demoUsers[3],
            message = "This would look great with jeans",
        ),
        LiveChatMessage(
            user = demoUsers[4],
            message = "So stylish! 👏",
        ),
        LiveChatMessage(
            user = demoUsers[5],
            message = "Adding to cart now!",
        ),
    )

    val randomMessages: List<String> = listOf(
        "Love this! 😍",
        "Where can I buy this?",
        "What size would you recommend?",
        "This looks amazing! 🤩",
        "Perfect for summer!",
        "Already in my cart! 🛒",
        "Can you show the back?",
        "What's the material?",
        "Shipping to Europe?",
        "Is there a discount code?",
        "This color is gorgeous! 💕",
        "Just what I was looking for!",
        "How's the fit?",
        "Ordering right now! 🎉",
        "Can you model it?",
        "What other colors available?",
        "Price looks good! 💰",
        "Adding to wishlist ⭐",
        "Perfect timing! 🕐",
        "Looks premium quality 👌",
    )

    val responseMessages: List<String> = listOf(
        "Thanks for joining! 🙌",
        "Check the product details below! 👇",
        "Limited time offer! ⏰",
        "Great choice! 👍",
        "You'll love it! 💕",
        "Don't miss out! 🚀",
        "Perfect for any occasion! ✨",
        "High quality guaranteed! 🏆",
        "Ships worldwide! 🌍",
        "Use code LIVE20 for 20% off! 🎁",
    )
}
