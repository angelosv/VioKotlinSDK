package io.reachu.liveshow.models

import io.reachu.VioCore.models.Price
import io.reachu.VioCore.models.Product
import io.reachu.VioCore.models.ProductImage
import java.text.NumberFormat
import java.time.Instant
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Core livestream entities mirrored from the Swift VioLiveShow module.
 */
@Serializable
data class LiveStream(
    val id: String,
    val title: String,
    val description: String? = null,
    val streamer: LiveStreamer,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val viewerCount: Int = 0,
    val isLive: Boolean = true,
    @Serializable(with = InstantIso8601Serializer::class)
    val startTime: Instant = Instant.now(),
    @Serializable(with = InstantIso8601Serializer::class)
    val endTime: Instant? = null,
    val featuredProducts: List<LiveProduct> = emptyList(),
    val chatMessages: List<LiveChatMessage> = emptyList(),
)

@Serializable
data class LiveStreamer(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val followerCount: Int = 0,
)

@Serializable
data class LiveProduct(
    val id: String,
    val title: String,
    val price: Price,
    val originalPrice: Price? = null,
    val imageUrl: String,
    val isAvailable: Boolean = true,
    val stockCount: Int? = null,
    val discount: String? = null,
    val specialOffer: String? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val showUntil: Instant? = null,
) {
    // Component-only helpers live as extensions to keep parity with Swift.
}

/**
 * Mirrors the Swift `asProduct` computed property for cart integrations.
 */
val LiveProduct.asProduct: Product
    get() = Product(
        id = id.hashCode(),
        title = title,
        brand = null,
        description = specialOffer ?: "Featured on Live Show",
        tags = null,
        sku = "LIVE-$id",
        quantity = stockCount ?: 100,
        price = price,
        variants = emptyList(),
        barcode = null,
        options = null,
        categories = null,
        images = listOf(
            ProductImage(
                id = UUID.randomUUID().toString(),
                url = imageUrl,
                width = null,
                height = null,
                order = 0,
            ),
        ),
        productShipping = null,
        supplier = "Live Show",
        supplierId = null,
        importedProduct = null,
        referralFee = null,
        optionsEnabled = false,
        digital = false,
        origin = "",
        returns = null,
    )

@Deprecated("Use asProduct to mirror the Swift API", ReplaceWith("asProduct"))
fun LiveProduct.asCartProduct(): Product = asProduct

@Serializable
data class LiveChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val user: LiveChatUser,
    val message: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant = Instant.now(),
    val isStreamerMessage: Boolean = false,
    val isPinned: Boolean = false,
    val reactions: List<LiveChatReaction> = emptyList(),
)

@Serializable
enum class ChatUserRole(val rawValue: String) {
    @SerialName("viewer")
    VIEWER("viewer"),
    @SerialName("subscriber")
    SUBSCRIBER("subscriber"),
    @SerialName("moderator")
    MODERATOR("moderator"),
    @SerialName("admin")
    ADMIN("admin"),
    @SerialName("streamer")
    STREAMER("streamer"),
    @SerialName("vip")
    VIP("vip");

    val displayName: String
        get() = when (this) {
            VIEWER -> "Viewer"
            SUBSCRIBER -> "Subscriber"
            MODERATOR -> "Mod"
            ADMIN -> "Admin"
            STREAMER -> "Streamer"
            VIP -> "VIP"
        }

    val colorToken: String
        get() = when (this) {
            VIEWER -> "gray"
            SUBSCRIBER -> "blue"
            MODERATOR -> "yellow"
            ADMIN -> "red"
            STREAMER -> "purple"
            VIP -> "gold"
        }

    val priority: Int
        get() = when (this) {
            STREAMER -> 5
            ADMIN -> 4
            MODERATOR -> 3
            VIP -> 2
            SUBSCRIBER -> 1
            VIEWER -> 0
        }

    companion object {
        fun fromRaw(value: String): ChatUserRole? =
            entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) }
    }
}

@Serializable
data class LiveChatUser(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val isModerator: Boolean = false,
    val role: ChatUserRole = ChatUserRole.VIEWER,
    @Serializable(with = InstantIso8601Serializer::class)
    val joinDate: Instant? = null,
    val subscriberMonths: Int? = null,
) {
    val isAdmin: Boolean get() = role == ChatUserRole.ADMIN || role == ChatUserRole.STREAMER
    val isStreamer: Boolean get() = role == ChatUserRole.STREAMER
    val isVip: Boolean get() = role == ChatUserRole.VIP
    val isSubscriber: Boolean get() = role == ChatUserRole.SUBSCRIBER || subscriberMonths != null
}

@Serializable
data class LiveChatReaction(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val count: Int = 1,
)

enum class LiveStreamLayout(val rawValue: String) {
    FULL_SCREEN_OVERLAY("fullScreenOverlay"),
    BOTTOM_SHEET("bottomSheet"),
    MODAL("modal");

    val displayName: String
        get() = when (this) {
            FULL_SCREEN_OVERLAY -> "Full Screen Overlay"
            BOTTOM_SHEET -> "Bottom Sheet"
            MODAL -> "Modal"
        }

    companion object {
        fun fromRaw(value: String): LiveStreamLayout? =
            entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) }
    }
}

enum class MiniPlayerPosition(val rawValue: String) {
    TOP_LEFT("topLeft"),
    TOP_RIGHT("topRight"),
    BOTTOM_LEFT("bottomLeft"),
    BOTTOM_RIGHT("bottomRight");

    val displayName: String
        get() = when (this) {
            TOP_LEFT -> "Top Left"
            TOP_RIGHT -> "Top Right"
            BOTTOM_LEFT -> "Bottom Left"
            BOTTOM_RIGHT -> "Bottom Right"
        }

    companion object {
        fun fromRaw(value: String): MiniPlayerPosition? =
            entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) }
    }
}

sealed class LiveStreamSocketEvent {
    data class Started(val stream: LiveStream) : LiveStreamSocketEvent()
    data class Ended(val stream: LiveStream) : LiveStreamSocketEvent()
}

/**
 * Kotlin helper mirroring the Swift `Price.formattedPrice`.
 */
fun Price.formattedPrice(locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = Currency.getInstance(currencyCode)
    val value = amountInclTaxes ?: amount
    return format.format(value)
}

fun Price.formattedCompareAtPrice(locale: Locale = Locale.getDefault()): String? {
    val compare = compareAtInclTaxes ?: compareAt ?: return null
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = Currency.getInstance(currencyCode)
    return format.format(compare)
}

/**
 * Utility used by debugging helpers to dump JSON payloads identically to Swift.
 */
fun LiveStream.prettyPrint(): String = Json { prettyPrint = true }.encodeToString(this)
