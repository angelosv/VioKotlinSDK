package io.reachu.sdk.modules.checkout

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.errors.ValidationException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.CheckoutGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.core.validation.Validation
import io.reachu.sdk.domain.models.CreateCheckoutDto
import io.reachu.sdk.domain.models.GetCheckoutDto
import io.reachu.sdk.domain.models.RemoveCheckoutDto
import io.reachu.sdk.domain.models.UpdateCheckoutDto
import io.reachu.sdk.domain.repositories.CheckoutRepository

class CheckoutRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : CheckoutRepository {

    override suspend fun getById(checkoutId: String): GetCheckoutDto {
        Validation.requireNonEmpty(checkoutId, "checkout_id")
        val response = client.runQuerySafe(
            CheckoutGraphQL.GET_BY_ID_CHECKOUT_QUERY,
            mapOf("checkoutId" to checkoutId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Checkout", "GetCheckout"))
            ?: throw SdkException("Empty response in Checkout.getById", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON(data)
    }

    override suspend fun create(cartId: String): CreateCheckoutDto {
        Validation.requireNonEmpty(cartId, "cart_id")
        val response = client.runMutationSafe(
            CheckoutGraphQL.CREATE_CHECKOUT_MUTATION,
            mapOf("cartId" to cartId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Checkout", "CreateCheckout"))
            ?: throw SdkException("Empty response in Checkout.create", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON(data)
    }

    override suspend fun update(
        checkoutId: String,
        status: String?,
        email: String?,
        successUrl: String?,
        cancelUrl: String?,
        paymentMethod: String?,
        shippingAddress: Map<String, Any?>?,
        billingAddress: Map<String, Any?>?,
        buyerAcceptsTermsConditions: Boolean,
        buyerAcceptsPurchaseConditions: Boolean,
    ): UpdateCheckoutDto {
        Validation.requireNonEmpty(checkoutId, "checkout_id")
        if (email != null && email.trim().isEmpty()) {
            throw ValidationException("email cannot be empty", details = mapOf("field" to "email"))
        }
        if (paymentMethod != null && paymentMethod.trim().isEmpty()) {
            throw ValidationException(
                "payment_method cannot be empty",
                details = mapOf("field" to "payment_method"),
            )
        }
        if (shippingAddress != null && shippingAddress.isEmpty()) {
            throw ValidationException(
                "shipping_address cannot be empty",
                details = mapOf("field" to "shipping_address"),
            )
        }
        if (billingAddress != null && billingAddress.isEmpty()) {
            throw ValidationException(
                "billing_address cannot be empty",
                details = mapOf("field" to "billing_address"),
            )
        }

        val variables = buildMap<String, Any?> {
            put("checkoutId", checkoutId)
            put("status", status)
            put("email", email)
            put("successUrl", successUrl)
            put("cancelUrl", cancelUrl)
            put("paymentMethod", paymentMethod)
            put("shippingAddress", shippingAddress)
            put("billingAddress", billingAddress)
            put("buyerAcceptsTermsConditions", buyerAcceptsTermsConditions)
            put("buyerAcceptsPurchaseConditions", buyerAcceptsPurchaseConditions)
        }.filterValues { it != null }

        val response = client.runMutationSafe(
            CheckoutGraphQL.UPDATE_CHECKOUT_MUTATION,
            variables,
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Checkout", "UpdateCheckout"))
            ?: throw SdkException("Empty response in Checkout.update", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON(data)
    }

    override suspend fun delete(checkoutId: String): RemoveCheckoutDto {
        Validation.requireNonEmpty(checkoutId, "checkout_id")
        val response = client.runMutationSafe(
            CheckoutGraphQL.DELETE_CHECKOUT_MUTATION,
            mapOf("checkoutId" to checkoutId),
        )
        val data: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Checkout", "RemoveCheckout"))
            ?: throw SdkException("Empty response in Checkout.delete", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON(data)
    }
}
