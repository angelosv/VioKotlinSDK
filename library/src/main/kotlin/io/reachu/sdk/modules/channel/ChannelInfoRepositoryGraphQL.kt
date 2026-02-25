package io.reachu.sdk.modules.channel

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.ChannelGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.domain.models.GetChannelsDto
import io.reachu.sdk.domain.models.GetTermsAndConditionsDto
import io.reachu.sdk.domain.repositories.ChannelInfoRepository

class ChannelInfoRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : ChannelInfoRepository {

    override suspend fun getChannels(): List<GetChannelsDto> {
        val response = client.runQuerySafe(ChannelGraphQL.GET_CHANNELS_CHANNEL_QUERY, emptyMap())
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Channel", "GetChannels"))
            ?: throw SdkException("Empty response in Channel.getChannels", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetChannelsDto>>(list)
    }

    override suspend fun getPurchaseConditions(): GetTermsAndConditionsDto {
        val response = client.runQuerySafe(
            ChannelGraphQL.GET_PURCHASE_CONDITIONS_CHANNEL_QUERY,
            emptyMap(),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(
            response.data,
            listOf("Channel", "GetPurchaseConditions"),
        ) ?: throw SdkException(
            "Empty response in Channel.getPurchaseConditions",
            code = "EMPTY_RESPONSE",
        )
        return GraphQLPick.decodeJSON<GetTermsAndConditionsDto>(obj)
    }

    override suspend fun getTermsAndConditions(): GetTermsAndConditionsDto {
        val response = client.runQuerySafe(
            ChannelGraphQL.GET_TERMS_AND_CONDITIONS_CHANNEL_QUERY,
            emptyMap(),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(
            response.data,
            listOf("Channel", "GetTermsAndConditions"),
        ) ?: throw SdkException(
            "Empty response in Channel.getTermsAndConditions",
            code = "EMPTY_RESPONSE",
        )
        return GraphQLPick.decodeJSON<GetTermsAndConditionsDto>(obj)
    }
}
