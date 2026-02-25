package io.reachu.sdk.modules.cart

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.CartGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.core.validation.Validation
import io.reachu.sdk.domain.models.CartDto
import io.reachu.sdk.domain.models.GetLineItemsBySupplierDto
import io.reachu.sdk.domain.models.LineItemInput
import io.reachu.sdk.domain.models.RemoveCartDto
import io.reachu.sdk.domain.repositories.CartRepository

class CartRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : CartRepository {

    override suspend fun getById(cartId: String): CartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        val response = client.runQuerySafe(
            CartGraphQL.GET_CART_QUERY,
            mapOf("cartId" to cartId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "GetCart"))
            ?: throw SdkException("Empty response in Cart.getById", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun create(
        customerSessionId: String,
        currency: String,
        shippingCountry: String?,
    ): CartDto {
        Validation.requireNonEmpty(customerSessionId, "customer_session_id")
        Validation.requireCurrency(currency)
        shippingCountry?.let { Validation.requireCountry(it) }

        val variables = buildMap {
            put("customerSessionId", customerSessionId)
            put("currency", currency)
            shippingCountry?.let { put("shippingCountry", it) }
        }

        val response = client.runMutationSafe(
            CartGraphQL.CREATE_CART_MUTATION,
            variables,
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "CreateCart"))
            ?: throw SdkException("Empty response in Cart.create", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun update(cartId: String, shippingCountry: String): CartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        Validation.requireCountry(shippingCountry)
        val response = client.runMutationSafe(
            CartGraphQL.UPDATE_CART_MUTATION,
            mapOf("cartId" to cartId, "shippingCountry" to shippingCountry),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "UpdateCart"))
            ?: throw SdkException("Empty response in Cart.update", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun delete(cartId: String): RemoveCartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        val response = client.runMutationSafe(
            CartGraphQL.DELETE_CART_MUTATION,
            mapOf("cartId" to cartId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "DeleteCart"))
            ?: throw SdkException("Empty response in Cart.delete", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<RemoveCartDto>(data)
    }

    override suspend fun addItem(cartId: String, lineItems: List<LineItemInput>): CartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        if (lineItems.isEmpty()) {
            throw io.reachu.sdk.core.errors.ValidationException(
                "line_items cannot be empty",
                details = mapOf("field" to "line_items"),
            )
        }
        val variables = mapOf(
            "cartId" to cartId,
            "lineItems" to lineItems.map { it.toJson() },
        )
        val response = client.runMutationSafe(
            CartGraphQL.ADD_ITEM_TO_CART_MUTATION,
            variables,
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "AddItem"))
            ?: throw SdkException("Empty response in Cart.addItem", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun updateItem(
        cartId: String,
        cartItemId: String,
        shippingId: String?,
        quantity: Int?,
    ): CartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        Validation.requireNonEmpty(cartItemId, "cart_item_id")
        if (quantity == null && (shippingId == null || shippingId.isEmpty())) {
            throw io.reachu.sdk.core.errors.ValidationException(
                "You must provide either quantity or shipping_id",
                details = mapOf("fields" to listOf("quantity", "shipping_id")),
            )
        }
        if (quantity != null && quantity <= 0) {
            throw io.reachu.sdk.core.errors.ValidationException(
                "quantity must be > 0",
                details = mapOf("field" to "quantity"),
            )
        }

        val variables = buildMap {
            put("cartId", cartId)
            put("cartItemId", cartItemId)
            quantity?.let { put("qty", it) }
            shippingId?.let { put("shippingId", it) }
        }

        val response = client.runMutationSafe(
            CartGraphQL.UPDATE_ITEM_TO_CART_MUTATION,
            variables,
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "UpdateItem"))
            ?: throw SdkException("Empty response in Cart.updateItem", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun deleteItem(cartId: String, cartItemId: String): CartDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        Validation.requireNonEmpty(cartItemId, "cart_item_id")
        val response = client.runMutationSafe(
            CartGraphQL.DELETE_ITEM_TO_CART_MUTATION,
            mapOf("cartId" to cartId, "cartItemId" to cartItemId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "DeleteItem"))
            ?: throw SdkException("Empty response in Cart.deleteItem", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<CartDto>(data)
    }

    override suspend fun getLineItemsBySupplier(cartId: String): List<GetLineItemsBySupplierDto> {
        Validation.requireNonEmpty(cartId, "cart_id")
        val response = client.runQuerySafe(
            CartGraphQL.GET_LINE_ITEMS_BY_SUPPLIER_QUERY,
            mapOf("cartId" to cartId),
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Cart", "GetLineItemsBySupplier"))
            ?: throw SdkException(
                "Empty response in Cart.getLineItemsBySupplier",
                code = "EMPTY_RESPONSE",
            )
        return GraphQLPick.decodeJSON<List<GetLineItemsBySupplierDto>>(list)
    }
}
