package io.reachu.sdk.modules.channel

import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.domain.repositories.ChannelCategoryRepository
import io.reachu.sdk.domain.repositories.ChannelInfoRepository
import io.reachu.sdk.domain.repositories.ChannelMarketRepository
import io.reachu.sdk.domain.repositories.ProductRepository

class Channel(
    client: GraphQLHttpClient,
) {
    val product: ProductRepository = ProductRepositoryGraphQL(client)
    val market: ChannelMarketRepository = ChannelMarketRepositoryGraphQL(client)
    val category: ChannelCategoryRepository = ChannelCategoryRepositoryGraphQL(client)
    val info: ChannelInfoRepository = ChannelInfoRepositoryGraphQL(client)
}
