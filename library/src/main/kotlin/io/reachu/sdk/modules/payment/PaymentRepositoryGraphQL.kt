package io.reachu.sdk.modules.payment

import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.errors.ValidationException
import io.reachu.sdk.core.graphql.GraphQLHttpClient
import io.reachu.sdk.core.graphql.operations.PaymentGraphQL
import io.reachu.sdk.core.helpers.GraphQLPick
import io.reachu.sdk.core.helpers.JsonUtils
import io.reachu.sdk.core.validation.Validation
import io.reachu.sdk.domain.models.ConfirmPaymentKlarnaNativeDto
import io.reachu.sdk.domain.models.GetAvailablePaymentMethodsDto
import io.reachu.sdk.domain.models.InitPaymentKlarnaDto
import io.reachu.sdk.domain.models.InitPaymentKlarnaNativeDto
import io.reachu.sdk.domain.models.InitPaymentStripeDto
import io.reachu.sdk.domain.models.InitPaymentVippsDto
import io.reachu.sdk.domain.models.KlarnaNativeConfirmInputDto
import io.reachu.sdk.domain.models.KlarnaNativeInitInputDto
import io.reachu.sdk.domain.models.KlarnaNativeOrderDto
import io.reachu.sdk.domain.models.PaymentIntentStripeDto
import io.reachu.sdk.domain.repositories.PaymentRepository

class PaymentRepositoryGraphQL(
    private val client: GraphQLHttpClient,
) : PaymentRepository {

    override suspend fun getAvailableMethods(): List<GetAvailablePaymentMethodsDto> {
        val response = client.runQuerySafe(
            PaymentGraphQL.GET_AVAILABLE_METHODS_PAYMENT_QUERY,
            emptyMap(),
        )
        val list: List<Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "GetAvailablePaymentMethods"))
            ?: throw SdkException("Empty response in Payment.getAvailableMethods", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<List<GetAvailablePaymentMethodsDto>>(list)
    }

    override suspend fun stripeIntent(
        checkoutId: String,
        returnEphemeralKey: Boolean?,
    ): PaymentIntentStripeDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        val variables = mapOf(
            "checkoutId" to checkoutId,
            "returnEphemeralKey" to returnEphemeralKey,
        ).filterValues { it != null }
        val response = client.runMutationSafe(
            PaymentGraphQL.STRIPE_INTENT_PAYMENT_MUTATION,
            variables,
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "CreatePaymentIntentStripe"))
            ?: throw SdkException("Empty response in Payment.stripeIntent", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<PaymentIntentStripeDto>(obj)
    }

    override suspend fun stripeLink(
        checkoutId: String,
        successUrl: String,
        paymentMethod: String,
        email: String,
    ): InitPaymentStripeDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        Validation.requireNonEmpty(successUrl, "successUrl")
        Validation.requireNonEmpty(paymentMethod, "paymentMethod")
        Validation.requireNonEmpty(email, "email")

        val response = client.runMutationSafe(
            PaymentGraphQL.STRIPE_PLATFORM_BUILDER_PAYMENT_MUTATION,
            mapOf(
                "checkoutId" to checkoutId,
                "successUrl" to successUrl,
                "paymentMethod" to paymentMethod,
                "email" to email,
            ),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "CreatePaymentStripe"))
            ?: throw SdkException("Empty response in Payment.stripeLink", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<InitPaymentStripeDto>(obj)
    }

    override suspend fun klarnaInit(
        checkoutId: String,
        countryCode: String,
        href: String,
        email: String?,
    ): InitPaymentKlarnaDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        Validation.requireCountry(countryCode)
        Validation.requireNonEmpty(href, "href")
        if (email != null && email.trim().isEmpty()) {
            throw ValidationException("email cannot be empty", details = mapOf("field" to "email"))
        }

        val response = client.runMutationSafe(
            PaymentGraphQL.KLARNA_PLATFORM_BUILDER_PAYMENT_MUTATION,
            mapOf(
                "checkoutId" to checkoutId,
                "countryCode" to countryCode,
                "href" to href,
                "email" to (email ?: ""),
            ),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "CreatePaymentKlarna"))
            ?: throw SdkException("Empty response in Payment.klarnaInit", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<InitPaymentKlarnaDto>(obj)
    }

    override suspend fun vippsInit(
        checkoutId: String,
        email: String,
        returnUrl: String,
    ): InitPaymentVippsDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        Validation.requireNonEmpty(email, "email")
        Validation.requireNonEmpty(returnUrl, "returnUrl")

        val response = client.runMutationSafe(
            PaymentGraphQL.VIPPS_PAYMENT,
            mapOf(
                "checkoutId" to checkoutId,
                "email" to email,
                "returnUrl" to returnUrl,
            ),
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "CreatePaymentVipps"))
            ?: throw SdkException("Empty response in Payment.vippsInit", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<InitPaymentVippsDto>(obj)
    }

    override suspend fun klarnaNativeInit(
        checkoutId: String,
        input: KlarnaNativeInitInputDto,
    ): InitPaymentKlarnaNativeDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        input.countryCode?.let { Validation.requireCountry(it) }
        input.currency?.let { Validation.requireCurrency(it) }
        input.returnUrl?.let {
            if (it.trim().isEmpty()) {
                throw ValidationException(
                    "returnUrl cannot be empty when provided",
                    details = mapOf("field" to "returnUrl"),
                )
            }
        }

        val variables = buildMap<String, Any?> {
            put("checkoutId", checkoutId)
            put("countryCode", input.countryCode)
            put("currency", input.currency)
            put("locale", input.locale)
            put("returnUrl", input.returnUrl)
            put("intent", input.intent)
            put("autoCapture", input.autoCapture)
            input.customer?.let { put("customer", encodeToMap(it)) }
            input.billingAddress?.let { put("billingAddress", encodeToMap(it)) }
            input.shippingAddress?.let { put("shippingAddress", encodeToMap(it)) }
        }.filterValues { it != null }

        val response = client.runMutationSafe(
            PaymentGraphQL.KLARNA_NATIVE_INIT_PAYMENT_MUTATION,
            variables,
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "CreatePaymentKlarnaNative"))
            ?: throw SdkException("Empty response in Payment.klarnaNativeInit", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<InitPaymentKlarnaNativeDto>(obj)
    }

    override suspend fun klarnaNativeConfirm(
        checkoutId: String,
        input: KlarnaNativeConfirmInputDto,
    ): ConfirmPaymentKlarnaNativeDto {
        Validation.requireNonEmpty(checkoutId, "checkoutId")
        Validation.requireNonEmpty(input.authorizationToken, "authorizationToken")

        val variables = buildMap<String, Any?> {
            put("checkoutId", checkoutId)
            put("authorizationToken", input.authorizationToken)
            put("autoCapture", input.autoCapture)
            input.customer?.let { put("customer", encodeToMap(it)) }
            input.billingAddress?.let { put("billingAddress", encodeToMap(it)) }
            input.shippingAddress?.let { put("shippingAddress", encodeToMap(it)) }
        }.filterValues { it != null }

        val response = client.runMutationSafe(
            PaymentGraphQL.KLARNA_NATIVE_CONFIRM_PAYMENT_MUTATION,
            variables,
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "ConfirmPaymentKlarnaNative"))
            ?: throw SdkException("Empty response in Payment.klarnaNativeConfirm", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<ConfirmPaymentKlarnaNativeDto>(obj)
    }

    override suspend fun klarnaNativeOrder(orderId: String, userId: String?): KlarnaNativeOrderDto {
        Validation.requireNonEmpty(orderId, "orderId")
        if (userId != null && userId.trim().isEmpty()) {
            throw ValidationException("userId cannot be empty when provided", details = mapOf("field" to "userId"))
        }

        val variables = mapOf("orderId" to orderId, "userId" to userId).filterValues { it != null }
        val response = client.runQuerySafe(
            PaymentGraphQL.KLARNA_NATIVE_ORDER_QUERY,
            variables,
        )
        val obj: Map<String, Any?> = GraphQLPick.pickPath(response.data, listOf("Payment", "GetKlarnaOrderNative"))
            ?: throw SdkException("Empty response in Payment.klarnaNativeOrder", code = "EMPTY_RESPONSE")
        return GraphQLPick.decodeJSON<KlarnaNativeOrderDto>(obj)
    }

    private fun encodeToMap(value: Any): Map<String, Any?> {
        return JsonUtils.convert<Map<String, Any?>>(value)
    }
}
