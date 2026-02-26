package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.SdkClient
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.LineItemInput
import java.util.UUID

suspend fun runSdkDemo(config: DemoConfig) {
    val currency = config.currency
    val country = config.country
    val countryName = "Norway"
    val email = "demo@acme.test"
    val phone = "41234567"
    val phoneCode = "47"
    val successUrl = "https://dev.vio.live/demo/success"
    val cancelUrl = "https://dev.vio.live/demo/cancel"

    val productId = 397968
    val quantity = 1

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
        var updated = 0
        groups.forEach { group ->
            val shippings = (group.availableShippings ?: emptyList())
                .sortedBy { it.price.amount ?: Double.MAX_VALUE }
            val cheapestId = shippings.firstOrNull()?.id
            if (cheapestId.isNullOrBlank()) {
                Logger.warn("No shippings for supplier ${group.supplier?.name ?: "N/A"}. Skipping.")
                return@forEach
            }
            group.lineItems.forEach { item ->
                if (item.shipping?.id == cheapestId) return@forEach
                sdk.cart.updateItem(
                    cartId = cartId,
                    cartItemId = item.id,
                    shippingId = cheapestId,
                    quantity = null,
                )
                updated += 1
            }
        }
        Logger.info("Shipping updated for $updated item(s).")

        Logger.section("Checkout.create")
        val (createdCheckout, _) = Logger.measure("Checkout.create") {
            sdk.checkout.create(cartId = cartId)
        }
        Logger.json(createdCheckout, "Response (Checkout.create)")
        val checkoutId = createdCheckout.id.ifBlank {
            throw SdkException("Cannot extract checkout id", code = "MISSING_ID")
        }

        val address = makeAddress(
            first = "Ola",
            last = "Nordmann",
            phone = phone,
            address1 = "Karl Johans gate 1",
            address2 = "Suite 2",
            city = "Oslo",
            countryName = countryName,
            countryCode = country,
            province = "",
            provinceCode = "",
            zip = "0154",
            company = "ACME AS",
            email = email,
            phoneCode = phoneCode,
        )

        Logger.section("Checkout.update")
        val (updatedCheckout, _) = Logger.measure("Checkout.update") {
            sdk.checkout.update(
                checkoutId = checkoutId,
                status = null,
                email = email,
                successUrl = successUrl,
                cancelUrl = cancelUrl,
                paymentMethod = "Klarna",
                shippingAddress = address,
                billingAddress = address,
                buyerAcceptsTermsConditions = true,
                buyerAcceptsPurchaseConditions = true,
            )
        }
        Logger.json(updatedCheckout, "Response (Checkout.update)")

        Logger.section("Payment.klarnaInit")
        runCatching {
            val (klarna, _) = Logger.measure("Payment.klarnaInit") {
                sdk.payment.klarnaInit(
                    checkoutId = checkoutId,
                    countryCode = country,
                    href = successUrl,
                    email = email,
                )
            }
            Logger.json(klarna, "Response (Payment.klarnaInit)")
            Logger.info("Follow Klarna redirect URL in a browser to finish the flow.")
        }.onFailure { logFailure("Klarna Init", it) }

        Logger.section("Cart.get (final)")
        val (finalCart, _) = Logger.measure("Cart.get final") {
            sdk.cart.getById(cartId)
        }
        Logger.json(finalCart, "Response (Cart final)")

        Logger.section("Done")
        Logger.success("SDK demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

private fun makeAddress(
    first: String,
    last: String,
    phone: String,
    address1: String,
    address2: String,
    city: String,
    countryName: String,
    countryCode: String,
    province: String,
    provinceCode: String,
    zip: String,
    company: String?,
    email: String,
    phoneCode: String,
): Map<String, Any?> = mapOf(
    "address1" to address1,
    "address2" to address2,
    "city" to city,
    "company" to (company ?: ""),
    "country" to countryName,
    "country_code" to countryCode,
    "email" to email,
    "first_name" to first,
    "last_name" to last,
    "phone" to phone,
    "phone_code" to phoneCode,
    "province" to province,
    "province_code" to provinceCode,
    "zip" to zip,
)

private fun logFailure(label: String, throwable: Throwable) {
    val message = (throwable as? SdkException)?.toString() ?: throwable.localizedMessage
    Logger.warn("$label failed: $message")
}

