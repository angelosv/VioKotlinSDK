package io.reachu.VioUI.Managers

import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.CartDto
import io.reachu.sdk.domain.models.GetLineItemsBySupplierDto
import io.reachu.sdk.domain.models.LineItemInput
import java.util.UUID
import kotlin.math.max

private data class ShippingSyncData(
    val shippingId: String?,
    val shippingName: String?,
    val shippingDescription: String?,
    val shippingAmount: Double?,
    val shippingCurrency: String?,
    val options: List<CartItem.ShippingOption>,
)

suspend fun CartManager.createCart(currency: String = "USD", country: String = "US") {
    if (currentCartId != null) {
        println("🛒 [Cart] createCart skipped — existing cartId=${currentCartId}")
        return
    }

    if (!VioConfiguration.shared.shouldUseSDK) {
        println("⚠️ [Cart] createCart skipped — SDK disabled (market not available)")
        return
    }

    mainCall {
        isLoading = true
        errorMessage = null
    }

    val session = "android-${UUID.randomUUID()}"
    println("🛒 [Cart] createCart START  session=$session currency=$currency country=$country")
    logRequest(
        "sdk.cart.create",
        mapOf(
            "session" to session,
            "currency" to currency,
            "country" to country,
        ),
    )

    try {
        val dto = ioCall {
            sdk.cart.create(
                customerSessionId = session,
                currency = currency,
                shippingCountry = country,
            )
        }
        println(
            "✅ [Cart] createCart OK     cartId=${dto.cartId} items=${dto.lineItems.size} currency=${dto.currency}",
        )
        logResponse(
            "sdk.cart.create",
            mapOf(
                "cartId" to dto.cartId,
                "items" to dto.lineItems.size,
                "currency" to dto.currency,
            ),
        )
        mainCall { sync(dto) }
    } catch (e: SdkException) {
        errorMessage = e.messageText
        logError("sdk.cart.create", e)
        println("❌ [Cart] createCart FAIL  ${e.messageText}")
    } catch (t: Throwable) {
        errorMessage = t.message
        logError("sdk.cart.create", t)
        println("❌ [Cart] createCart FAIL  ${t.message}")
    } finally {
        mainCall { isLoading = false }
    }
}

suspend fun CartManager.loadProductsIfNeeded() {
    if (products.isNotEmpty()) return
    loadProducts()
}

suspend fun CartManager.reloadProducts() {
    loadProducts(useCache = false)
}

suspend fun CartManager.loadProducts(
    currency: String? = null,
    shippingCountryCode: String? = null,
    imageSize: String = "large",
    useCache: Boolean = true,
) {
    val requestedCurrency = currency ?: this.currency
    val requestedCountry = shippingCountryCode ?: this.country

    val shouldUseCache =
        useCache &&
            lastLoadedProductCurrency == requestedCurrency &&
            lastLoadedProductCountry == requestedCountry

    val requestId = UUID.randomUUID().toString()
    activeProductRequestID = requestId
    mainCall {
        isProductsLoading = true
        productsErrorMessage = null
    }

    try {
        logRequest(
            "sdk.product.get",
            mapOf(
                "currency" to requestedCurrency,
                "country" to requestedCountry,
                "imageSize" to imageSize,
                "useCache" to shouldUseCache,
            ),
        )
        val dtoProducts = ioCall {
            sdk.product.get(
                currency = requestedCurrency,
                imageSize = imageSize,
                barcodeList = null,
                categoryIds = null,
                productIds = null,
                skuList = null,
                useCache = shouldUseCache,
                shippingCountryCode = requestedCountry,
            )
        }

        if (activeProductRequestID != requestId) return

        mainCall {
            products = dtoProducts.map { it.toDomainProduct() }
            lastLoadedProductCurrency = requestedCurrency
            lastLoadedProductCountry = requestedCountry
            didLoadInitialProducts = true
        }
        logResponse(
            "sdk.product.get",
            mapOf(
                "count" to dtoProducts.size,
                "currency" to requestedCurrency,
                "country" to requestedCountry,
            ),
        )
    } catch (sdkError: SdkException) {
        if (activeProductRequestID != requestId) return
        mainCall { productsErrorMessage = sdkError.messageText }
        logError("sdk.product.get", sdkError)
        products = emptyList()
    } catch (t: Throwable) {
        if (activeProductRequestID != requestId) return
        mainCall { productsErrorMessage = t.message }
        logError("sdk.product.get", t)
        products = emptyList()
    } finally {
        mainCall {
            isProductsLoading = false
            activeProductRequestID = null
        }
    }
}

fun CartManager.sync(cart: CartDto) {
    currentCartId = cart.cartId
    currency = cart.currency
    country = cart.shippingCountry ?: country

    val normalizedItems = syncLineItems(cart)
    items = normalizedItems

    subtotal = cart.subtotal
    shippingTotal = cart.shipping
    
    // Once synced, we trust the backend
    trustBackendTotals = true
    
    cartTotal = totalOverride ?: (subtotal + shippingTotal - discountTotal)

    shippingCurrency =
        normalizedItems.firstOrNull { !it.shippingCurrency.isNullOrBlank() }?.shippingCurrency
            ?: cart.currency

    selectedMarket?.let { market ->
        currencySymbol = market.currencySymbol
        phoneCode = market.phoneCode
        flagURL = market.flagURL
    }
}

suspend fun CartManager.refreshShippingOptions(): Boolean {
    isLoading = true
    errorMessage = null
    try {
        val cid = ensureCartIDForCheckout()
            ?: run {
                println("ℹ️ [Cart] refreshShippingOptions: missing cartId")
                return false
            }

        logRequest("sdk.cart.getLineItemsBySupplier", mapOf("cart_id" to cid))
        val groups: List<GetLineItemsBySupplierDto> = ioCall {
            sdk.cart.getLineItemsBySupplier(cartId = cid)
        }
        logResponse("sdk.cart.getLineItemsBySupplier", mapOf("groupCount" to groups.size))

        val metadata = mutableMapOf<String, ShippingSyncData>()
        groups.forEach { group ->
            val options = group.availableShippings.orEmpty().mapNotNull { option ->
                val id = option.id ?: return@mapNotNull null
                if (id.isEmpty()) return@mapNotNull null
                val amount =
                    option.price.amountInclTaxes
                        ?: option.price.amount
                        ?: 0.0
                val currency = option.price.currencyCode ?: this.currency
                CartItem.ShippingOption(
                    id = id,
                    name = option.name ?: "Shipping",
                    description = option.description,
                    amount = amount,
                    currency = currency,
                )
            }

            group.lineItems.forEach { li ->
                if (items.none { it.id == li.id }) return@forEach
                val shipping = li.shipping
                val shippingCurrency = shipping?.price?.currencyCode ?: currency
                val shippingAmount =
                    shipping?.price?.amountInclTaxes ?: shipping?.price?.amount

                metadata[li.id] = ShippingSyncData(
                    shippingId = shipping?.id,
                    shippingName = shipping?.name,
                    shippingDescription = shipping?.description,
                    shippingAmount = shippingAmount,
                    shippingCurrency = shippingCurrency,
                    options = options,
                )
            }
        }

        if (metadata.isNotEmpty()) {
            applyShippingMetadata(metadata)
        }

        return true
    } catch (t: Throwable) {
        val msg = (t as? SdkException)?.messageText ?: t.message
        errorMessage = msg
        logError("sdk.cart.getLineItemsBySupplier", t)
        println("❌ [Cart] refreshShippingOptions FAIL $msg")
        return false
    } finally {
        isLoading = false
    }
}

fun CartManager.setShippingOption(itemId: String, optionId: String) {
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

suspend fun CartManager.applyCheapestShippingPerSupplier(): Int {
    isLoading = true
    errorMessage = null
    try {
        val cid = ensureCartIDForCheckout()
            ?: run {
                println("ℹ️ [Cart] applyCheapestShippingPerSupplier: missing cartId")
                return 0
            }

        val selections = pendingShippingSelections.toMap()
        if (selections.isEmpty()) {
            println("ℹ️ [Cart] applyCheapestShippingPerSupplier: no pending selections")
            return 0
        }

        var updatedCount = 0
        var lastResponse: CartDto? = null
        val succeeded = mutableListOf<String>()

        for ((itemId, option) in selections) {
            try {
                logRequest(
                    "sdk.cart.updateItem",
                    mapOf(
                        "cart_id" to cid,
                        "cart_item_id" to itemId,
                        "shipping_id" to option.id,
                    ),
                )
                val dto = sdk.cart.updateItem(
                    cartId = cid,
                    cartItemId = itemId,
                    shippingId = option.id,
                    quantity = null,
                )
                lastResponse = dto
                succeeded += itemId
                updatedCount += 1
                logResponse(
                    "sdk.cart.updateItem",
                    mapOf(
                        "cartId" to dto.cartId,
                        "itemCount" to dto.lineItems.size,
                        "shippingUpdatedItem" to itemId,
                    ),
                )
            } catch (sdkError: SdkException) {
                errorMessage = sdkError.messageText
                logError("sdk.cart.updateItem", sdkError)
                println("⚠️ [Cart] updateItem(shipping) failed for $itemId: ${sdkError.messageText}")
            } catch (t: Throwable) {
                errorMessage = t.message
                logError("sdk.cart.updateItem", t)
                println("⚠️ [Cart] updateItem(shipping) failed for $itemId: ${t.message}")
            }
        }

        succeeded.forEach { pendingShippingSelections.remove(it) }

        if (lastResponse != null) {
            sync(lastResponse!!)
            refreshShippingOptions()
        } else {
            recalcShippingTotalsFromItems()
        }

        println("✅ [Cart] Shipping updated for $updatedCount item(s).")
        return updatedCount
    } finally {
        isLoading = false
    }
}

private fun CartManager.applyShippingMetadata(metadata: Map<String, ShippingSyncData>) {
    if (metadata.isEmpty()) return

    items = items.map { item ->
        val info = metadata[item.id] ?: return@map item
        var updated = item.copy(
            shippingId = info.shippingId ?: item.shippingId,
            shippingName = info.shippingName ?: item.shippingName,
            shippingDescription = info.shippingDescription ?: item.shippingDescription,
            shippingAmount = info.shippingAmount ?: item.shippingAmount,
            shippingCurrency = info.shippingCurrency ?: item.shippingCurrency,
            availableShippings = if (info.options.isEmpty()) item.availableShippings else info.options,
        )

        pendingShippingSelections[item.id]?.let { pending ->
            updated = updated.copy(
                shippingId = pending.id,
                shippingName = pending.name,
                shippingDescription = pending.description,
                shippingAmount = pending.amount,
                shippingCurrency = pending.currency,
                availableShippings = if (updated.availableShippings.isEmpty()) listOf(pending) else updated.availableShippings,
            )
        }
        updated
    }

    recalcShippingTotalsFromItems()
}

suspend fun CartManager.addProduct(product: Product, quantity: Int) {
    addProduct(product, variant = null, quantity = quantity)
}

// Internal helper to call the extension from contexts where a member method
// with the same name exists and could cause recursion/ambiguity.
suspend fun addProductInternal(manager: CartManager, product: Product, quantity: Int) {
    manager.addProduct(product = product, variant = null, quantity = quantity)
}

suspend fun CartManager.addProduct(
    product: Product,
    variant: Variant? = null,
    quantity: Int = 1,
) {
    isLoading = true
    errorMessage = null
    trustBackendTotals = false

    val previousCount = itemCount
    val selectedVariant = variant ?: product.variants.firstOrNull()
    val selectedVariantId = selectedVariant?.id

    val hadExistingItem = items.any {
        it.productId == product.id && it.variantId == selectedVariantId
    }

    try {
        val cid = ensureCartIDForCheckout()
        if (cid == null) {
            addProductLocally(product, selectedVariant, quantity)
            updateCartTotal()
            ToastManager.showSuccess(
                if (hadExistingItem) "Updated ${product.title} quantity in cart"
                else "Added ${product.title} to cart",
            )
            trackProductAddedAnalytics(product, selectedVariant, quantity)
            triggerFeedbackIfNeeded(previousCount)
            return
        }

        val existingItem = items.firstOrNull {
            it.productId == product.id && it.variantId == selectedVariantId
        }

        if (existingItem != null) {
            val newQuantity = existingItem.quantity + quantity
            logRequest(
                "sdk.cart.updateItem",
                mapOf(
                    "cart_id" to cid,
                    "cart_item_id" to existingItem.id,
                    "quantity" to newQuantity,
                ),
            )
            val dto = ioCall {
                sdk.cart.updateItem(
                    cartId = cid,
                    cartItemId = existingItem.id,
                    shippingId = null,
                    quantity = newQuantity,
                )
            }
            logResponse(
                "sdk.cart.updateItem",
                mapOf("cartId" to dto.cartId, "itemCount" to dto.lineItems.size),
            )
            mainCall {
                sync(dto)
                ToastManager.showSuccess("Updated ${product.title} quantity in cart")
            }
            if (checkoutId != null) refreshCheckoutTotals()
            trackProductAddedAnalytics(product, selectedVariant, quantity)
        } else {
            val variantIdInt = selectedVariantId?.toIntOrNull()
            val line = createLineItemInput(product, selectedVariant, quantity)
            logRequest(
                "sdk.cart.addItem",
                mapOf(
                    "cart_id" to cid,
                    "productId" to product.id,
                    "variantId" to variantIdInt,
                    "quantity" to quantity,
                ),
            )
            val dto = ioCall {
                sdk.cart.addItem(
                    cartId = cid,
                    lineItems = listOf(line),
                )
            }
            logResponse(
                "sdk.cart.addItem",
                mapOf("cartId" to dto.cartId, "itemCount" to dto.lineItems.size),
            )
            mainCall {
                sync(dto)
                ToastManager.showSuccess("Added ${product.title} to cart")
            }
            if (checkoutId != null) refreshCheckoutTotals()
            trackProductAddedAnalytics(product, selectedVariant, quantity)
        }
    } catch (sdkError: SdkException) {
        errorMessage = sdkError.messageText
        logError("sdk.cart.update/addItem", sdkError)
        println("❌ [Cart] addProduct FAIL ${sdkError.messageText}")
        addProductLocally(product, selectedVariant, quantity)
        trackProductAddedAnalytics(product, selectedVariant, quantity)
        ToastManager.showWarning("Using local cart for ${product.title} (sync error)")
    } catch (t: Throwable) {
        errorMessage = t.message
        logError("sdk.cart.update/addItem", t)
        println("❌ [Cart] addProduct FAIL ${t.message}")
        addProductLocally(product, selectedVariant, quantity)
        trackProductAddedAnalytics(product, selectedVariant, quantity)
        ToastManager.showWarning("Added ${product.title} locally due to error")
    } finally {
        updateCartTotal()
        isLoading = false
        triggerFeedbackIfNeeded(previousCount)
    }
}

private fun trackProductAddedAnalytics(product: Product, variant: Variant?, quantity: Int) {
    val price = resolvePrice(variant, product)
    val currency = variant?.price?.currencyCode ?: product.price.currencyCode
    AnalyticsManager.trackProductAddedToCart(
        productId = product.id.toString(),
        productName = product.title,
        quantity = quantity,
        productPrice = price,
        productCurrency = currency,
        source = "cart_manager",
    )
}

private fun resolvePrice(variant: Variant?, product: Product): Double {
    val variantPrice = variant?.price
    return when {
        variantPrice?.amountInclTaxes != null -> variantPrice.amountInclTaxes.toDouble()
        variantPrice != null -> variantPrice.amount.toDouble()
        product.price.amountInclTaxes != null -> product.price.amountInclTaxes!!.toDouble()
        else -> product.price.amount.toDouble()
    }
}

suspend fun CartManager.removeItem(item: CartItem) {
    isLoading = true
    errorMessage = null
    trustBackendTotals = false
    var didSyncFromServer = false

    try {
        val cid = currentCartId
        if (!cid.isNullOrEmpty()) {
            logRequest(
                "sdk.cart.deleteItem",
                mapOf("cart_id" to cid, "cart_item_id" to item.id),
            )
            val dto = ioCall {
                sdk.cart.deleteItem(
                    cartId = cid,
                    cartItemId = item.id,
                )
            }
            logResponse(
                "sdk.cart.deleteItem",
                mapOf("cartId" to dto.cartId, "itemCount" to dto.lineItems.size),
            )
            mainCall { sync(dto) }
            if (checkoutId != null) refreshCheckoutTotals()
            didSyncFromServer = true
        } else {
            println("ℹ️ [Cart] removeItem: skipped SDK call (missing cartId)")
        }
    } catch (sdkError: SdkException) {
        errorMessage = sdkError.messageText
        logError("sdk.cart.deleteItem", sdkError)
        println("⚠️ [Cart] SDK.deleteItem failed: ${sdkError.messageText}")
    } catch (t: Throwable) {
        errorMessage = t.message
        logError("sdk.cart.deleteItem", t)
        println("⚠️ [Cart] SDK.deleteItem failed: ${t.message}")
    } finally {
        if (!didSyncFromServer) {
            mainCall { removeItemLocally(item) }
        }
        mainCall {
            updateCartTotal()
            ToastManager.showInfo("Removed ${item.title} from cart")
            isLoading = false
        }
    }
}

suspend fun CartManager.updateQuantity(item: CartItem, newQuantity: Int) {
    isLoading = true
    errorMessage = null
    trustBackendTotals = false
    var didSyncFromServer = false

    try {
        val cid = currentCartId
        if (!cid.isNullOrEmpty()) {
            val dto: CartDto = ioCall {
                if (newQuantity <= 0) {
                    logRequest(
                        "sdk.cart.deleteItem",
                        mapOf("cart_id" to cid, "cart_item_id" to item.id),
                    )
                    sdk.cart.deleteItem(
                        cartId = cid,
                        cartItemId = item.id,
                    )
                } else {
                    logRequest(
                        "sdk.cart.updateItem",
                        mapOf(
                            "cart_id" to cid,
                            "cart_item_id" to item.id,
                            "quantity" to newQuantity,
                        ),
                    )
                    sdk.cart.updateItem(
                        cartId = cid,
                        cartItemId = item.id,
                        shippingId = null,
                        quantity = newQuantity,
                    )
                }
            }
            logResponse(
                "sdk.cart.updateItem",
                mapOf("cartId" to dto.cartId, "itemCount" to dto.lineItems.size),
            )
            mainCall { sync(dto) }
            if (checkoutId != null) refreshCheckoutTotals()
            didSyncFromServer = true
        } else {
            println("ℹ️ [Cart] updateQuantity: skipped SDK call (missing cartId)")
        }
    } catch (sdkError: SdkException) {
        errorMessage = sdkError.messageText
        logError("sdk.cart.updateItem", sdkError)
        println("⚠️ [Cart] SDK.updateItem failed: ${sdkError.messageText}")
    } catch (t: Throwable) {
        errorMessage = t.message
        logError("sdk.cart.updateItem", t)
        println("⚠️ [Cart] SDK.updateItem failed: ${t.message}")
    } finally {
        if (!didSyncFromServer) {
            mainCall { updateQuantityLocally(item, newQuantity) }
        }
        mainCall {
            updateCartTotal()
            runCatching {
                val clamped = max(newQuantity, 0)
                val name = item.title.ifBlank { "item" }
                ToastManager.showSuccess("Updated $name to qty $clamped")
            }
            isLoading = false
        }
    }
}

suspend fun CartManager.clearCart() {
    mainCall {
        isLoading = true
        errorMessage = null
    }

    try {
        currentCartId?.let { cid ->
            logRequest("sdk.cart.delete", mapOf("cart_id" to cid))
            ioCall { sdk.cart.delete(cartId = cid) }
        }
        mainCall {
            items = emptyList()
            cartTotal = 0.0
            shippingTotal = 0.0
            shippingCurrency = currency
        }
        resetCartAndCreateNew()
    } catch (t: Throwable) {
        errorMessage = t.message
        logError("sdk.cart.delete", t)
    } finally {
        mainCall { isLoading = false }
    }
}

suspend fun CartManager.resetCartAndCreateNew() {
    items = emptyList()
    cartTotal = 0.0
    shippingTotal = 0.0
    shippingCurrency = currency
    checkoutId = null
    lastDiscountCode = null
    lastDiscountId = null
    pendingShippingSelections.clear()

    currentCartId = null
    cartId = null

    createCart(currency = currency, country = country)
}

val CartManager.itemCount: Int
    get() = items.sumOf { max(it.quantity, 0) }

internal fun CartManager.updateCartTotal() {
    if (!trustBackendTotals) {
        subtotal = items.fold(0.0) { acc, item ->
            acc + (item.price * item.quantity)
        }
    }
    
    cartTotal = totalOverride ?: (subtotal + shippingTotal - discountTotal)

    val firstCurrency = items.firstOrNull()?.currency
    currency = when {
        !firstCurrency.isNullOrBlank() -> firstCurrency
        currency.isNotBlank() -> currency
        else -> "USD"
    }

    if (items.isEmpty()) {
        shippingTotal = 0.0
        shippingCurrency = currency
    }
}

internal fun CartManager.addProductLocally(
    product: Product,
    variant: Variant? = null,
    quantity: Int,
) {
    val variantId = variant?.id
    val variantTitle = variant?.title

    val existingIndex = items.indexOfFirst {
        it.productId == product.id && it.variantId == variantId
    }

    if (existingIndex >= 0) {
        val existing = items[existingIndex]
        val updated = existing.copy(quantity = existing.quantity + quantity)
        items = items.toMutableList().also { it[existingIndex] = updated }
        recalcShippingTotalsFromItems()
        return
    }

    val sortedImages = product.images.sortedBy { it.order }
    val imageUrl = sortedImages.firstOrNull()?.url

    val price = resolvePrice(variant, product)
    val currencyCode = variant?.price?.currencyCode ?: product.price.currencyCode

    val cartItem = CartItem(
        id = UUID.randomUUID().toString(),
        productId = product.id,
        variantId = variantId,
        variantTitle = variantTitle,
        title = product.title,
        brand = product.brand,
        imageUrl = imageUrl,
        price = price,
        currency = currencyCode,
        quantity = quantity,
        sku = product.sku,
        supplier = product.supplier,
    )

    items = items + cartItem
    recalcShippingTotalsFromItems()
}

internal fun CartManager.removeItemLocally(item: CartItem) {
    items = items.filterNot { it.id == item.id }
    recalcShippingTotalsFromItems()
}

internal fun CartManager.updateQuantityLocally(item: CartItem, newQuantity: Int) {
    items = items.mapNotNull { current ->
        if (current.id != item.id) return@mapNotNull current
        when {
            newQuantity <= 0 -> null
            else -> current.copy(quantity = newQuantity)
        }
    }
    recalcShippingTotalsFromItems()
}

internal fun CartManager.triggerFeedbackIfNeeded(previousCount: Int) {
    // No-op on Android placeholder (Swift used haptic feedback)
}

internal fun CartManager.recalcShippingTotalsFromItems() {
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

suspend fun CartManager.ensureCartIDForCheckout(): String? {
    currentCartId?.let { return it }
    createCart(currency = currency, country = country)
    return currentCartId
}

internal fun CartManager.syncLineItems(dto: CartDto): List<CartItem> {
    return dto.lineItems.map { line ->
        val sortedImages = line.image.orEmpty().sortedBy { it.order ?: 0 }
        val imageUrl = sortedImages.firstOrNull()?.url
        val shipping = line.shipping
        val shippingCurrency = shipping?.price?.currencyCode ?: dto.currency

        val availableShippings = line.availableShippings.orEmpty().mapNotNull { option ->
            val id = option.id ?: return@mapNotNull null
            if (id.isEmpty()) return@mapNotNull null
            val amount =
                option.price.amountInclTaxes
                    ?: option.price.amount
                    ?: 0.0
            val currency = option.price.currencyCode ?: dto.currency
            CartItem.ShippingOption(
                id = id,
                name = option.name ?: "Shipping",
                description = option.description,
                amount = amount,
                currency = currency,
            )
        }

        val itemPrice = line.price.amountInclTaxes ?: line.price.amount ?: 0.0
        val shippingAmount =
            shipping?.price?.amountInclTaxes
                ?: shipping?.price?.amount

        CartItem(
            id = line.id,
            productId = line.productId,
            variantId = line.variantId?.toString(),
            variantTitle = line.variantTitle,
            title = line.title ?: "",
            brand = line.brand,
            imageUrl = imageUrl,
            price = itemPrice,
            currency = line.price.currencyCode,
            quantity = line.quantity,
            sku = line.sku,
            supplier = line.supplier,
            shippingId = shipping?.id,
            shippingName = shipping?.name,
            shippingDescription = shipping?.description,
            shippingAmount = shippingAmount,
            shippingCurrency = shippingCurrency,
            availableShippings = availableShippings,
        )
    }
}

internal fun CartManager.createLineItemInput(
    product: Product,
    variant: Variant?,
    quantity: Int,
): LineItemInput {
    val variantIdInt = variant?.id?.toIntOrNull()
    return LineItemInput(
        productId = product.id,
        variantId = variantIdInt,
        quantity = quantity,
        priceData = null,
    )
}
