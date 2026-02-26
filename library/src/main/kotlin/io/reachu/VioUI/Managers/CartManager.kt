package io.reachu.VioUI.Managers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.VioCore.models.Product as CoreProduct
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.domain.repositories.CartRepository
import io.reachu.sdk.domain.repositories.CheckoutRepository
import io.reachu.sdk.domain.repositories.DiscountRepository
import io.reachu.sdk.domain.repositories.MarketRepository
import io.reachu.sdk.domain.repositories.PaymentRepository
import io.reachu.sdk.domain.repositories.ProductRepository
import java.net.URL
import io.reachu.VioCore.configuration.VioConfiguration as CoreVioConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface CartManagingSDK {
    val cart: CartRepository
    val product: ProductRepository
    val checkout: CheckoutRepository
    val payment: PaymentRepository
    val discount: DiscountRepository
    val market: MarketRepository
}

fun VioSdkClient.asCartManagingSDK(): CartManagingSDK = object : CartManagingSDK {
    override val cart: CartRepository get() = this@asCartManagingSDK.cart
    override val product: ProductRepository get() = this@asCartManagingSDK.channel.product
    override val checkout: CheckoutRepository get() = this@asCartManagingSDK.checkout
    override val payment: PaymentRepository get() = this@asCartManagingSDK.payment
    override val discount: DiscountRepository get() = this@asCartManagingSDK.discount
    override val market: MarketRepository get() = this@asCartManagingSDK.market
}

class CartManager(
    sdk: CartManagingSDK? = null,
    autoBootstrap: Boolean = true,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    internal val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher),
) : LiveShowCartManaging {

    internal suspend fun <T> ioCall(block: suspend () -> T): T =
        withContext(workerDispatcher) { block() }

    internal suspend fun <T> mainCall(block: suspend () -> T): T =
        withContext(mainDispatcher) { block() }

    var items: List<CartItem> by mutableStateOf(emptyList())
    var isCheckoutPresented: Boolean by mutableStateOf(false)
    var isLoading: Boolean by mutableStateOf(false)
    var cartTotal: Double by mutableStateOf(0.0)
    var subtotal: Double by mutableStateOf(0.0)
    var discountTotal: Double by mutableStateOf(0.0)
    var taxTotal: Double by mutableStateOf(0.0)
    var totalOverride: Double? by mutableStateOf(null)
    var currency: String by mutableStateOf("USD")
    var country: String by mutableStateOf("US")
    var errorMessage: String? by mutableStateOf(null)
    var cartId: String? by mutableStateOf(null)
    var checkoutId: String? by mutableStateOf(null)
    var lastDiscountCode: String? by mutableStateOf(null)
    var lastDiscountId: Int? by mutableStateOf(null)
    var products: List<Product> by mutableStateOf(emptyList())
    var isProductsLoading: Boolean by mutableStateOf(false)
    var productsErrorMessage: String? by mutableStateOf(null)
    var shippingTotal: Double by mutableStateOf(0.0)
    var shippingCurrency: String by mutableStateOf("USD")
    var markets: List<Market> by mutableStateOf(emptyList())
    var selectedMarket: Market? by mutableStateOf(null)
    var currencySymbol: String by mutableStateOf("$")
    var phoneCode: String by mutableStateOf("+1")
    var flagURL: String? by mutableStateOf(null)
    internal var trustBackendTotals: Boolean = false

    internal var currentCartId: String? = null
    internal var pendingShippingSelections: MutableMap<String, CartItem.ShippingOption> = mutableMapOf()
    var isMarketReady: Boolean by mutableStateOf(false)
    internal var isLoadingMarkets: Boolean = false
    internal var activeProductRequestID: String? = null
    internal var lastLoadedProductCurrency: String? = null
    internal var lastLoadedProductCountry: String? = null
    internal var didLoadInitialProducts: Boolean = false

    internal val sdk: CartManagingSDK

    init {
        val providedSdk = sdk ?: run {
            val cfg = CoreVioConfiguration.shared.state.value
            val overrideBase = System.getenv("REACHU_BASE_URL")?.trim()?.takeIf { it.isNotBlank() }
            val baseUrl = URL(overrideBase ?: cfg.environment.graphQLUrl)
            val overrideToken = System.getenv("REACHU_API_TOKEN")?.trim()?.takeIf { it.isNotBlank() }
            val apiKey = (overrideToken ?: cfg.apiKey).ifBlank { "DEMO_KEY" }
            println("🔧 [CartManager] Initializing SDK Client (baseUrl=$baseUrl)")
            VioSdkClient(baseUrl = baseUrl, apiKey = apiKey).asCartManagingSDK()
        }
        this.sdk = providedSdk

        val fallback = CoreVioConfiguration.shared.state.value.market
        val fallbackMarket = Market(
            code = fallback.countryCode,
            name = fallback.countryName,
            officialName = fallback.countryName,
            flagURL = fallback.flagURL,
            phoneCode = fallback.phoneCode,
            currencyCode = fallback.currencyCode,
            currencySymbol = fallback.currencySymbol,
        )

        markets = listOf(fallbackMarket)
        selectedMarket = fallbackMarket
        country = fallback.countryCode
        currency = fallback.currencyCode
        currencySymbol = fallback.currencySymbol
        phoneCode = fallback.phoneCode
        flagURL = fallback.flagURL
        shippingCurrency = fallback.currencyCode

        if (autoBootstrap) {
            scope.launch {
                createCart(currency = currency, country = country)
                // Load markets in background without blocking initialization
                scope.launch { loadMarketsIfNeeded() }
            }
        }
    }

    override suspend fun addProduct(product: CoreProduct, quantity: Int) {
        // Delegate to top-level helper to avoid recursion with the extension name.
        val mapped = product.toCartProduct()
        io.reachu.VioUI.Managers.addProductInternal(this, mapped, quantity)
    }

    

    override fun showCheckout() {
        isCheckoutPresented = true
    }

    fun hideCheckout() {
        isCheckoutPresented = false
    }

    internal fun iso8601String(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    internal fun logRequest(action: String, payload: Any? = null) {
        if (payload != null) {
            println("➡️ [CartManager] $action request: $payload")
        } else {
            println("➡️ [CartManager] $action request")
        }
    }

    internal fun logResponse(action: String, payload: Any? = null) {
        if (payload != null) {
            println("⬅️ [CartManager] $action response: $payload")
        } else {
            println("⬅️ [CartManager] $action response")
        }
    }

    internal fun logError(action: String, error: Throwable) {
        println("❌ [CartManager] $action error: ${error.message}")
        error.printStackTrace()
    }

    // Non-suspending wrappers to avoid Compose scope cancellations
    fun addProductAsync(product: Product, quantity: Int = 1, variant: Variant? = null) {
        scope.launch(workerDispatcher) { try { addProduct(product, variant, quantity) } catch (_: Throwable) { /* handled in addProduct */ } }
    }

    fun updateQuantityAsync(item: CartItem, newQuantity: Int) {
        scope.launch(workerDispatcher) { try { updateQuantity(item, newQuantity) } catch (_: Throwable) { /* handled in updateQuantity */ } }
    }

    fun removeItemAsync(item: CartItem) {
        scope.launch(workerDispatcher) { try { removeItem(item) } catch (_: Throwable) { /* handled in removeItem */ } }
    }

    fun setShippingOption(itemId: String, optionId: String) {
        val option = items
            .firstOrNull { it.id == itemId }
            ?.availableShippings
            ?.firstOrNull { it.id == optionId }
            ?: return

        items = items.map { item ->
            if (item.id != itemId) return@map item
            item.copy(
                shippingId = option.id,
                shippingName = option.name,
                shippingDescription = option.description,
                shippingAmount = option.amount,
                shippingCurrency = option.currency,
            )
        }

        pendingShippingSelections[itemId] = option
        recalcShippingTotalsFromItems()
    }

    private fun recalcShippingTotalsFromItems() {
        var total = 0.0
        var detectedCurrency: String? = null

        items.forEach { item ->
            item.shippingAmount?.let { total += it }
            if (detectedCurrency == null && !item.shippingCurrency.isNullOrBlank()) {
                detectedCurrency = item.shippingCurrency
            }
        }

        shippingTotal = total
        shippingCurrency = detectedCurrency ?: currency
    }
}

suspend fun CartManager.getCheckoutStatus(checkoutId: String): Boolean {
    println("🧾 [Checkout] getCheckoutStatus() checkoutId=$checkoutId")
    return try {
        val result = sdk.checkout.getById(checkoutId = checkoutId)
        println("🧾 [Checkout] getCheckoutStatus() result=${result.status}")
        if (result.status == "SUCCESS") {
            println("✅ [Checkout] Payment confirmed in backend")
            true
        } else {
            println("ℹ️ [Checkout] Still unpaid: ${result.status}")
            false
        }
    } catch (t: Throwable) {
        logError("getCheckoutStatus", t)
        false
    }
}

suspend fun CartManager.refreshCheckoutTotals(): Boolean {
    val chkId = checkoutId ?: return false
    println("🧾 [Checkout] refreshing totals for $chkId")
    return try {
        val dto = sdk.checkout.getById(checkoutId = chkId)
        val totals = dto.totals ?: return false

        mainCall {
            subtotal = totals.subtotal
            shippingTotal = totals.shipping
            discountTotal = totals.discounts ?: 0.0
            taxTotal = totals.taxAmount ?: totals.taxes
            totalOverride = totals.total
            cartTotal = totals.total
            currency = totals.currencyCode.ifBlank { currency }
            trustBackendTotals = true
        }
        println("✅ [Checkout] Totals refreshed: subtotal=${subtotal} discount=${discountTotal} total=${totals.total}")
        true
    } catch (t: Throwable) {
        logError("refreshCheckoutTotals", t)
        false
    }
}

/** Adapter to the Design System toast manager */
object ToastManager {
    private val delegate = io.reachu.VioDesignSystem.Components.VioToastManager
    fun showSuccess(message: String) = delegate.showSuccess(message)
    fun showError(message: String) = delegate.showError(message)
    fun showInfo(message: String) = delegate.showInfo(message)
    fun showWarning(message: String) = delegate.showWarning(message)
}
