package live.vio.VioCore.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.models.*
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for interacting with the Vio Commerce GraphQL API.
 */
object VioCommerceService {
    private const val TAG = "VioCommerceService"

    /**
     * Fetches product details by ID.
     */
    suspend fun fetchProduct(id: String): CommerceProduct? = withContext(Dispatchers.IO) {
        val commerceConfig = VioConfiguration.shared.state.value.commerce
        val apiKey = commerceConfig?.apiKey ?: ""
        val endpoint = commerceConfig?.endpoint
            ?: VioConfiguration.shared.state.value.sdkBootstrapCommerceGraphQLURL
        
        if (apiKey.isBlank() || endpoint.isNullOrBlank()) {
            VioLogger.warning(
                "Commerce bootstrap missing (apiKey or endpoint). Cannot fetch product $id",
                TAG,
            )
            return@withContext null
        }

        val query = """
            {
              Channel {
                GetProductById(id: "$id", countryCode: "NO", currencyCode: "NOK") {
                  id name
                  images { url order }
                  price { amount amount_incl_taxes currency_code }
                }
              }
            }
        """.trimIndent()

        val variables = emptyMap<String, Any>()
        val payload = mapOf(
            "query" to query,
            "variables" to variables
        )

        val jsonPayload = JsonUtils.mapper.writeValueAsString(payload)

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-API-Key", apiKey)
            doOutput = true
        }

        try {
            connection.outputStream.use { it.write(jsonPayload.toByteArray()) }
            
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (status !in 200..299) {
                VioLogger.error("GraphQL request failed with status $status: $body", TAG)
                return@withContext null
            }

            val responseType = JsonUtils.mapper.typeFactory.constructParametricType(
                GraphQLResponse::class.java,
                GetProductByIdResponse::class.java
            )
            val gqlResponse: GraphQLResponse<GetProductByIdResponse> = JsonUtils.mapper.readValue(body, responseType)

            if (gqlResponse.errors?.isNotEmpty() == true) {
                VioLogger.error("GraphQL errors: ${gqlResponse.errors.firstOrNull()?.message}", TAG)
                return@withContext null
            }

            val productData = gqlResponse.data?.channel?.product ?: return@withContext null
            
            val primaryImage = productData.images?.minByOrNull { it.order }?.url
            val priceValue = productData.price?.amountInclTaxes ?: productData.price?.amount ?: 0.0
            val currency = productData.price?.currencyCode ?: ""
            val formattedPrice = formatPrice(currency, priceValue)

            CommerceProduct(
                id = productData.id,
                name = productData.name,
                primaryImageUrl = primaryImage,
                formattedPrice = formattedPrice
            )
        } catch (e: Exception) {
            VioLogger.error("Failed to fetch product $id: ${e.message}", TAG)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun formatPrice(currency: String, amount: Double): String {
        val symbol = when(currency.uppercase()) {
            "NOK" -> "kr"
            "EUR" -> "€"
            "USD" -> "$"
            else -> currency
        }
        val sep = if (symbol.length > 1) " " else ""
        return "%s%s%.2f".format(symbol, sep, amount)
    }
}
