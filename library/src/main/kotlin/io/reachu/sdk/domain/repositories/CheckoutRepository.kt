package io.reachu.sdk.domain.repositories

import io.reachu.sdk.domain.models.CreateCheckoutDto
import io.reachu.sdk.domain.models.GetCheckoutDto
import io.reachu.sdk.domain.models.RemoveCheckoutDto
import io.reachu.sdk.domain.models.UpdateCheckoutDto

interface CheckoutRepository {
    suspend fun getById(checkoutId: String): GetCheckoutDto
    suspend fun create(cartId: String): CreateCheckoutDto
    suspend fun update(
        checkoutId: String,
        status: String? = null,
        email: String? = null,
        successUrl: String? = null,
        cancelUrl: String? = null,
        paymentMethod: String? = null,
        shippingAddress: Map<String, Any?>? = null,
        billingAddress: Map<String, Any?>? = null,
        buyerAcceptsTermsConditions: Boolean = true,
        buyerAcceptsPurchaseConditions: Boolean = true,
    ): UpdateCheckoutDto
    suspend fun delete(checkoutId: String): RemoveCheckoutDto
}
