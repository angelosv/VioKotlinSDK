package io.reachu.liveshow.models

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import io.reachu.VioCore.models.Price
import io.reachu.VioCore.models.Product
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Suppress("UNUSED_PARAMETER")
private val jacksonBootstrap = LiveShowJacksonConfigurator

// MARK: Tipio API models ------------------------------------------------------

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioLiveStream(
    val id: Int,
    val title: String,
    val liveStreamId: String,
    val hls: String? = null,
    val player: String? = null,
    val thumbnail: String? = null,
    val broadcasting: Boolean,
    @Serializable(with = InstantIso8601Serializer::class)
    val date: Instant,
    @JsonAlias("end_date")
    @Serializable(with = InstantIso8601Serializer::class)
    val endDate: Instant,
    val streamDone: Boolean? = null,
    val videoId: String? = null,
    val videoUrl: String? = null,
)

fun TipioLiveStream.toLiveStream(
    streamerOverride: LiveStreamer? = null,
    featuredProducts: List<LiveProduct> = emptyList(),
    chatMessages: List<LiveChatMessage> = emptyList(),
): LiveStream {
    val name = extractStreamerName(title) ?: "Live Host"
    val username = generateUsername(name)
    val streamer = streamerOverride ?: LiveStreamer(
        id = "tipio-$id",
        name = name,
        username = username,
        avatarUrl = thumbnail,
        isVerified = true,
        followerCount = (100..5_000).random(),
    )

    return LiveStream(
        id = id.toString(),
        title = title,
        streamer = streamer,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnail,
        viewerCount = 0,
        isLive = broadcasting,
        startTime = date,
        endTime = endDate,
        featuredProducts = featuredProducts,
        chatMessages = chatMessages,
    )
}

private fun extractStreamerName(title: String): String? {
    val patterns = listOf(
        "Live with ([A-Za-z\\s]+)",
        "Live: ([A-Za-z\\s]+)",
        "Live - ([A-Za-z\\s]+)",
        "Live \\| ([A-Za-z\\s]+)",
        "([A-Za-z\\s]+) Live",
        "([A-Za-z\\s]+)'s Live",
        "Hosted by ([A-Za-z\\s]+)",
        "([A-Za-z\\s]+) presents",
    )
    patterns.forEach { regex ->
        val matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(title)
        if (matcher.find()) {
            val name = matcher.group(1)?.trim()
            if (!name.isNullOrBlank() && name.length > 2) return name
        }
    }

    val clean = title.trim()
    if (clean.isEmpty()) return null
    val pieces = clean.split(" ")
    return if (pieces.size == 1) "${pieces.first().replaceFirstChar { it.uppercase(Locale.getDefault()) }} Host"
    else "${pieces.first().replaceFirstChar { it.uppercase(Locale.getDefault()) }} Host"
}

private fun generateUsername(name: String): String {
    val normalized = name.lowercase(Locale.getDefault())
        .replace(" ", "")
        .replace(Regex("[^a-z0-9]"), "")
    val suffix = (10..99).random()
    return "@$normalized$suffix"
}

// MARK: WebSocket events ------------------------------------------------------

enum class TipioEventType(val wireName: String) {
    STREAM_STARTED("stream_started"),
    STREAM_ENDED("stream_ended"),
    STREAM_STATUS_CHANGED("stream_status_changed"),
    CHAT_MESSAGE("chat_message"),
    VIEWER_COUNT_CHANGED("viewer_count_changed"),
    PRODUCT_HIGHLIGHTED("product_highlighted"),
    COMPONENT_ACTIVATED("component_activated"),
    COMPONENT_DEACTIVATED("component_deactivated"),
    CHAT("CHAT"),
    DELETE_PINNED_MESSAGE("DELETE-PINNED-MESSAGE");

    companion object {
        fun from(value: String): TipioEventType? = entries.firstOrNull { it.wireName.equals(value, true) }
    }
}

sealed interface TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioStreamStatusData(
    val broadcasting: Boolean,
    @JsonAlias("hls") val hlsUrl: String? = null,
    @JsonAlias("player") val playerUrl: String? = null,
    val videoId: String? = null,
    val videoUrl: String? = null,
) : TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioChatMessageData(
    val userId: String,
    val username: String,
    val message: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant,
    val isStreamer: Boolean,
    val avatarUrl: String? = null,
    val isPinned: Boolean,
) : TipioEventData {
    fun toLiveChatMessage(): LiveChatMessage = LiveChatMessage(
        user = LiveChatUser(
            id = userId,
            username = username,
            avatarUrl = avatarUrl,
            isVerified = false,
            isModerator = isStreamer,
            role = if (isStreamer) ChatUserRole.STREAMER else ChatUserRole.VIEWER,
        ),
        message = message,
        timestamp = timestamp,
        isStreamerMessage = isStreamer,
        isPinned = isPinned,
    )
}

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioViewerCountData(
    val count: Int,
    val activeViewers: Int,
) : TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioProductHighlightData(
    val productId: String,
    val duration: Double,
    val position: String? = null,
) : TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioComponentData(
    val componentId: String,
    val type: String,
    val active: Boolean,
    val config: Map<String, JsonElement> = emptyMap(),
) : TipioEventData

@Serializable
data class TipioStreamLifecycleData(val stream: LiveStream) : TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioDeletePinnedMessageData(
    val channel: String,
    val liveStreamId: String,
    val message: TipioDeletePinnedMessage,
) : TipioEventData

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioDeletePinnedMessage(
    val clientId: String,
    val id: String? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val messageid: Instant,
    val visible: Boolean,
)

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioChatMessage(
    val user: String,
    val text: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val userTime: Instant,
    val role: String,
    val pinned: Boolean,
    val visible: Boolean? = null,
    val clientId: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val messageid: Instant,
    val father: TipioChatMessageFather? = null,
    val replies: List<TipioChatMessage> = emptyList(),
) : TipioEventData {
    fun toLiveChatMessage(): LiveChatMessage = LiveChatMessage(
        user = LiveChatUser(
            id = clientId,
            username = user,
            avatarUrl = null,
            isVerified = false,
            isModerator = role.equals("admin", true),
            role = if (role.equals("admin", true)) ChatUserRole.ADMIN else ChatUserRole.VIEWER,
        ),
        message = text,
        timestamp = userTime,
        isStreamerMessage = role.equals("admin", true),
        isPinned = pinned,
    )

    fun toRealtimeData(): TipioChatMessageData = TipioChatMessageData(
        userId = clientId,
        username = user,
        message = text,
        timestamp = userTime,
        isStreamer = role.equals("admin", true),
        avatarUrl = null,
        isPinned = pinned,
    )
}

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioChatMessageFather(
    val user: String,
    val text: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val userTime: Instant,
)

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class TipioEvent(
    val type: TipioEventType,
    val streamId: Int,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant = Instant.now(),
    val data: TipioEventData,
)

// MARK: API wrappers ----------------------------------------------------------

@Serializable
data class TipioApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: TipioApiError? = null,
    val message: String? = null,
)

@Serializable
data class TipioApiError(
    val code: String,
    override val message: String,
) : Exception(message)

@Serializable
data class TipioStatusResponse(
    val streamId: Int,
    val status: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant,
)

// MARK: Decoding helpers ------------------------------------------------------

object TipioEventDecoder {
    private val mapper get() = io.reachu.sdk.core.helpers.JsonUtils.mapper

    fun decode(json: String): TipioEvent? = runCatching {
        val node = mapper.readTree(json)
        val rawType = node["type"]?.asText()
            ?: node["event"]?.asText()
            ?: return null
        val type = TipioEventType.from(rawType) ?: return null
        val streamId = node["streamId"]?.asInt()
            ?: node["stream_id"]?.asInt()
            ?: 0
        val timestamp = node["timestamp"]?.asText()?.let { iso ->
            runCatching { Instant.parse(iso) }.getOrNull()
        } ?: Instant.now()
        val data = resolveData(type, node)
        TipioEvent(type = type, streamId = streamId, timestamp = timestamp, data = data)
    }.getOrNull()

    private fun resolveData(type: TipioEventType, node: JsonNode): TipioEventData {
        val payload = node["data"] ?: node["payload"] ?: node
        return when (type) {
            TipioEventType.STREAM_STATUS_CHANGED ->
                mapper.treeToValue(payload, TipioStreamStatusData::class.java)
            TipioEventType.CHAT_MESSAGE ->
                mapper.treeToValue(payload, TipioChatMessageData::class.java)
            TipioEventType.VIEWER_COUNT_CHANGED ->
                mapper.treeToValue(payload, TipioViewerCountData::class.java)
            TipioEventType.PRODUCT_HIGHLIGHTED ->
                mapper.treeToValue(payload, TipioProductHighlightData::class.java)
            TipioEventType.COMPONENT_ACTIVATED,
            TipioEventType.COMPONENT_DEACTIVATED ->
                mapper.treeToValue(payload, TipioComponentData::class.java)
            TipioEventType.CHAT ->
                mapper.treeToValue(payload, TipioChatMessage::class.java).toRealtimeData()
            TipioEventType.DELETE_PINNED_MESSAGE ->
                mapper.treeToValue(payload, TipioDeletePinnedMessageData::class.java)
            TipioEventType.STREAM_STARTED,
            TipioEventType.STREAM_ENDED ->
                TipioStreamLifecycleData(
                    mapper.treeToValue(payload, TipioLiveStream::class.java).toLiveStream(),
                )
        }
    }
}
