package live.vio.VioUI.Services

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioLogger
import live.vio.VioUI.Managers.Product
import live.vio.VioUI.Managers.toDomainProduct
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.domain.models.ProductDto
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
        VioConfiguration.waitForRemoteConfig("ProductService")
        
        val state = VioConfiguration.shared.state.value
        val commerceApiKey = state.commerce?.apiKey
        
        // If we have a cached client, check if the API key matches the current commerce key
        cachedVioSdkClient?.let { client ->
            if (commerceApiKey != null && client.apiKey == commerceApiKey) {
                return client
            }
            // If the commerce key changed, we need to clear the cache and recreate
            if (commerceApiKey != null) {
                clearCache()
            } else if (client.apiKey == state.apiKey) {
                // If we are using the fallback key and it hasn't changed, return it
                return client
            } else {
                clearCache()
            }
        }

        return mutex.withLock {
            cachedVioSdkClient?.let { return it }
            
            // Re-fetch state to be sure we have the latest after lock
            val currentState = VioConfiguration.shared.state.value
            // PRIORITY: Use commerce-specific API key if available
            val apiKey = currentState.commerce?.apiKey ?: currentState.apiKey

            val baseUrl = try {
                URL(currentState.commerce?.endpoint ?: currentState.environment.graphQLUrl)
            } catch (error: MalformedURLException) {
                throw ProductServiceError.InvalidConfiguration("Invalid GraphQL URL")
            }
            
            val client = VioSdkClient(baseUrl = baseUrl, apiKey = apiKey)
            VioLogger.info("Created SDK client with dynamic Commerce API key", "ProductService")
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
        println("📦 [ProductService] loadProducts: IDs=${productIds ?: "ALL"}, currency=$currency, country=$country")
        val dtos = fetchProducts(idsToUse, currency, country)
        println("📦 [ProductService] loadProducts: got ${dtos.size} DTOs")
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
