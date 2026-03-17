package live.vio.VioUI.Managers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import live.vio.VioCore.models.LiveShowCartManaging
import live.vio.VioCore.models.Product as CoreProduct
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.domain.repositories.CartRepository
import live.vio.sdk.domain.repositories.CheckoutRepository
import live.vio.sdk.domain.repositories.DiscountRepository
import live.vio.sdk.domain.repositories.MarketRepository
import live.vio.sdk.domain.repositories.PaymentRepository
import live.vio.sdk.domain.repositories.ProductRepository
import java.net.URL
import live.vio.VioCore.configuration.VioConfiguration as CoreVioConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    internal suspend fun waitForRemoteConfig() {
        CoreVioConfiguration.waitForRemoteConfig("CartManager")
    }

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
    var shippingAddress: Map<String, Any?>? by mutableStateOf(null)
    var billingAddress: Map<String, Any?>? by mutableStateOf(null)
    var customerEmail: String? by mutableStateOf(null)
    internal var trustBackendTotals: Boolean = false

    internal var currentCartId: String? = null
    internal var pendingShippingSelections: MutableMap<String, CartItem.ShippingOption> = mutableMapOf()
    var isMarketReady: Boolean by mutableStateOf(false)
    internal var isLoadingMarkets: Boolean = false
    internal var activeProductRequestID: String? = null
    internal var lastLoadedProductCurrency: String? = null
    internal var lastLoadedProductCountry: String? = null
    internal var didLoadInitialProducts: Boolean = false

    private val constructorProvidedSdk: CartManagingSDK? = sdk
    private var cachedSdk: CartManagingSDK? = null
    private var cachedSdkSignature: String? = null

    internal val sdk: CartManagingSDK
        get() {
            constructorProvidedSdk?.let { return it }
            val cfg = CoreVioConfiguration.shared.state.value
            val endpoint = cfg.commerce?.endpoint?.takeIf { it.isNotBlank() } ?: cfg.environment.graphQLUrl
            println("****** endpoint ${endpoint}")
            val apiKeyForCommerce = (cfg.commerce?.apiKey?.takeIf { it.isNotBlank() } ?: cfg.apiKey).ifBlank { "DEMO_KEY" }
            println("****** apiKeyForCommerce ${apiKeyForCommerce}")
            val overrideBase = System.getenv("REACHU_BASE_URL")?.trim()?.takeIf { it.isNotBlank() }
            val overrideToken = System.getenv("REACHU_API_TOKEN")?.trim()?.takeIf { it.isNotBlank() }
            val signature = "${overrideBase ?: endpoint}|${overrideToken ?: apiKeyForCommerce}"
            if (cachedSdk == null || cachedSdkSignature != signature) {
                cachedSdkSignature = signature
                val baseUrl = URL(overrideBase ?: endpoint)
                val apiKey = (overrideToken ?: apiKeyForCommerce).ifBlank { "DEMO_KEY" }
                cachedSdk = VioSdkClient(baseUrl = baseUrl, apiKey = apiKey).asCartManagingSDK()
            }
            return cachedSdk!!
        }

    init {
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

        // Listen for remote SDK configuration updates
        scope.launch {
            live.vio.VioCore.managers.CampaignManager.shared.events.collect { event ->
                if (event is live.vio.VioCore.managers.CampaignNotification.CommerceConfigChanged) {
                    println("➡️ [CartManager] Detected commerce config change, invalidating SDK client")
                    cachedSdk = null
                    cachedSdkSignature = null
                    
                    // If we haven't successfully created a cart yet, retry automatically
                    if (cartId == null && autoBootstrap) {
                        try {
                            createCart(currency = currency, country = country)
                        } catch (t: Throwable) {
                            logError("retryCreateCart", t)
                        }
                    }
                    
                    // Reload products with the new API key
                    try {
                        reloadProducts()
                    } catch (t: Throwable) {
                        logError("reloadProducts", t)
                    }
                }
            }
        }
    }

    override suspend fun addProduct(product: CoreProduct, quantity: Int) {
        // Delegate to top-level helper to avoid recursion with the extension name.
        val mapped = product.toCartProduct()
        live.vio.VioUI.Managers.addProductInternal(this, mapped, quantity)
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
    private val delegate = live.vio.VioDesignSystem.Components.VioToastManager
    fun showSuccess(message: String) = delegate.showSuccess(message)
    fun showError(message: String) = delegate.showError(message)
    fun showInfo(message: String) = delegate.showInfo(message)
    fun showWarning(message: String) = delegate.showWarning(message)
}
