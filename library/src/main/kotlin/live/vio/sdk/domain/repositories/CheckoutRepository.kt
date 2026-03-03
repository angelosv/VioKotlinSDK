package live.vio.sdk.domain.repositories

import live.vio.sdk.domain.models.CreateCheckoutDto
import live.vio.sdk.domain.models.GetCheckoutDto
import live.vio.sdk.domain.models.RemoveCheckoutDto
import live.vio.sdk.domain.models.UpdateCheckoutDto

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
