package io.reachu.liveshow

import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.VioCore.models.Product

/**
 * Simple holder so UI layers can access a default [LiveShowCartManaging] implementation
 * without taking a hard dependency on the actual cart SDK. Host applications should
 * replace [default] with their own implementation (for example `CartManager`).
 */
object LiveShowCartManagerProvider {
    var default: LiveShowCartManaging = NoopLiveShowCartManager()
}

class NoopLiveShowCartManager : LiveShowCartManaging {
    override suspend fun addProduct(product: Product, quantity: Int) {
        println("🛒 [LiveShow] No cart manager registered, ignoring addProduct for ${product.title}")
    }

    override fun showCheckout() {
        println("🛒 [LiveShow] No cart manager registered, showCheckout ignored")
    }
}
