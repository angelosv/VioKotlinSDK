package live.vio.VioUI.Services

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioLogger
import live.vio.VioUI.Managers.Product
import live.vio.VioUI.Managers.toDomainProduct
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.core.graphql.CommerceSdkClientProvider
import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.domain.models.ProductDto

/**
 * Kotlin analogue of the Swift `ProductService`.
 * Centralizes SDK client management and exposes helpers to fetch products for Vio UI components.
 */
object ProductService {
    private suspend fun getVioSdkClient(forSponsorId: Int? = null): VioSdkClient {
        VioConfiguration.waitForRemoteConfig("ProductService")
        val client = CommerceSdkClientProvider.client(
            forSponsorId = forSponsorId,
            configuration = VioConfiguration.shared,
        )
        val state = VioConfiguration.shared.state.value
        val currentGraphQLUrl = state.sdkBootstrapCommerceGraphQLURL
            ?: state.commerce?.endpoint
            ?: state.environment.graphQLUrl
        val authSource = when {
            forSponsorId != null && CommerceSdkClientProvider.activeSponsorId == forSponsorId -> "per-sponsor (id=$forSponsorId)"
            state.sdkBootstrapCommerceApiKey != null -> "bootstrap primary"
            state.commerce?.apiKey != null -> "remote commerce"
            else -> "fallback"
        }
        VioLogger.info("Created SDK client auth=$authSource url=$currentGraphQLUrl", "ProductService")
        return client
    }

    fun clearCache() {
        CommerceSdkClientProvider.clear()
        VioLogger.debug("Cleared SDK client cache", "ProductService")
    }

    suspend fun loadProduct(productId: String, currency: String, country: String, sponsorId: Int? = null): Product {
        val id = productId.toIntOrNull() ?: throw ProductServiceError.InvalidProductId(productId)
        return loadProduct(id, currency, country, sponsorId = sponsorId)
    }

    suspend fun loadProductDto(productId: String, currency: String, country: String, sponsorId: Int? = null): ProductDto {
        val id = productId.toIntOrNull() ?: throw ProductServiceError.InvalidProductId(productId)
        return loadProductDto(id, currency, country, sponsorId = sponsorId)
    }

    suspend fun loadProductDto(productId: Int, currency: String, country: String, sponsorId: Int? = null): ProductDto {
        VioLogger.debug("Loading product DTO with ID: $productId", "ProductService")
        return fetchProducts(
            productIds = listOf(productId),
            currency = currency,
            country = country,
            sponsorId = sponsorId,
        ).firstOrNull() ?: throw ProductServiceError.ProductNotFound(productId)
    }

    suspend fun loadProduct(productId: Int, currency: String, country: String, sponsorId: Int? = null): Product {
        val source = when {
            sponsorId != null && VioConfiguration.commerce(forSponsorId = sponsorId) != null -> "per-sponsor (id=$sponsorId)"
            VioConfiguration.shared.state.value.sdkBootstrapCommerceApiKey != null -> "bootstrap primary"
            else -> "fallback"
        }
        VioLogger.info("loadProduct → id=$productId auth=$source cc=$country cur=$currency", "ProductService")
        VioLogger.debug("Loading product with ID: $productId", "ProductService")
        VioLogger.debug("Currency: $currency, Country: $country", "ProductService")
        val dto = loadProductDto(productId, currency, country, sponsorId = sponsorId)
        return dto.toDomainProduct()
    }

    suspend fun loadProducts(
        productIds: List<Int>?,
        currency: String,
        country: String,
        sponsorId: Int? = null,
    ): List<Product> {
        val source = when {
            sponsorId != null && VioConfiguration.commerce(forSponsorId = sponsorId) != null -> "per-sponsor (id=$sponsorId)"
            VioConfiguration.shared.state.value.sdkBootstrapCommerceApiKey != null -> "bootstrap primary"
            else -> "fallback"
        }
        val idsText = productIds?.joinToString(",") ?: "ALL"
        VioLogger.info("loadProducts → ids=$idsText auth=$source cc=$country cur=$currency", "ProductService")
        
        val idsToUse = productIds?.takeIf { it.isNotEmpty() }
        if (idsToUse != null) {
            VioLogger.debug("Loading products with IDs: $idsToUse", "ProductService")
        } else {
            VioLogger.debug("No product IDs provided - loading all products", "ProductService")
        }
        println("📦 [ProductService] loadProducts: IDs=${productIds ?: "ALL"}, currency=$currency, country=$country")
        val dtos = fetchProducts(idsToUse, currency, country, sponsorId = sponsorId)
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
            val primary = client.channel.product.get(
                currency = currency,
                imageSize = "medium",
                barcodeList = null,
                categoryIds = listOf(categoryId),
                productIds = null,
                skuList = null,
                useCache = true,
                shippingCountryCode = country,
            )
            if (primary.isNotEmpty() || country.isBlank()) {
                primary
            } else {
                VioLogger.warning(
                    "No products for country=$country currency=$currency category=$categoryId. Retrying without shipping country filter.",
                    "ProductService",
                )
                client.channel.product.get(
                    currency = currency,
                    imageSize = "medium",
                    barcodeList = null,
                    categoryIds = listOf(categoryId),
                    productIds = null,
                    skuList = null,
                    useCache = true,
                    shippingCountryCode = null,
                )
            }
        } catch (error: Throwable) {
            throw mapError(error)
        }
        return dtos.map { it.toDomainProduct() }
    }

    private suspend fun fetchProducts(
        productIds: List<Int>?,
        currency: String,
        country: String,
        sponsorId: Int?,
    ): List<ProductDto> = try {
        val client = getVioSdkClient(forSponsorId = sponsorId)
        val primary = client.channel.product.get(
            currency = currency,
            imageSize = "medium",
            barcodeList = null,
            categoryIds = null,
            productIds = productIds,
            skuList = null,
            useCache = true,
            shippingCountryCode = country,
        )
        if (primary.isNotEmpty() || country.isBlank()) {
            primary
        } else {
            VioLogger.warning(
                "No products for country=$country currency=$currency ids=${productIds ?: "ALL"}. Retrying without shipping country filter.",
                "ProductService",
            )
            client.channel.product.get(
                currency = currency,
                imageSize = "medium",
                barcodeList = null,
                categoryIds = null,
                productIds = productIds,
                skuList = null,
                useCache = true,
                shippingCountryCode = null,
            )
        }
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
