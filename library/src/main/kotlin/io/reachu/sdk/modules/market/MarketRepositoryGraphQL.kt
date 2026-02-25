package io.reachu.sdk.modules.market

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.MarketGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.domain.models.GetAvailableGlobalMarketsDto
import io.reachu.sdk.domain.repositories.MarketRepository

class MarketRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : MarketRepository {

    override suspend fun getAvailable(): List<GetAvailableGlobalMarketsDto> {
        val response = client.runQuerySafe(MarketGraphQL.GET_AVAILABLE_MARKET_QUERY, emptyMap())
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Markets", "GetAvailableMarkets"))
            ?: throw SdkException("Empty response in Market.getAvailable", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetAvailableGlobalMarketsDto>>(list)
    }
}
