package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.SdkClient
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.LineItemInput
import java.util.UUID

suspend fun runCartDemo(config: DemoConfig) {
    val currency = config.currency
    val country = config.country
    val productId = 397968
    val quantity = 1
    val deleteItemAtEnd = false
    val deleteCartAtEnd = true

    val sessionId = "demo-${UUID.randomUUID()}"
    val sdk = SdkClient(config.baseUrl, config.apiToken)

    try {
        Logger.section("CreateCart ($sessionId)")
        val (createdCart, _) = Logger.measure("CreateCart") {
            sdk.cart.create(
                customerSessionId = sessionId,
                currency = currency,
                shippingCountry = country,
            )
        }
        Logger.json(createdCart, "Response (CreateCart)")
        var cartId = createdCart.cartId

        Logger.section("AddItem")
        val line = LineItemInput(productId = productId, quantity = quantity)
        val (afterAdd, _) = Logger.measure("AddItem") {
            sdk.cart.addItem(cartId = cartId, lineItems = listOf(line))
        }
        Logger.json(afterAdd, "Response (AddItem)")
        cartId = afterAdd.cartId

        Logger.section("GetLineItemsBySupplier")
        val (groups, _) = Logger.measure("GetLineItemsBySupplier") {
            sdk.cart.getLineItemsBySupplier(cartId)
        }
        Logger.json(groups, "Response (GetLineItemsBySupplier)")

        Logger.section("Apply cheapest shipping per supplier")
        var totalUpdates = 0
        val failures = mutableListOf<Pair<String, String>>()
        groups.forEach { group ->
            val shippings = (group.availableShippings ?: emptyList())
                .sortedBy { it.price.amount ?: Double.MAX_VALUE }
            val cheapestId = shippings.firstOrNull()?.id
            if (cheapestId.isNullOrBlank()) {
                Logger.warn("No shippings for supplier ${group.supplier?.name ?: "N/A"}. Skipping.")
                return@forEach
            }
            group.lineItems.forEach { item ->
                try {
                    sdk.cart.updateItem(
                        cartId = cartId,
                        cartItemId = item.id,
                        shippingId = cheapestId,
                        quantity = null,
                    )
                    totalUpdates += 1
                } catch (ex: Exception) {
                    val message = (ex as? SdkException)?.message ?: ex.localizedMessage
                    failures += item.id to message
                }
            }
        }
        Logger.info("Applied shipping to $totalUpdates item(s).")
        Logger.warnFailures(failures)

        Logger.section("GetCart (final)")
        val (finalCart, _) = Logger.measure("GetCart final") {
            sdk.cart.getById(cartId)
        }
        Logger.json(finalCart, "Response (GetCart final)")

        if (deleteItemAtEnd) {
            finalCart.lineItems.firstOrNull()?.let { firstItem ->
                Logger.section("DeleteItem")
                val (afterDelete, _) = Logger.measure("DeleteItem") {
                    sdk.cart.deleteItem(cartId, firstItem.id)
                }
                Logger.json(afterDelete, "Response (DeleteItem)")
            }
        }

        if (deleteCartAtEnd) {
            Logger.section("DeleteCart")
            val (deleted, _) = Logger.measure("DeleteCart") {
                sdk.cart.delete(cartId)
            }
            Logger.json(deleted, "Response (DeleteCart)")
        }

        Logger.section("Done")
        Logger.success("Cart demo completed successfully.")

    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

