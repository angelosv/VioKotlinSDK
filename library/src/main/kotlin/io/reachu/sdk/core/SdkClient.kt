package io.reachu.sdk.core

import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.modules.cart.CartRepositoryGraphQL
import io.reachu.sdk.modules.channel.Channel
import io.reachu.sdk.modules.checkout.CheckoutRepositoryGraphQL
import io.reachu.sdk.modules.discount.DiscountRepositoryGraphQL
import io.reachu.sdk.modules.market.MarketRepositoryGraphQL
import io.reachu.sdk.modules.payment.PaymentRepositoryGraphQL
import io.reachu.sdk.domain.repositories.CartRepository
import io.reachu.sdk.domain.repositories.CheckoutRepository
import io.reachu.sdk.domain.repositories.DiscountRepository
import io.reachu.sdk.domain.repositories.MarketRepository
import io.reachu.sdk.domain.repositories.PaymentRepository
import java.net.URL

/**
 * Kotlin counterpart for the Swift `SdkClient`.
 * It wires repositories with a shared GraphQL client and exposes them as properties.
 */
class SdkClient(
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
