package io.reachu.sdk.domain.repositories

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

interface PaymentRepository {
    suspend fun getAvailableMethods(): List<GetAvailablePaymentMethodsDto>
    suspend fun stripeIntent(checkoutId: String, returnEphemeralKey: Boolean? = null): PaymentIntentStripeDto
    suspend fun stripeLink(
        checkoutId: String,
        successUrl: String,
        paymentMethod: String,
        email: String,
    ): InitPaymentStripeDto

    suspend fun klarnaInit(
        checkoutId: String,
        countryCode: String,
        href: String,
        email: String? = null,
    ): InitPaymentKlarnaDto

    suspend fun vippsInit(
        checkoutId: String,
        email: String,
        returnUrl: String,
    ): InitPaymentVippsDto

    suspend fun klarnaNativeInit(
        checkoutId: String,
        input: KlarnaNativeInitInputDto,
    ): InitPaymentKlarnaNativeDto

    suspend fun klarnaNativeConfirm(
        checkoutId: String,
        input: KlarnaNativeConfirmInputDto,
    ): ConfirmPaymentKlarnaNativeDto

    suspend fun klarnaNativeOrder(
        orderId: String,
        userId: String? = null,
    ): KlarnaNativeOrderDto
}
