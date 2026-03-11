package live.vio.sdk.domain.repositories

import live.vio.sdk.domain.models.ConfirmPaymentKlarnaNativeDto
import live.vio.sdk.domain.models.GetAvailablePaymentMethodsDto
import live.vio.sdk.domain.models.InitPaymentKlarnaDto
import live.vio.sdk.domain.models.InitPaymentKlarnaNativeDto
import live.vio.sdk.domain.models.InitPaymentStripeDto
import live.vio.sdk.domain.models.InitPaymentVippsDto
import live.vio.sdk.domain.models.KlarnaNativeConfirmInputDto
import live.vio.sdk.domain.models.KlarnaNativeInitInputDto
import live.vio.sdk.domain.models.KlarnaNativeOrderDto
import live.vio.sdk.domain.models.PaymentIntentStripeDto
import live.vio.sdk.domain.models.InitGooglePayDto
import live.vio.sdk.domain.models.ConfirmGooglePayDto

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

    suspend fun googlePayInit(checkoutId: String): InitGooglePayDto
    suspend fun googlePayConfirm(checkoutId: String, googlePayToken: String): ConfirmGooglePayDto

    suspend fun klarnaNativeOrder(
        orderId: String,
        userId: String? = null,
    ): KlarnaNativeOrderDto
}
