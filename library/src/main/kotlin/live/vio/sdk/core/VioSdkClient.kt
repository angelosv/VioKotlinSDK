package live.vio.sdk.core

import live.vio.sdk.core.graphql.GraphQLHttpClient
import live.vio.sdk.modules.cart.CartRepositoryGraphQL
import live.vio.sdk.modules.channel.Channel
import live.vio.sdk.modules.checkout.CheckoutRepositoryGraphQL
import live.vio.sdk.modules.discount.DiscountRepositoryGraphQL
import live.vio.sdk.modules.market.MarketRepositoryGraphQL
import live.vio.sdk.modules.payment.PaymentRepositoryGraphQL
import live.vio.sdk.domain.repositories.CartRepository
import live.vio.sdk.domain.repositories.CheckoutRepository
import live.vio.sdk.domain.repositories.DiscountRepository
import live.vio.sdk.domain.repositories.MarketRepository
import live.vio.sdk.domain.repositories.PaymentRepository
import java.net.URL

/**
 * The primary entry point for the Vio SDK logic.
 *
 * This client provides access to all Vio platform modules (Cart, Checkout, Discount, etc.)
 * by wiring them with a shared [GraphQLHttpClient].
 *
 * ### Example Usage:
 *
 * ```kotlin
 * val sdk = VioSdkClient(
 *     baseUrl = URL("https://graph-ql.vio.live"),
 *     apiKey = "YOUR_API_KEY"
 * )
 *
 * val markets = sdk.market.getAvailable()
 * ```
 *
 * @property baseUrl The base URL of the Vio GraphQL API.
 * @property apiKey The API token for authentication.
 */
class VioSdkClient(
    val baseUrl: URL,
    val apiKey: String,
    httpClientFactory: (URL, String) -> GraphQLHttpClient = { url, key ->
        GraphQLHttpClient(url.toExternalForm(), key)
    },
) {
    val apolloClient: GraphQLHttpClient = httpClientFactory(baseUrl, apiKey)

    val cart: CartRepository = CartRepositoryGraphQL(apolloClient)
    val channel: Channel = Channel(apolloClient)
    val checkout: CheckoutRepository = CheckoutRepositoryGraphQL(apolloClient)
    val discount: DiscountRepository = DiscountRepositoryGraphQL(
        apolloClient,
        apiKey,
        baseUrl.toExternalForm(),
    )
    val market: MarketRepository = MarketRepositoryGraphQL(apolloClient)
    val payment: PaymentRepository = PaymentRepositoryGraphQL(apolloClient)
}
