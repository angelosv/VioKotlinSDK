package io.reachu.VioUI.Components.compose.checkout

import androidx.compose.ui.graphics.painter.Painter
import io.reachu.VioUI.Components.CheckoutDraft
import io.reachu.VioUI.Components.VioCheckoutOverlayController

/**
 * Payment launcher abstraction so the UI module does not depend on specific SDKs.
 */
interface CheckoutPaymentLauncher {
    fun presentStripe(config: StripeSheetConfig, onResult: (CheckoutPaymentResult) -> Unit)
    fun presentKlarnaNative(config: KlarnaNativeConfig, onResult: (CheckoutPaymentResult) -> Unit)
    fun presentVipps(onResult: (CheckoutPaymentResult) -> Unit)
}

data class StripeSheetConfig(
    val clientSecret: String,
    val customerConfig: CustomerConfig,
) {
    data class CustomerConfig(val id: String, val ephemeralKey: String)
}

data class KlarnaNativeConfig(
    val clientToken: String,
    val category: String?,
)

enum class CheckoutPaymentResult {
    Completed,
    Canceled,
    Failed,
}

data class CheckoutAssets(
    val stripeIcon: Painter? = null,
    val klarnaIcon: Painter? = null,
    val vippsIcon: Painter? = null,
)

interface CheckoutDeepLinkObserver {
    fun observe(onEvent: (DeepLinkEvent) -> Unit)
}

data class DeepLinkEvent(val status: Status) {
    enum class Status { Success, Cancel }
}

data class CheckoutUiState(
    val currentStep: VioCheckoutOverlayController.CheckoutStep = VioCheckoutOverlayController.CheckoutStep.OrderSummary,
    val selectedPaymentMethod: VioCheckoutOverlayController.PaymentMethod = VioCheckoutOverlayController.PaymentMethod.Stripe,
    val draft: CheckoutDraft = CheckoutDraft(),
)
