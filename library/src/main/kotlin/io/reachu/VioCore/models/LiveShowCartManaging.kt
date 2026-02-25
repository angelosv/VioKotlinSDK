package io.reachu.VioCore.models

/**
 * Kotlin analogue of Swift's LiveShowCartManaging protocol.
 * Host applications implement this to bridge live show actions with their cart system.
 */
interface LiveShowCartManaging {
    suspend fun addProduct(product: Product, quantity: Int)
    fun showCheckout()
}
