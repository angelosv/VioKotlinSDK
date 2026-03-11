package live.vio.VioCore.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommerceProduct(
    val id: String,
    val name: String,
    val primaryImageUrl: String?,
    val formattedPrice: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLError(
    val message: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GetProductByIdResponse(
    @JsonProperty("Channel") val channel: ChannelData? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChannelData(
    @JsonProperty("GetProductById") val product: ProductData? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductData(
    val id: String,
    val name: String,
    val images: List<CommerceImage>? = emptyList(),
    val price: CommercePrice? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommerceImage(
    val url: String,
    val order: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommercePrice(
    val amount: Double,
    @JsonProperty("amount_incl_taxes") val amountInclTaxes: Double?,
    @JsonProperty("currency_code") val currencyCode: String
)
