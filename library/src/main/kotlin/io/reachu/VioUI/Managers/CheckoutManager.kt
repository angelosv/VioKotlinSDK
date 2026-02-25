package io.reachu.VioUI.Managers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.domain.models.GetCheckoutDto
import io.reachu.sdk.domain.models.UpdateCheckoutDto

private val checkoutMapper = jacksonObjectMapper()

suspend fun CartManager.createCheckout(): String? {
    isLoading = true
    errorMessage = null
    try {
        val cid = ensureCartIDForCheckout()
            ?: run {
                println("ℹ️ [Checkout] Create: missing cartId")
                return null
            }

        println("🧾 [Checkout] Create START cartId=$cid")
        return try {
            logRequest("sdk.checkout.create", mapOf("cart_id" to cid))
            val dto = sdk.checkout.create(cartId = cid)
            val chkId = extractCheckoutId(dto)
            checkoutId = chkId
            logResponse("sdk.checkout.create", mapOf("checkoutId" to chkId))
            println("✅ [Checkout] Create OK checkoutId=${chkId ?: "nil"}")
            chkId
        } catch (sdkError: SdkException) {
            val msg = sdkError.messageText
            errorMessage = msg
            logError("sdk.checkout.create", sdkError)
            println("❌ [Checkout] Create FAIL $msg")
            null
        } catch (t: Throwable) {
            val msg = t.message
            errorMessage = msg
            logError("sdk.checkout.create", t)
            println("❌ [Checkout] Create FAIL $msg")
            null
        }
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.updateCheckout(
    checkoutId: String? = null,
    email: String? = null,
    successUrl: String? = null,
    cancelUrl: String? = null,
    paymentMethod: String? = null,
    shippingAddress: Map<String, Any?>? = null,
    billingAddress: Map<String, Any?>? = null,
    acceptsTerms: Boolean = true,
    acceptsPurchaseConditions: Boolean = true,
    status: String? = null,
): UpdateCheckoutDto? {
    isLoading = true
    errorMessage = null
    try {
        val chkId = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (chkId.isNullOrBlank()) {
            println("ℹ️ [Checkout] Update: missing checkoutId")
            return null
        }

        println("🧾 [Checkout] Update START checkoutId=$chkId")
        return try {
            logRequest(
                action = "sdk.checkout.update",
                payload = mapOf(
                    "checkout_id" to chkId,
                    "email" to email,
                    "success_url" to successUrl,
                    "cancel_url" to cancelUrl,
                    "payment_method" to paymentMethod,
                ),
            )
            val dto = sdk.checkout.update(
                checkoutId = chkId,
                status = status,
                email = email,
                successUrl = successUrl,
                cancelUrl = cancelUrl,
                paymentMethod = paymentMethod,
                shippingAddress = shippingAddress,
                billingAddress = billingAddress,
                buyerAcceptsTermsConditions = acceptsTerms,
                buyerAcceptsPurchaseConditions = acceptsPurchaseConditions,
            )
            logResponse("sdk.checkout.update", mapOf("checkoutId" to chkId))
            println("✅ [Checkout] Update OK")
            dto
        } catch (sdkError: SdkException) {
            val msg = sdkError.messageText
            errorMessage = msg
            logError("sdk.checkout.update", sdkError)
            println("❌ [Checkout] Update FAIL $msg")
            null
        } catch (t: Throwable) {
            val msg = t.message
            errorMessage = msg
            logError("sdk.checkout.update", t)
            println("❌ [Checkout] Update FAIL $msg")
            null
        }
    } finally {
        isLoading = false
    }
}

fun <T : Any> CartManager.extractCheckoutId(dto: T): String? {
    return try {
        val node: com.fasterxml.jackson.databind.JsonNode = checkoutMapper.valueToTree(dto)
        node.get("checkout_id")?.asText(null)
            ?: node.get("checkoutId")?.asText(null)
            ?: node.get("id")?.asText(null)
    } catch (t: Throwable) {
        logError("checkout.extractCheckoutId", t)
        null
    }
}

suspend fun CartManager.getCheckoutById(checkoutId: String): GetCheckoutDto? {
    if (checkoutId.isBlank()) {
        println("ℹ️ [Checkout] GetById: empty checkoutId")
        return null
    }

    println("🧾 [Checkout] GetById START checkoutId=$checkoutId")
    return try {
        logRequest("sdk.checkout.getById", mapOf("checkout_id" to checkoutId))
        val dto = sdk.checkout.getById(checkoutId = checkoutId)
        logResponse("sdk.checkout.getById", mapOf("status" to dto.status))
        println("✅ [Checkout] GetById OK status=${dto.status ?: "unknown"}")
        dto
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        logError("sdk.checkout.getById", sdkError)
        println("❌ [Checkout] GetById FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        logError("sdk.checkout.getById", t)
        println("❌ [Checkout] GetById FAIL $msg")
        null
    }
}


