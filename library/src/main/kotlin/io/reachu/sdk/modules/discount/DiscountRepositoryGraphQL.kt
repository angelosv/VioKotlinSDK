package io.reachu.sdk.modules.discount

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.errors.ValidationException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.DiscountGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.core.validation.Validation
import io.reachu.sdk.domain.models.AddDiscountDto
import io.reachu.sdk.domain.models.ApplyDiscountDto
import io.reachu.sdk.domain.models.DeleteAppliedDiscountDto
import io.reachu.sdk.domain.models.DeleteDiscountDto
import io.reachu.sdk.domain.models.GetDiscountByIdDto
import io.reachu.sdk.domain.models.GetDiscountTypeDto
import io.reachu.sdk.domain.models.GetDiscountsDto
import io.reachu.sdk.domain.models.UpdateDiscountDto
import io.reachu.sdk.domain.models.VerifyDiscountDto
import io.reachu.sdk.domain.repositories.DiscountRepository
import java.time.Instant
import java.time.format.DateTimeParseException

class DiscountRepositoryGraphQL(
    private val client: GraphQLHttpClient,
    private val apiKey: String,
    private val baseUrl: String = "",
) : DiscountRepository {

    override suspend fun get(): List<GetDiscountsDto> {
        val response = client.runQuerySafe(DiscountGraphQL.GET_DISCOUNT_QUERY, emptyMap())
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "GetDiscounts"))
            ?: throw SdkException("Empty response in Discount.get", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetDiscountsDto>>(list)
    }

    override suspend fun getByChannel(): List<GetDiscountsDto> {
        Validation.requireNonEmpty(apiKey, "apiKey")
        return get().filter { it.discountMetadata?.apiKey == apiKey }
    }

    override suspend fun getById(discountId: Int): GetDiscountByIdDto {
        if (discountId <= 0) {
            throw ValidationException(
                "discountId must be > 0",
                details = mapOf("field" to "discountId"),
            )
        }
        val response = client.runQuerySafe(
            DiscountGraphQL.GET_DISCOUNT_BY_ID_QUERY,
            mapOf("discountId" to discountId),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "GetDiscountsById"))
            ?: throw SdkException("Empty response in Discount.getById", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<GetDiscountByIdDto>(obj)
    }

    override suspend fun getType(id: Int?, type: String?): List<GetDiscountTypeDto> {
        if (id == null && (type == null || type.trim().isEmpty())) {
            throw ValidationException(
                "Provide at least one of: id or type",
                details = mapOf("fields" to listOf("id", "type")),
            )
        }
        if (id != null && id <= 0) {
            throw ValidationException("id must be > 0", details = mapOf("field" to "id"))
        }
        if (type != null && type.trim().isEmpty()) {
            throw ValidationException("type cannot be empty", details = mapOf("field" to "type"))
        }

        val response = client.runQuerySafe(
            DiscountGraphQL.GET_DISCOUNT_TYPE_QUERY,
            mapOf("getDiscountTypeId" to id, "type" to type).filterValues { it != null },
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "GetDiscountType"))
            ?: throw SdkException("Empty response in Discount.getType", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetDiscountTypeDto>>(list)
    }

    override suspend fun add(
        code: String,
        percentage: Int,
        startDate: String,
        endDate: String,
        typeId: Int,
    ): AddDiscountDto {
        Validation.requireNonEmpty(code, "code")
        if (percentage !in 1..100) {
            throw ValidationException("percentage must be between 1 and 100", details = mapOf("field" to "percentage"))
        }
        Validation.requireNonEmpty(startDate, "startDate")
        Validation.requireNonEmpty(endDate, "endDate")
        val start = parseIsoDate(startDate, "startDate")
        val end = parseIsoDate(endDate, "endDate")
        if (!end.isAfter(start)) {
            throw ValidationException(
                "endDate must be after startDate",
                details = mapOf("fields" to listOf("startDate", "endDate")),
            )
        }
        if (typeId <= 0) {
            throw ValidationException("typeId must be > 0", details = mapOf("field" to "typeId"))
        }

        val response = client.runMutationSafe(
            DiscountGraphQL.ADD_DISCOUNT_MUTATION,
            mapOf(
                "code" to code,
                "percentage" to percentage,
                "startDate" to startDate,
                "endDate" to endDate,
                "typeId" to typeId,
            ),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "AddDiscount"))
            ?: throw SdkException("Empty response in Discount.add", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<AddDiscountDto>(obj)
    }

    override suspend fun apply(code: String, cartId: String): ApplyDiscountDto {
        Validation.requireNonEmpty(code, "code")
        Validation.requireNonEmpty(cartId, "cartId")
        val response = client.runMutationSafe(
            DiscountGraphQL.APPLY_DISCOUNT_MUTATION,
            mapOf("code" to code, "cartId" to cartId),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "ApplyDiscount"))
            ?: throw SdkException("Empty response in Discount.apply", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<ApplyDiscountDto>(obj)
    }

    override suspend fun deleteApplied(code: String, cartId: String): DeleteAppliedDiscountDto {
        Validation.requireNonEmpty(code, "code")
        Validation.requireNonEmpty(cartId, "cartId")
        val response = client.runMutationSafe(
            DiscountGraphQL.DELETE_APPLIED_DISCOUNT_MUTATION,
            mapOf("code" to code, "cartId" to cartId),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "DeleteAppliedDiscount"))
            ?: throw SdkException("Empty response in Discount.deleteApplied", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<DeleteAppliedDiscountDto>(obj)
    }

    override suspend fun delete(discountId: Int): DeleteDiscountDto {
        if (discountId <= 0) {
            throw ValidationException(
                "discountId must be > 0",
                details = mapOf("field" to "discountId"),
            )
        }
        val response = client.runMutationSafe(
            DiscountGraphQL.DELETE_DISCOUNT_MUTATION,
            mapOf("discountId" to discountId),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "DeleteDiscount"))
            ?: throw SdkException("Empty response in Discount.delete", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<DeleteDiscountDto>(obj)
    }

    override suspend fun update(
        discountId: Int,
        code: String?,
        percentage: Int?,
        startDate: String?,
        endDate: String?,
        products: List<Int>?,
    ): UpdateDiscountDto {
        if (discountId <= 0) {
            throw ValidationException(
                "discountId must be > 0",
                details = mapOf("field" to "discountId"),
            )
        }
        if (code != null && code.trim().isEmpty()) {
            throw ValidationException("code cannot be empty", details = mapOf("field" to "code"))
        }
        if (percentage != null && percentage !in 1..100) {
            throw ValidationException("percentage must be between 1 and 100", details = mapOf("field" to "percentage"))
        }
        val start = startDate?.let {
            if (it.trim().isEmpty()) {
                throw ValidationException("startDate cannot be empty", details = mapOf("field" to "startDate"))
            }
            parseIsoDate(it, "startDate")
        }
        val end = endDate?.let {
            if (it.trim().isEmpty()) {
                throw ValidationException("endDate cannot be empty", details = mapOf("field" to "endDate"))
            }
            parseIsoDate(it, "endDate")
        }
        if (start != null && end != null && !end.isAfter(start)) {
            throw ValidationException(
                "endDate must be after startDate",
                details = mapOf("fields" to listOf("startDate", "endDate")),
            )
        }

        val variables = buildMap<String, Any?> {
            put("discountId", discountId)
            put("code", code)
            put("percentage", percentage)
            put("startDate", startDate)
            put("endDate", endDate)
            put("products", products)
        }.filterValues { it != null }

        val response = client.runMutationSafe(
            DiscountGraphQL.UPDATE_DISCOUNT_MUTATION,
            variables,
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "UpdateDiscount"))
            ?: throw SdkException("Empty response in Discount.update", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<UpdateDiscountDto>(obj)
    }

    override suspend fun verify(verifyDiscountId: Int?, code: String?): VerifyDiscountDto {
        if ((verifyDiscountId == null || verifyDiscountId <= 0)
            && (code == null || code.trim().isEmpty())
        ) {
            throw ValidationException(
                "Provide verifyDiscountId (> 0) or code",
                details = mapOf("fields" to listOf("verifyDiscountId", "code")),
            )
        }
        if (verifyDiscountId != null && verifyDiscountId <= 0) {
            throw ValidationException(
                "verifyDiscountId must be > 0",
                details = mapOf("field" to "verifyDiscountId"),
            )
        }
        if (code != null && code.trim().isEmpty()) {
            throw ValidationException("code cannot be empty", details = mapOf("field" to "code"))
        }

        val variables = mapOf("verifyDiscountId" to verifyDiscountId, "code" to code).filterValues { it != null }
        val response = client.runMutationSafe(DiscountGraphQL.VERIFY_DISCOUNT_MUTATION, variables)
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Discounts", "VerifyDiscount"))
            ?: throw SdkException("Empty response in Discount.verify", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<VerifyDiscountDto>(obj)
    }

    private fun parseIsoDate(value: String, field: String): Instant {
        return try {
            Instant.parse(value)
        } catch (_: DateTimeParseException) {
            throw ValidationException(
                "$field must be ISO-8601",
                details = mapOf("field" to field),
            )
        }
    }
}
