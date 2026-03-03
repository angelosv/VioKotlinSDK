package live.vio.sdk.modules.channel

import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.core.graphql.GraphQLHttpClient
import live.vio.sdk.core.graphql.operations.ChannelGraphQL
import live.vio.sdk.core.helpers.GraphQLPick
import live.vio.sdk.domain.models.GetAvailableMarketsDto
import live.vio.sdk.domain.repositories.ChannelMarketRepository

class ChannelMarketRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : ChannelMarketRepository {

    override suspend fun getAvailable(): List<GetAvailableMarketsDto> {
        val response = client.runQuerySafe(
            ChannelGraphQL.GET_AVAILABLE_MARKETS_CHANNEL_QUERY,
            emptyMap(),
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Channel", "GetAvailableMarkets"))
            ?: throw SdkException("Empty response in Channel.market", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetAvailableMarketsDto>>(list)
    }
}
