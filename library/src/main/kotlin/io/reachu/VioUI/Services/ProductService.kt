package io.reachu.VioUI.Services

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.utils.VioLogger
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.toDomainProduct
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.ProductDto
import java.net.MalformedURLException
import java.net.URL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Kotlin analogue of the Swift `ProductService`.
 * Centralizes SDK client management and exposes helpers to fetch products for Vio UI components.
 */
object ProductService {
    private val mutex = Mutex()
    @Volatile
    private var cachedVioSdkClient: VioSdkClient? = null

    private suspend fun getVioSdkClient(): VioSdkClient {
        cachedVioSdkClient?.let { return it }
        return mutex.withLock {
            cachedVioSdkClient?.let { return it }
            val state = VioConfiguration.shared.state.value
            val apiKey = state.apiKey.ifBlank { "DEMO_KEY" }
            val baseUrl = try {
                URL(state.environment.graphQLUrl)
            } catch (error: MalformedURLException) {
                throw ProductServiceError.InvalidConfiguration("Invalid GraphQL URL: ${state.environment.graphQLUrl}")
            }
            val client = VioSdkClient(baseUrl = baseUrl, apiKey = apiKey)
            VioLogger.debug("Created SDK client", "ProductService")
            cachedVioSdkClient = client
            client
        }
    }

    fun clearCache() {
        cachedVioSdkClient = null
        VioLogger.debug("Cleared SDK client cache", "ProductService")
    }

    suspend fun loadProduct(productId: String, currency: String, country: String): Product {
        val id = productId.toIntOrNull() ?: throw ProductServiceError.InvalidProductId(productId)
        return loadProduct(id, currency, country)
    }

    suspend fun loadProduct(productId: Int, currency: String, country: String): Product {
        VioLogger.debug("Loading product with ID: $productId", "ProductService")
        VioLogger.debug("Currency: $currency, Country: $country", "ProductService")
        val dto = fetchProducts(
            productIds = listOf(productId),
            currency = currency,
            country = country,
        ).firstOrNull() ?: throw ProductServiceError.ProductNotFound(productId)
        return dto.toDomainProduct()
    }

    suspend fun loadProducts(
        productIds: List<Int>?,
        currency: String,
        country: String,
    ): List<Product> {
        val idsToUse = productIds?.takeIf { it.isNotEmpty() }
        if (idsToUse != null) {
            VioLogger.debug("Loading products with IDs: $idsToUse", "ProductService")
        } else {
            VioLogger.debug("No product IDs provided - loading all products", "ProductService")
        }
        VioLogger.debug("Currency: $currency, Country: $country", "ProductService")
        val dtos = fetchProducts(idsToUse, currency, country)
        if (idsToUse != null && dtos.size < idsToUse.size) {
            val foundIds = dtos.map { it.id }.toSet()
            val missing = idsToUse.filterNot(foundIds::contains)
            if (missing.isNotEmpty()) {
                VioLogger.warning("Missing products for IDs: $missing", "ProductService")
            }
        }
        return dtos.map { it.toDomainProduct() }
    }

    suspend fun loadProductsByCategory(
        categoryId: Int,
        currency: String,
        country: String,
    ): List<Product> {
        VioLogger.debug("Loading products for category ID: $categoryId", "ProductService")
        VioLogger.debug("Currency: $currency, Country: $country", "ProductService")
        val dtos = try {
            val client = getVioSdkClient()
            client.channel.product.get(
                currency = currency,
                imageSize = "medium",
                barcodeList = null,
                categoryIds = listOf(categoryId),
                productIds = null,
                skuList = null,
                useCache = true,
                shippingCountryCode = country,
            )
        } catch (error: Throwable) {
            throw mapError(error)
        }
        return dtos.map { it.toDomainProduct() }
    }

    private suspend fun fetchProducts(
        productIds: List<Int>?,
        currency: String,
        country: String,
    ): List<ProductDto> = try {
        val client = getVioSdkClient()
        client.channel.product.get(
            currency = currency,
            imageSize = "medium",
            barcodeList = null,
            categoryIds = null,
            productIds = productIds,
            skuList = null,
            useCache = true,
            shippingCountryCode = country,
        )
    } catch (error: Throwable) {
        throw mapError(error)
    }

    private fun mapError(error: Throwable): ProductServiceError = when (error) {
        is ProductServiceError -> error
        is SdkException -> ProductServiceError.Sdk(error)
        else -> ProductServiceError.Network(error)
    }
}

sealed class ProductServiceError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidConfiguration(message: String) : ProductServiceError(message)
    class InvalidProductId(rawValue: String) : ProductServiceError("Invalid product ID format: $rawValue")
    class ProductNotFound(val productId: Int) : ProductServiceError("Product not found: $productId")
    class Sdk(val error: SdkException) : ProductServiceError(error.messageText, error)
    class Network(cause: Throwable) : ProductServiceError(cause.message ?: "Network error", cause)
}
