package live.vio.sdk.modules.channel

import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.core.graphql.GraphQLHttpClient
import live.vio.sdk.core.graphql.operations.ChannelGraphQL
import live.vio.sdk.core.helpers.GraphQLPick
import live.vio.sdk.domain.models.GetCategoryDto
import live.vio.sdk.domain.repositories.ChannelCategoryRepository

class ChannelCategoryRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : ChannelCategoryRepository {

    override suspend fun get(): List<GetCategoryDto> {
        val response = client.runQuerySafe(ChannelGraphQL.GET_CATEGORIES_CHANNEL_QUERY, emptyMap())
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Channel", "GetCategories"))
            ?: throw SdkException("Empty response in Channel.getCategories", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetCategoryDto>>(list)
    }
}
