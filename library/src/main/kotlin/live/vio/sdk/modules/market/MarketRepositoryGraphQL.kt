package live.vio.sdk.modules.market

import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.core.graphql.GraphQLHttpClient
import live.vio.sdk.core.graphql.operations.MarketGraphQL
import live.vio.sdk.core.helpers.GraphQLPick
import live.vio.sdk.domain.models.GetAvailableGlobalMarketsDto
import live.vio.sdk.domain.repositories.MarketRepository

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
