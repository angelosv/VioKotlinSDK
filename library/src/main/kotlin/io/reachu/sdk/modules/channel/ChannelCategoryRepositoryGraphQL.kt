package io.reachu.sdk.modules.channel

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.ChannelGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.domain.models.GetCategoryDto
import io.reachu.sdk.domain.repositories.ChannelCategoryRepository

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
