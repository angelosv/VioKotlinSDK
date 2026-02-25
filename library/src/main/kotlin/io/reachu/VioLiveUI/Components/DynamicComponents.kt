package io.reachu.liveui.components

import io.reachu.VioCore.models.Price
import io.reachu.VioCore.models.Product
import io.reachu.VioCore.models.ProductImage
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.reachu.liveui.components.DynamicComponentType.BANNER
import io.reachu.liveui.components.DynamicComponentType.FEATURED_PRODUCT

enum class DynamicComponentType(val rawValue: String) {
    @SerialName("featured_product")
    FEATURED_PRODUCT("featured_product"),
    @SerialName("banner")
    BANNER("banner");

    companion object {
        fun fromRaw(raw: String) = values().firstOrNull { it.rawValue == raw }
    }
}

enum class DynamicComponentPosition(val rawValue: String) {
    TOP("top"),
    BOTTOM("bottom"),
    TOP_CENTER("top-center"),
    CENTER("center"),
    BOTTOM_CENTER("bottom-center"),
    CUSTOM("custom"),
}

enum class DynamicComponentTrigger(val rawValue: String) {
    STREAM_START("stream_start"),
    MANUAL("manual"),
}

sealed interface DynamicComponentData {
    data class FeaturedProduct(
        val product: Product,
        val productId: Int?,
        val position: DynamicComponentPosition?,
        val startTime: Instant?,
        val endTime: Instant?,
        val triggerOn: DynamicComponentTrigger?,
    ) : DynamicComponentData

    data class Banner(
        val title: String?,
        val text: String?,
        val position: DynamicComponentPosition?,
        val animation: String?,
        val duration: Duration?,
        val startTime: Instant?,
        val endTime: Instant?,
    ) : DynamicComponentData
}

data class DynamicComponent(
    val id: String,
    val type: DynamicComponentType,
    val startTime: Instant?,
    val endTime: Instant?,
    val position: DynamicComponentPosition?,
    val triggerOn: DynamicComponentTrigger?,
    val data: DynamicComponentData,
)

/**
 * Wire DTO consumed by [DynamicComponentsService]. JSON payload mirrors the Swift
 * `RemoteDTO` structure.
 */
@Serializable
data class DynamicComponentRemoteDto(
    val id: String,
    val type: String,
    val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    val position: String? = null,
    val triggerOn: String? = null,
    val data: DataPayload,
) {
    @Serializable
    data class DataPayload(
        val title: String? = null,
        val text: String? = null,
        val animation: String? = null,
        val duration: Double? = null,
        val productId: Int? = null,
        val product: ProductDtoCompat? = null,
    )

    fun toDomain(): DynamicComponent? {
        val resolvedType = DynamicComponentType.fromRaw(type) ?: return null
        val start = startTime?.let { Instant.parse(it) }
        val end = endTime?.let { Instant.parse(it) }
        val pos = position?.let {
            runCatching { DynamicComponentPosition.valueOf(it.replace("-", "_").uppercase()) }.getOrNull()
        }
        val trigger = triggerOn?.let {
            runCatching { DynamicComponentTrigger.valueOf(it.replace("-", "_").uppercase()) }.getOrNull()
        }

        val domainData = when (resolvedType) {
            FEATURED_PRODUCT -> {
                val dtoProduct = data.product?.toDomain()
                dtoProduct ?: return null
                DynamicComponentData.FeaturedProduct(
                    product = dtoProduct,
                    productId = data.productId,
                    position = pos,
                    startTime = start,
                    endTime = end,
                    triggerOn = trigger,
                )
            }
            BANNER -> DynamicComponentData.Banner(
                title = data.title,
                text = data.text,
                position = pos,
                animation = data.animation,
                duration = data.duration?.let { Duration.ofMillis((it * 1000L).toLong()) },
                startTime = start,
                endTime = end,
            )
        }

        return DynamicComponent(
            id = id,
            type = resolvedType,
            startTime = start,
            endTime = end,
            position = pos,
            triggerOn = trigger,
            data = domainData,
        )
    }
}

/**
 * Light-weight product DTO compatible with the payload served by the dynamic
 * component service. The structure mirrors the Swift `ProductDtoCompat`.
 */
@Serializable
data class ProductDtoCompat(
    val id: Int,
    val title: String,
    val description: String? = null,
    val price: PriceDto,
    val image: String? = null,
) {
    @Serializable
    data class PriceDto(
        val amount: Double,
        val currencyCode: String,
        val amountInclTaxes: Double? = null,
        val taxAmount: Double? = null,
    )

    fun toDomain(): Product = Product(
        id = id,
        title = title,
        brand = null,
        description = description,
        tags = null,
        sku = "LIVE-$id",
        quantity = null,
        price = Price(
            amount = price.amount.toFloat(),
            currencyCode = price.currencyCode,
            amountInclTaxes = price.amountInclTaxes?.toFloat(),
            taxAmount = price.taxAmount?.toFloat(),
        ),
        variants = emptyList(),
        barcode = null,
        options = null,
        categories = null,
        images = listOfNotNull(image).mapIndexed { index, url ->
            ProductImage(
                id = "${id}_$index",
                url = url,
                width = null,
                height = null,
                order = index,
            )
        },
        productShipping = null,
        supplier = "Live Components",
        supplierId = null,
        importedProduct = null,
        referralFee = null,
        optionsEnabled = false,
        digital = false,
        origin = "",
        returns = null,
    )
}
