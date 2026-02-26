package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.LineItemInput
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

suspend fun runDiscountDemo(config: DemoConfig) {
    val currency = config.currency
    val country = config.country
    val productId = 397968
    val quantity = 10
    val discountTypeId = 2

    val sessionId = "demo-${UUID.randomUUID()}"
    val code = "DEMO-${UUID.randomUUID().toString().take(6)}"
    val now = Instant.now()
    val startDate = now.truncatedTo(ChronoUnit.SECONDS).toString()
    val endDate = now.plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString()

    val sdk = VioSdkClient(config.baseUrl, config.apiToken)

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
        val cartId = createdCart.cartId

        Logger.section("AddItem")
        val line = LineItemInput(productId = productId, quantity = quantity)
        val (afterAdd, _) = Logger.measure("AddItem") {
            sdk.cart.addItem(cartId = cartId, lineItems = listOf(line))
        }
        Logger.json(afterAdd, "Response (AddItem)")

        Logger.section("Discounts.get")
        val (allDiscounts, _) = Logger.measure("Discounts.get") {
            sdk.discount.get()
        }
        Logger.json(allDiscounts, "Response (Discounts.get)")

        Logger.section("Discounts.getByChannel")
        val (channelDiscounts, _) = Logger.measure("Discounts.getByChannel") {
            sdk.discount.getByChannel()
        }
        Logger.json(channelDiscounts, "Response (Discounts.getByChannel)")

        Logger.section("Discounts.add")
        val (addResponse, _) = Logger.measure("Discounts.add") {
            sdk.discount.add(
                code = code,
                percentage = 10,
                startDate = startDate,
                endDate = endDate,
                typeId = discountTypeId,
            )
        }
        Logger.json(addResponse, "Response (Discounts.add)")

        Logger.section("Discounts.getById")
        val (byId, _) = Logger.measure("Discounts.getById") {
            sdk.discount.getById(addResponse.id)
        }
        Logger.json(byId, "Response (Discounts.getById)")

        Logger.section("Cart.get (before apply)")
        val (cartBefore, _) = Logger.measure("Cart.get before apply") {
            sdk.cart.getById(cartId)
        }
        Logger.json(cartBefore, "Response (Cart before apply)")

        Logger.section("Discounts.apply")
        val (applyResponse, _) = Logger.measure("Discounts.apply") {
            sdk.discount.apply(code = code, cartId = cartId)
        }
        Logger.json(applyResponse, "Response (Discounts.apply)")

        Logger.section("Cart.get (after apply)")
        val (cartAfter, _) = Logger.measure("Cart.get after apply") {
            sdk.cart.getById(cartId)
        }
        Logger.json(cartAfter, "Response (Cart after apply)")

        Logger.section("Discounts.deleteApplied")
        val (deleteApplied, _) = Logger.measure("Discounts.deleteApplied") {
            sdk.discount.deleteApplied(code = code, cartId = cartId)
        }
        Logger.json(deleteApplied, "Response (Discounts.deleteApplied)")

        Logger.section("Discounts.delete")
        val (deletedDiscount, _) = Logger.measure("Discounts.delete") {
            sdk.discount.delete(addResponse.id)
        }
        Logger.json(deletedDiscount, "Response (Discounts.delete)")

        Logger.section("Done")
        Logger.success("Discount demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

