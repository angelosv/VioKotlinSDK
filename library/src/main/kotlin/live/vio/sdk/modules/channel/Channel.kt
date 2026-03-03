package live.vio.sdk.modules.channel

import live.vio.sdk.core.graphql.GraphQLHttpClient
import live.vio.sdk.domain.repositories.ChannelCategoryRepository
import live.vio.sdk.domain.repositories.ChannelInfoRepository
import live.vio.sdk.domain.repositories.ChannelMarketRepository
import live.vio.sdk.domain.repositories.ProductRepository

class Channel(
    client: GraphQLHttpClient,
) {
    val product: ProductRepository = ProductRepositoryGraphQL(client)
    val market: ChannelMarketRepository = ChannelMarketRepositoryGraphQL(client)
    val category: ChannelCategoryRepository = ChannelCategoryRepositoryGraphQL(client)
    val info: ChannelInfoRepository = ChannelInfoRepositoryGraphQL(client)
}
