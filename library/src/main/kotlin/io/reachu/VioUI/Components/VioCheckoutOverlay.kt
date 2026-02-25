package io.reachu.VioUI.Components

import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.configuration.VioEnvironment
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Market
import io.reachu.VioUI.Managers.ToastManager
import io.reachu.VioUI.Managers.getAvailablePaymentMethodNames
import io.reachu.VioUI.Managers.createCheckout
import io.reachu.VioUI.Managers.discountApply
import io.reachu.VioUI.Managers.discountApplyOrCreate
import io.reachu.VioUI.Managers.discountRemoveApplied
import io.reachu.VioUI.Managers.initKlarna
import io.reachu.VioUI.Managers.initKlarnaNative
import io.reachu.VioUI.Managers.klarnaNativeOrder
import io.reachu.VioUI.Managers.confirmKlarnaNative
import io.reachu.VioUI.Managers.loadMarketsIfNeeded
import io.reachu.VioUI.Managers.loadProductsIfNeeded
import io.reachu.VioUI.Managers.selectMarket
import io.reachu.VioUI.Managers.stripeIntent
import io.reachu.VioUI.Managers.stripeLink
import io.reachu.VioUI.Managers.updateCheckout
import io.reachu.VioUI.Managers.resetCartAndCreateNew
import io.reachu.VioUI.Managers.refreshCheckoutTotals
import io.reachu.sdk.domain.models.ConfirmPaymentKlarnaNativeDto
import io.reachu.sdk.domain.models.InitPaymentKlarnaDto
import io.reachu.sdk.domain.models.InitPaymentKlarnaNativeDto
import io.reachu.sdk.domain.models.InitPaymentStripeDto
import io.reachu.sdk.domain.models.KlarnaNativeAddressInputDto
import io.reachu.sdk.domain.models.KlarnaNativeCustomerInputDto
import io.reachu.sdk.domain.models.KlarnaNativeInitInputDto
import io.reachu.sdk.domain.models.KlarnaNativeOrderDto
import io.reachu.sdk.domain.models.PaymentIntentStripeDto
import io.reachu.sdk.domain.models.UpdateCheckoutDto
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.reachu.VioCore.configuration.VioConfiguration as CoreVioConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CheckoutPrefill(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val phoneCountryCode: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val zip: String? = null,
)

/**
 * Controller counterpart of the SwiftUI checkout overlay.  It encapsulates the
 * state machine and operations so that a Compose UI (or any other presentation
 * layer) can consume it without duplicating logic.
 */
class VioCheckoutOverlayController(
    private val cartManager: CartManager,
    private val checkoutDraft: CheckoutDraft,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val prefill: CheckoutPrefill? = null,
) {

    enum class CheckoutStep {
        OrderSummary,
        Review,
        Processing,
        Success,
        Error,
    }

    enum class PaymentMethod(val displayName: String) {
        Stripe("Credit Card"),
        Klarna("Pay with Klarna"),
        Vipps("Vipps"),
    }

    var currentStep by mutableStateOf(CheckoutStep.OrderSummary)
        internal set
    var selectedPaymentMethod: PaymentMethod = PaymentMethod.Stripe
        private set

    var isEditingAddress: Boolean = false
        private set
    var discountCodeInput: String = ""
    var discountMessage: String = ""
        private set
    var isApplyingDiscount: Boolean = false
        private set
    var isPlacingOrder: Boolean = false
        private set

    // Allowed methods after intersecting config and backend
    var allowedPaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Stripe, PaymentMethod.Klarna, PaymentMethod.Vipps)
        private set

    init {
        applyInitialData(prefill)
        scope.launch {
            cartManager.loadMarketsIfNeeded()
            cartManager.loadProductsIfNeeded()
            syncSelectedMarket()
            // Determine allowed payment methods
            resolveAllowedPaymentMethods()
            // Ensure current selection is valid
            if (!allowedPaymentMethods.contains(selectedPaymentMethod) && allowedPaymentMethods.isNotEmpty()) {
                selectedPaymentMethod = allowedPaymentMethods.first()
            }
            cartManager.refreshCheckoutTotals()
        }
    }

    fun goToStep(step: CheckoutStep) {
        currentStep = step
    }

    fun goToNextStep() {
        currentStep = when (currentStep) {
            CheckoutStep.OrderSummary -> CheckoutStep.Review
            CheckoutStep.Review -> CheckoutStep.Success
            CheckoutStep.Processing -> CheckoutStep.Success
            CheckoutStep.Success -> CheckoutStep.Success
            CheckoutStep.Error -> CheckoutStep.OrderSummary
        }
    }

    fun goToPreviousStep() {
        currentStep = when (currentStep) {
            CheckoutStep.OrderSummary -> CheckoutStep.OrderSummary
            CheckoutStep.Review -> CheckoutStep.OrderSummary
            CheckoutStep.Processing -> CheckoutStep.Review
            CheckoutStep.Success,
            CheckoutStep.Error -> CheckoutStep.OrderSummary
        }
    }

    fun reset() {
        currentStep = CheckoutStep.OrderSummary
        selectedPaymentMethod = PaymentMethod.Stripe
        discountCodeInput = ""
        discountMessage = ""
        isApplyingDiscount = false
        isPlacingOrder = false
    }

    private fun applyInitialData(prefill: CheckoutPrefill?) {
        val state = CoreVioConfiguration.shared.state.value
        val isDevEnv = state.environment == VioEnvironment.DEVELOPMENT || state.environment == VioEnvironment.SANDBOX
        val marketState = state.market
        val fallbackMarket = cartManager.selectedMarket ?: Market(
            code = marketState.countryCode,
            name = marketState.countryName,
            officialName = marketState.countryName,
            flagURL = marketState.flagURL,
            phoneCode = marketState.phoneCode,
            currencyCode = marketState.currencyCode,
            currencySymbol = marketState.currencySymbol,
        )

        fun resolved(value: String?, demo: String? = null): String {
            return when {
                !value.isNullOrBlank() -> value
                isDevEnv && !demo.isNullOrBlank() -> demo
                else -> ""
            }
        }

        checkoutDraft.firstName = resolved(prefill?.firstName, "John")
        checkoutDraft.lastName = resolved(prefill?.lastName, "Doe")
        checkoutDraft.email = resolved(prefill?.email, "john.doe@example.com")
        checkoutDraft.phone = resolved(prefill?.phone, "2125551212")
        checkoutDraft.address1 = resolved(prefill?.address1, "5th Avenue 1")
        checkoutDraft.address2 = resolved(prefill?.address2)
        checkoutDraft.city = resolved(prefill?.city, "New York")
        checkoutDraft.province = resolved(prefill?.province, "NY")
        checkoutDraft.zip = resolved(prefill?.zip, "10001")

        val phoneCode = prefill?.phoneCountryCode
            ?: fallbackMarket.phoneCode
            ?: marketState.phoneCode
            ?: "+1"
        checkoutDraft.phoneCountryCode = if (phoneCode.startsWith("+")) phoneCode else "+$phoneCode"

        checkoutDraft.countryName = when {
            !prefill?.country.isNullOrBlank() -> prefill.country
            !fallbackMarket.name.isNullOrBlank() -> fallbackMarket.name
            else -> marketState.countryName
        }
        checkoutDraft.countryCode = when {
            !prefill?.countryCode.isNullOrBlank() -> prefill.countryCode.uppercase()
            !fallbackMarket.code.isNullOrBlank() -> fallbackMarket.code.uppercase()
            else -> marketState.countryCode
        }
    }

    fun toggleEditAddress() {
        isEditingAddress = !isEditingAddress
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        selectedPaymentMethod = method
    }

    fun isMethodAllowed(method: PaymentMethod): Boolean = allowedPaymentMethods.contains(method)

    private suspend fun resolveAllowedPaymentMethods() {
        val configLower = CoreVioConfiguration.shared.state.value.cart.supportedPaymentMethods.map { it.lowercase() }
        val backendLower = runCatching { cartManager.getAvailablePaymentMethodNames().map { it.lowercase() } }.getOrElse { emptyList() }

        val mapping: (String) -> PaymentMethod? = { name ->
            when (name.lowercase()) {
                "stripe" -> PaymentMethod.Stripe
                "klarna" -> PaymentMethod.Klarna
                "vipps" -> PaymentMethod.Vipps
                else -> null
            }
        }

        // Prefer explicit config; if empty, fallback to backend; else default list
        val allowedLower = when {
            configLower.isNotEmpty() -> configLower
            backendLower.isNotEmpty() -> backendLower
            else -> listOf("stripe", "klarna", "vipps")
        }

        val orderSource = if (configLower.isNotEmpty()) configLower else allowedLower
        allowedPaymentMethods = orderSource.mapNotNull(mapping).filter { allowedLower.contains(it.name.lowercase()) }
    }

    fun syncSelectedMarket() {
        val market = cartManager.selectedMarket
        if (market != null) {
            applyMarketToDraft(market)
        }
    }

    fun updatePhoneCodeFromCart() {
        checkoutDraft.phoneCountryCode = cartManager.phoneCode
    }

    fun updateAddressDraft(
        firstName: String? = null,
        lastName: String? = null,
        address1: String? = null,
        address2: String? = null,
        city: String? = null,
        province: String? = null,
        zip: String? = null,
        phone: String? = null,
        email: String? = null,
        company: String? = null,
    ) {
        firstName?.let { checkoutDraft.firstName = it }
        lastName?.let { checkoutDraft.lastName = it }
        address1?.let { checkoutDraft.address1 = it }
        address2?.let { checkoutDraft.address2 = it }
        city?.let { checkoutDraft.city = it }
        province?.let { checkoutDraft.province = it }
        zip?.let { checkoutDraft.zip = it }
        phone?.let { checkoutDraft.phone = it }
        email?.let { checkoutDraft.email = it }
        company?.let { checkoutDraft.company = it }
    }

    fun applyDiscount(code: String = discountCodeInput) {
        val normalized = code.trim()
        if (normalized.isEmpty()) return
        discountCodeInput = normalized
        isApplyingDiscount = true
        scope.launch {
            val applied = cartManager.discountApply(normalized)
            discountMessage = if (applied) {
                discountCodeInput = ""
                cartManager.refreshCheckoutTotals()
                "Discount applied: $normalized"
            } else {
                cartManager.errorMessage ?: "Discount not applied"
            }
            isApplyingDiscount = false
        }
    }

    fun applyDiscountOrCreate(
        code: String,
        percentage: Int = 10,
        typeId: Int = 2,
    ) {
        val normalized = code.trim()
        if (normalized.isEmpty()) return
        discountCodeInput = normalized
        isApplyingDiscount = true
        scope.launch {
            val applied = cartManager.discountApplyOrCreate(
                code = normalized,
                percentage = percentage,
                typeId = typeId,
            )
            discountMessage = if (applied) {
                discountCodeInput = ""
                cartManager.refreshCheckoutTotals()
                "Discount applied: $normalized"
            } else {
                cartManager.errorMessage ?: "Failed to apply discount"
            }
            isApplyingDiscount = false
        }
    }

    fun removeDiscount(code: String? = cartManager.lastDiscountCode) {
        val normalized = code?.trim().orEmpty()
        if (normalized.isEmpty()) return
        isApplyingDiscount = true
        scope.launch {
            val removed = cartManager.discountRemoveApplied(normalized)
            discountMessage = if (removed) {
                cartManager.refreshCheckoutTotals()
                "Discount removed: $normalized"
            } else {
                cartManager.errorMessage ?: "Failed to remove discount"
            }
            isApplyingDiscount = false
        }
    }

    fun proceedToPayment(
        advanceToReview: Boolean = true,
        onResult: (Result<String?>) -> Unit = {},
    ) {
        if (isPlacingOrder) return
        applyDraftSanitization()
        isPlacingOrder = true
        scope.launch {
            val checkoutId = cartManager.createCheckout()
            isPlacingOrder = false
            if (checkoutId != null) {
                cartManager.refreshCheckoutTotals()
                onResult(Result.success(checkoutId))
                AnalyticsManager.trackCheckoutStarted(
                    checkoutId = checkoutId,
                    cartValue = cartManager.cartTotal,
                    currency = cartManager.currency,
                    productCount = cartManager.items.size,
                    userEmail = checkoutDraft.email.takeIf { it.isNotBlank() },
                    userFirstName = checkoutDraft.firstName.takeIf { it.isNotBlank() },
                    userLastName = checkoutDraft.lastName.takeIf { it.isNotBlank() },
                )
                if (advanceToReview) {
                    goToStep(CheckoutStep.Review)
                }
            } else {
                onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Checkout creation failed")))
                if (advanceToReview) {
                    goToStep(CheckoutStep.Error)
                }
            }
        }
    }

    fun updateCheckout(
        paymentMethod: PaymentMethod = selectedPaymentMethod,
        advanceToSuccess: Boolean = true,
        status: String? = null,
        onResult: (Result<UpdateCheckoutDto?>) -> Unit = {},
    ) {
        applyDraftSanitization()
        scope.launch(cartManager.workerDispatcher) {
            val dto = cartManager.updateCheckout(
                checkoutId = cartManager.checkoutId,
                email = checkoutDraft.email,
                successUrl = checkoutDraft.successUrl,
                cancelUrl = checkoutDraft.cancelUrl,
                paymentMethod = paymentMethod.name.lowercase(Locale.ROOT),
                shippingAddress = checkoutDraft.shippingAddressPayload(cartManager.country),
                billingAddress = checkoutDraft.billingAddressPayload(cartManager.country),
                acceptsTerms = true,
                acceptsPurchaseConditions = true,
                status = status,
            )
            cartManager.mainCall {
                if (dto != null) {
                    onResult(Result.success(dto))
                    ToastManager.showSuccess("Checkout updated")
                    if (advanceToSuccess) {
                        val checkoutIdForAnalytics = cartManager.checkoutId
                        val productsPayload = cartManager.items.map { item ->
                            mapOf(
                                "product_id" to item.productId.toString(),
                                "product_name" to item.title,
                                "quantity" to item.quantity,
                                "price" to item.price,
                                "currency" to item.currency,
                                "variant_id" to item.variantId,
                            )
                        }
                        val revenue = cartManager.cartTotal
                        val currency = cartManager.currency
                        val shippingTotal = cartManager.shippingTotal.takeIf { it > 0 }

                        goToStep(CheckoutStep.Success)
                        scope.launch { cartManager.resetCartAndCreateNew() }
                        checkoutIdForAnalytics?.let { checkoutId ->
                            AnalyticsManager.trackTransaction(
                                checkoutId = checkoutId,
                                revenue = revenue,
                                currency = currency,
                                paymentMethod = paymentMethod.name.lowercase(Locale.ROOT),
                                products = productsPayload,
                                discount = null,
                                shipping = shippingTotal,
                                tax = null,
                            )
                        }
                    }
                } else {
                    val error = cartManager.errorMessage ?: "Checkout update failed"
                    onResult(Result.failure(IllegalStateException(error)))
                    if (advanceToSuccess) {
                        goToStep(CheckoutStep.Error)
                    }
                }
            }
        }
    }

    fun initKlarna(
        countryCode: String,
        href: String,
        email: String?,
        onResult: (Result<InitPaymentKlarnaDto?>) -> Unit = {},
    ) {
        scope.launch {
            val dto = cartManager.initKlarna(countryCode, href, email)
            if (dto != null) {
                onResult(Result.success(dto))
            } else {
                onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Klarna init failed")))
            }
        }
    }

    fun initKlarnaNative(
        input: KlarnaNativeInitInputDto,
        onResult: (Result<InitPaymentKlarnaNativeDto?>) -> Unit = {},
    ) {
        scope.launch {
            val dto = cartManager.initKlarnaNative(input)
            if (dto != null) {
                onResult(Result.success(dto))
            } else {
                onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Klarna native init failed")))
            }
        }
    }

    fun confirmKlarnaNative(
        authorizationToken: String,
        autoCapture: Boolean? = null,
        customer: KlarnaNativeCustomerInputDto? = null,
        billingAddress: KlarnaNativeAddressInputDto? = null,
        shippingAddress: KlarnaNativeAddressInputDto? = null,
        onResult: (Result<ConfirmPaymentKlarnaNativeDto?>) -> Unit = {},
    ) {
        scope.launch {
            val dto = cartManager.confirmKlarnaNative(
                authorizationToken = authorizationToken,
                autoCapture = autoCapture,
                customer = customer,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
            )
            if (dto != null) {
                onResult(Result.success(dto))
            } else {
                onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Klarna confirm failed")))
            }
        }
    }

    fun fetchKlarnaOrder(
        orderId: String,
        userId: String? = null,
        onResult: (Result<KlarnaNativeOrderDto?>) -> Unit = {},
    ) {
        scope.launch {
            val dto = cartManager.klarnaNativeOrder(orderId, userId)
            if (dto != null) {
                onResult(Result.success(dto))
            } else {
                onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Klarna order failed")))
            }
        }
    }

    fun requestStripeIntent(
        returnEphemeralKey: Boolean? = true,
        onResult: (Result<PaymentIntentStripeDto?>) -> Unit = {},
    ) {
        scope.launch(cartManager.workerDispatcher) {
            val dto = cartManager.stripeIntent(returnEphemeralKey)
            cartManager.mainCall {
                if (dto != null) {
                    onResult(Result.success(dto))
                } else {
                    onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Stripe intent failed")))
                }
            }
        }
    }

    fun requestStripeLink(
        successUrl: String,
        paymentMethod: String,
        email: String,
        onResult: (Result<InitPaymentStripeDto?>) -> Unit = {},
    ) {
        scope.launch(cartManager.workerDispatcher) {
            val dto = cartManager.stripeLink(successUrl, paymentMethod, email)
            cartManager.mainCall {
                if (dto != null) {
                    onResult(Result.success(dto))
                } else {
                    onResult(Result.failure(IllegalStateException(cartManager.errorMessage ?: "Stripe link failed")))
                }
            }
        }
    }

    fun pickMarket(market: Market) {
        scope.launch(cartManager.workerDispatcher) {
            cartManager.selectMarket(market)
            cartManager.mainCall { applyMarketToDraft(market) }
        }
    }

    // UI rendering is delegated to the presentation layer (e.g., Compose).

    private fun applyMarketToDraft(market: Market) {
        checkoutDraft.countryCode = market.code
        checkoutDraft.countryName = market.name
        checkoutDraft.phoneCountryCode = market.phoneCode
    }

    private fun applyDraftSanitization() {
        checkoutDraft.apply {
            email = email.trim()
            phone = phone.trim()
            firstName = firstName.trim()
            lastName = lastName.trim()
            address1 = address1.trim()
            address2 = address2.trim()
            city = city.trim()
            province = province.trim()
            zip = zip.trim()
            company = company.trim()
        }
    }
}
