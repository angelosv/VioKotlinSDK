package io.reachu.sdk.domain.repositories

import io.reachu.sdk.domain.models.GetAvailableMarketsDto
import io.reachu.sdk.domain.models.GetCategoryDto
import io.reachu.sdk.domain.models.GetChannelsDto
import io.reachu.sdk.domain.models.GetTermsAndConditionsDto
import io.reachu.sdk.domain.models.ProductDto

interface ProductRepository {
    suspend fun get(
        currency: String?,
        imageSize: String? = "large",
        barcodeList: List<String>?,
        categoryIds: List<Int>?,
        productIds: List<Int>?,
        skuList: List<String>?,
        useCache: Boolean = true,
        shippingCountryCode: String?,
    ): List<ProductDto>

    suspend fun getByCategoryId(
        categoryId: Int,
        currency: String?,
        imageSize: String = "large",
        shippingCountryCode: String?,
    ): List<ProductDto>

    suspend fun getByCategoryIds(
        categoryIds: List<Int>,
        currency: String?,
        imageSize: String = "large",
        shippingCountryCode: String?,
    ): List<ProductDto>

    suspend fun getByParams(
        currency: String?,
        imageSize: String = "large",
        sku: String?,
        barcode: String?,
        productId: Int?,
        shippingCountryCode: String?,
    ): ProductDto

    suspend fun getByIds(
        productIds: List<Int>,
        currency: String?,
        imageSize: String = "large",
        useCache: Boolean = true,
        shippingCountryCode: String?,
    ): List<ProductDto>

    suspend fun getBySkus(
        sku: String,
        productId: Int?,
        currency: String?,
        imageSize: String = "large",
        shippingCountryCode: String?,
    ): List<ProductDto>

    suspend fun getByBarcodes(
        barcode: String,
        productId: Int?,
        currency: String?,
        imageSize: String = "large",
        shippingCountryCode: String?,
    ): List<ProductDto>
}

interface ChannelMarketRepository {
    suspend fun getAvailable(): List<GetAvailableMarketsDto>
}

interface ChannelCategoryRepository {
    suspend fun get(): List<GetCategoryDto>
}

interface ChannelInfoRepository {
    suspend fun getChannels(): List<GetChannelsDto>
    suspend fun getPurchaseConditions(): GetTermsAndConditionsDto
    suspend fun getTermsAndConditions(): GetTermsAndConditionsDto
}
