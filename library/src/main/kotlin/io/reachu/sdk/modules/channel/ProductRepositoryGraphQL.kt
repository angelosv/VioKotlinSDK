package io.reachu.sdk.modules.channel

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.errors.ValidationException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.ChannelGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.core.validation.Validation
import io.reachu.sdk.domain.models.ProductDto
import io.reachu.sdk.domain.repositories.ProductRepository

class ProductRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : ProductRepository {

    private fun requirePositiveIds(ids: List<Int>, field: String) {
        if (ids.isEmpty()) {
            throw ValidationException("$field cannot be empty", details = mapOf("field" to field))
        }
        if (ids.any { it <= 0 }) {
            throw ValidationException(
                "$field must contain positive IDs only",
                details = mapOf("field" to field),
            )
        }
    }

    private fun requireNonEmptyStrings(values: List<String>, field: String) {
        if (values.any { it.trim().isEmpty() }) {
            throw ValidationException("$field cannot contain empty strings", details = mapOf("field" to field))
        }
    }

    private fun validateCommonFilters(
        currency: String?,
        imageSize: String?,
        shippingCountryCode: String?,
    ) {
        currency?.let { Validation.requireCurrency(it) }
        shippingCountryCode?.let { Validation.requireCountry(it) }
        if (imageSize != null && imageSize.trim().isEmpty()) {
            throw ValidationException("imageSize cannot be empty", details = mapOf("field" to "imageSize"))
        }
    }

    private suspend fun fetchProducts(
        currency: String?,
        imageSize: String?,
        barcodeList: List<String>?,
        categoryIds: List<Int>?,
        productIds: List<Int>?,
        skuList: List<String>?,
        useCache: Boolean,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        val variables = buildMap<String, Any?> {
            put("currency", currency)
            put("imageSize", imageSize)
            put("barcodeList", barcodeList ?: emptyList<String>())
            put("skuList", skuList ?: emptyList<String>())
            put("categoryIds", categoryIds ?: emptyList<Int>())
            put("productIds", productIds ?: emptyList<Int>())
            put("useCache", useCache)
            put("shippingCountryCode", shippingCountryCode)
        }

        val response = client.runQuerySafe(
            ChannelGraphQL.GET_PRODUCTS_CHANNEL_QUERY,
            variables,
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Channel", "Products"))
            ?: throw SdkException("Empty response in Product.get", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<ProductDto>>(list)
    }

    private suspend fun fetchAllProducts(
        currency: String?,
        imageSize: String?,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        val variables = buildMap<String, Any?> {
            put("currency", currency)
            put("imageSize", imageSize)
            put("shippingCountryCode", shippingCountryCode)
        }
        val response = client.runQuerySafe(
            ChannelGraphQL.GET_PRODUCTS_QUERY,
            variables,
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Channel", "GetProducts"))
            ?: throw SdkException("Empty response in Product.get", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<ProductDto>>(list)
    }

    override suspend fun get(
        currency: String?,
        imageSize: String?,
        barcodeList: List<String>?,
        categoryIds: List<Int>?,
        productIds: List<Int>?,
        skuList: List<String>?,
        useCache: Boolean,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        categoryIds?.let { requirePositiveIds(it, "categoryIds") }
        productIds?.let { requirePositiveIds(it, "productIds") }
        skuList?.let { requireNonEmptyStrings(it, "skuList") }
        barcodeList?.let { requireNonEmptyStrings(it, "barcodeList") }
        val hasProductIds = !productIds.isNullOrEmpty()
        val hasBarcodeList = !barcodeList.isNullOrEmpty()
        val hasSkuList = !skuList.isNullOrEmpty()
        val hasCategoryIds = !categoryIds.isNullOrEmpty()
        val loadAllProducts = !(hasProductIds || hasBarcodeList || hasSkuList || hasCategoryIds)

        return if (loadAllProducts) {
            fetchAllProducts(currency, imageSize, shippingCountryCode)
        } else {
            fetchProducts(
                currency,
                imageSize,
                barcodeList,
                categoryIds,
                productIds,
                skuList,
                useCache,
                shippingCountryCode,
            )
        }
    }

    override suspend fun getByCategoryId(
        categoryId: Int,
        currency: String?,
        imageSize: String,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        if (categoryId <= 0) {
            throw ValidationException(
                "categoryId must be > 0",
                details = mapOf("field" to "categoryId"),
            )
        }
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        return fetchProducts(
            currency,
            imageSize,
            emptyList(),
            listOf(categoryId),
            emptyList(),
            emptyList(),
            true,
            shippingCountryCode,
        )
    }

    override suspend fun getByCategoryIds(
        categoryIds: List<Int>,
        currency: String?,
        imageSize: String,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        requirePositiveIds(categoryIds, "categoryIds")
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        return fetchProducts(
            currency,
            imageSize,
            emptyList(),
            categoryIds,
            emptyList(),
            emptyList(),
            true,
            shippingCountryCode,
        )
    }

    override suspend fun getByParams(
        currency: String?,
        imageSize: String,
        sku: String?,
        barcode: String?,
        productId: Int?,
        shippingCountryCode: String?,
    ): ProductDto {
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        if (productId != null && productId <= 0) {
            throw ValidationException("productId must be > 0", details = mapOf("field" to "productId"))
        }
        if (sku != null && sku.trim().isEmpty()) {
            throw ValidationException("sku cannot be empty", details = mapOf("field" to "sku"))
        }
        if (barcode != null && barcode.trim().isEmpty()) {
            throw ValidationException("barcode cannot be empty", details = mapOf("field" to "barcode"))
        }

        val products = fetchProducts(
            currency,
            imageSize,
            barcode?.let { listOf(it) } ?: emptyList(),
            emptyList(),
            productId?.let { listOf(it) } ?: emptyList(),
            sku?.let { listOf(it) } ?: emptyList(),
            true,
            shippingCountryCode,
        )
        return products.firstOrNull()
            ?: throw SdkException("Empty response in Product.getByParams", code = "EMPTY_RESPONSE")
    }

    override suspend fun getByIds(
        productIds: List<Int>,
        currency: String?,
        imageSize: String,
        useCache: Boolean,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        requirePositiveIds(productIds, "productIds")
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        return fetchProducts(
            currency,
            imageSize,
            emptyList(),
            emptyList(),
            productIds,
            emptyList(),
            useCache,
            shippingCountryCode,
        )
    }

    override suspend fun getBySkus(
        sku: String,
        productId: Int?,
        currency: String?,
        imageSize: String,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        Validation.requireNonEmpty(sku, "sku")
        if (productId != null && productId <= 0) {
            throw ValidationException("productId must be > 0", details = mapOf("field" to "productId"))
        }
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        return fetchProducts(
            currency,
            imageSize,
            emptyList(),
            emptyList(),
            productId?.let { listOf(it) } ?: emptyList(),
            listOf(sku),
            true,
            shippingCountryCode,
        )
    }

    override suspend fun getByBarcodes(
        barcode: String,
        productId: Int?,
        currency: String?,
        imageSize: String,
        shippingCountryCode: String?,
    ): List<ProductDto> {
        Validation.requireNonEmpty(barcode, "barcode")
        if (productId != null && productId <= 0) {
            throw ValidationException("productId must be > 0", details = mapOf("field" to "productId"))
        }
        validateCommonFilters(currency, imageSize, shippingCountryCode)
        return fetchProducts(
            currency,
            imageSize,
            listOf(barcode),
            emptyList(),
            productId?.let { listOf(it) } ?: emptyList(),
            emptyList(),
            true,
            shippingCountryCode,
        )
    }
}
