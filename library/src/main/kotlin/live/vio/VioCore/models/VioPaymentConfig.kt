package live.vio.VioCore.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 * Configuration for checkout and payment methods.
 * Mirrors the Swift `CheckoutConfig`.
 */
@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class CheckoutConfig(
    @JsonProperty("paymentMethods") val paymentMethods: List<VioPaymentMethod> = emptyList()
) {
    val hasGooglePay: Boolean get() = paymentMethods.contains(VioPaymentMethod.GOOGLE_PAY) || paymentMethods.contains(VioPaymentMethod.APPLE_PAY)
    val hasKlarna: Boolean get() = paymentMethods.contains(VioPaymentMethod.KLARNA)
    val hasVipps: Boolean get() = paymentMethods.contains(VioPaymentMethod.VIPPS)
    val hasStripe: Boolean get() = paymentMethods.contains(VioPaymentMethod.STRIPE)
}
