package io.reachu.sdk.modules.channel

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.ChannelGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.domain.models.GetAvailableMarketsDto
import io.reachu.sdk.domain.repositories.ChannelMarketRepository

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
