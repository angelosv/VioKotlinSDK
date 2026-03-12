package live.vio.VioUI.Managers

import live.vio.sdk.core.errors.SdkException
import live.vio.sdk.domain.models.ConfirmPaymentKlarnaNativeDto
import live.vio.sdk.domain.models.InitPaymentKlarnaDto
import live.vio.sdk.domain.models.InitPaymentKlarnaNativeDto
import live.vio.sdk.domain.models.InitPaymentStripeDto
import live.vio.sdk.domain.models.KlarnaNativeAddressInputDto
import live.vio.sdk.domain.models.KlarnaNativeConfirmInputDto
import live.vio.sdk.domain.models.KlarnaNativeCustomerInputDto
import live.vio.sdk.domain.models.KlarnaNativeInitInputDto
import live.vio.sdk.domain.models.KlarnaNativeOrderDto
import live.vio.sdk.domain.models.PaymentIntentStripeDto
import live.vio.sdk.domain.models.InitPaymentVippsDto
import live.vio.sdk.domain.models.GetAvailablePaymentMethodsDto
import live.vio.sdk.domain.models.InitGooglePayDto
import live.vio.sdk.domain.models.ConfirmGooglePayDto
import live.vio.sdk.domain.models.ShippingAddressInputDto
import live.vio.sdk.domain.models.VioShippingContact
import live.vio.VioCore.managers.VioGooglePayManager
import android.content.Intent

suspend fun CartManager.initKlarna(
    countryCode: String,
    href: String,
    email: String?,
): InitPaymentKlarnaDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] KlarnaInit: missing checkoutId")
            null
        } else {
            println("💳 [Payment] KlarnaInit START checkoutId=$checkout")
            logRequest(
                "sdk.payment.klarnaInit",
                mapOf(
                    "checkoutId" to checkout,
                    "countryCode" to countryCode,
                    "href" to href,
                    "email" to email,
                ),
            )
            val dto = sdk.payment.klarnaInit(
                checkoutId = checkout,
                countryCode = countryCode,
                href = href,
                email = email,
            )
            logResponse("sdk.payment.klarnaInit")
            println("✅ [Payment] KlarnaInit OK")
            dto
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.klarnaInit", sdkError)
        println("❌ [Payment] KlarnaInit FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.klarnaInit", t)
        println("❌ [Payment] KlarnaInit FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.initKlarnaNative(
    input: KlarnaNativeInitInputDto,
): InitPaymentKlarnaNativeDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] KlarnaNativeInit: missing checkoutId")
            null
        } else {
            println("💳 [Payment] KlarnaNativeInit START checkoutId=$checkout")
            logRequest(
                "sdk.payment.klarnaNativeInit",
                mapOf("checkoutId" to checkout, "autoCapture" to input.autoCapture),
            )
            val dto = sdk.payment.klarnaNativeInit(
                checkoutId = checkout,
                input = input,
            )
            checkoutId = dto.checkoutId
            logResponse(
                "sdk.payment.klarnaNativeInit",
                mapOf("sessionId" to dto.sessionId, "checkoutId" to dto.checkoutId),
            )
            println("✅ [Payment] KlarnaNativeInit OK sessionId=${dto.sessionId}")
            dto
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.klarnaNativeInit", sdkError)
        println("❌ [Payment] KlarnaNativeInit FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.klarnaNativeInit", t)
        println("❌ [Payment] KlarnaNativeInit FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.confirmKlarnaNative(
    authorizationToken: String,
    autoCapture: Boolean? = null,
    customer: KlarnaNativeCustomerInputDto? = null,
    billingAddress: KlarnaNativeAddressInputDto? = null,
    shippingAddress: KlarnaNativeAddressInputDto? = null,
): ConfirmPaymentKlarnaNativeDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] KlarnaNativeConfirm: missing checkoutId")
            null
        } else {
            val input = KlarnaNativeConfirmInputDto(
                authorizationToken = authorizationToken,
                autoCapture = autoCapture,
                customer = customer,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
            )
            println("💳 [Payment] KlarnaNativeConfirm START checkoutId=$checkout")
            logRequest(
                "sdk.payment.klarnaNativeConfirm",
                mapOf("checkoutId" to checkout, "authorizationToken" to authorizationToken),
            )
            val dto = sdk.payment.klarnaNativeConfirm(
                checkoutId = checkout,
                input = input,
            )
            logResponse("sdk.payment.klarnaNativeConfirm", mapOf("orderId" to dto.orderId))
            println("✅ [Payment] KlarnaNativeConfirm OK orderId=${dto.orderId}")
            dto
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.klarnaNativeConfirm", sdkError)
        println("❌ [Payment] KlarnaNativeConfirm FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.klarnaNativeConfirm", t)
        println("❌ [Payment] KlarnaNativeConfirm FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.klarnaNativeOrder(
    orderId: String,
    userId: String? = null,
): KlarnaNativeOrderDto? {
    isLoading = true
    errorMessage = null
    println("🔍 [Payment] KlarnaNativeOrder START orderId=$orderId")
    return try {
        logRequest(
            "sdk.payment.klarnaNativeOrder",
            mapOf("orderId" to orderId, "userId" to userId),
        )
        val dto = sdk.payment.klarnaNativeOrder(orderId = orderId, userId = userId)
        logResponse("sdk.payment.klarnaNativeOrder", mapOf("status" to dto.status))
        println("✅ [Payment] KlarnaNativeOrder OK status=${dto.status ?: "-"}")
        dto
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.klarnaNativeOrder", sdkError)
        println("❌ [Payment] KlarnaNativeOrder FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.klarnaNativeOrder", t)
        println("❌ [Payment] KlarnaNativeOrder FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.stripeIntent(
    returnEphemeralKey: Boolean? = true,
): PaymentIntentStripeDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] StripeIntent: missing checkoutId")
            null
        } else {
            println("💳 [Payment] StripeIntent START checkoutId=$checkout")
            logRequest(
                "sdk.payment.stripeIntent",
                mapOf("checkoutId" to checkout, "returnEphemeralKey" to returnEphemeralKey),
            )
            val dto = sdk.payment.stripeIntent(
                checkoutId = checkout,
                returnEphemeralKey = returnEphemeralKey,
            )
            logResponse("sdk.payment.stripeIntent", mapOf("clientSecret" to dto.clientSecret))
            println("✅ [Payment] StripeIntent OK")
            dto
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.stripeIntent", sdkError)
        println("❌ [Payment] StripeIntent FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.stripeIntent", t)
        println("❌ [Payment] StripeIntent FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.stripeLink(
    successUrl: String,
    paymentMethod: String,
    email: String,
): InitPaymentStripeDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] StripeLink: missing checkoutId")
            null
        } else {
            println("💳 [Payment] StripeLink START checkoutId=$checkout")
            logRequest(
                "sdk.payment.stripeLink",
                mapOf(
                    "checkoutId" to checkout,
                    "successUrl" to successUrl,
                    "paymentMethod" to paymentMethod,
                    "email" to email,
                ),
            )
            val dto = sdk.payment.stripeLink(
                checkoutId = checkout,
                successUrl = successUrl,
                paymentMethod = paymentMethod,
                email = email,
            )
            logResponse("sdk.payment.stripeLink")
            println("✅ [Payment] StripeLink OK")
            dto
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.stripeLink", sdkError)
        println("❌ [Payment] StripeLink FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.stripeLink", t)
        println("❌ [Payment] StripeLink FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.vippsInit(
    email: String,
    returnUrl: String,
): InitPaymentVippsDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] VippsInit: missing checkoutId")
            null
        } else {
            println("💳 [Payment] VippsInit START checkoutId=$checkout")
            logRequest(
                "sdk.payment.vippsInit",
                mapOf(
                    "checkoutId" to checkout,
                    "email" to email,
                    "returnUrl" to returnUrl,
                ),
            )
            val dto = sdk.payment.vippsInit(
                checkoutId = checkout,
                email = email,
                returnUrl = returnUrl,
            )
            logResponse("sdk.payment.vippsInit", mapOf("returnUrl" to returnUrl))
            println("✅ [Payment] VippsInit OK")
            dto
        }
    } catch (sdkError: live.vio.sdk.core.errors.SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.payment.vippsInit", sdkError)
        println("❌ [Payment] VippsInit FAIL $msg")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.vippsInit", t)
        println("❌ [Payment] VippsInit FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.initGooglePay(): InitGooglePayDto? {
    isLoading = true
    errorMessage = null
    try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] GooglePayInit: missing checkoutId, using MOCK for Demo")
            return InitGooglePayDto(gateway = "stripe", gatewayMerchantId = "pk_test_vio_demo")
        } else {
            println("💳 [Payment] GooglePayInit START checkoutId=$checkout")
            logRequest("sdk.payment.googlePayInit", mapOf("checkoutId" to checkout))
            val dto = sdk.payment.googlePayInit(checkoutId = checkout)
            logResponse("sdk.payment.googlePayInit")
            println("✅ [Payment] GooglePayInit OK")
            return dto
        }
    } catch (t: Throwable) {
        val msg = t.message
        println("⚠️ [Payment] GooglePayInit FAIL ($msg), falling back to MOCK for Demo")
        return InitGooglePayDto(gateway = "stripe", gatewayMerchantId = "pk_test_vio_demo")
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.confirmGooglePay(
    token: String,
    email: String? = null,
    shippingAddress: ShippingAddressInputDto? = null
): ConfirmGooglePayDto? {
    isLoading = true
    errorMessage = null
    return try {
        val checkout = checkoutId?.takeIf { it.isNotBlank() } ?: createCheckout()
        if (checkout.isNullOrEmpty()) {
            println("ℹ️ [Payment] GooglePayConfirm: missing checkoutId")
            null
        } else {
            println("💳 [Payment] GooglePayConfirm START checkoutId=$checkout")
            logRequest("sdk.payment.googlePayConfirm", mapOf(
                "checkoutId" to checkout,
                "email" to email,
                "hasShipping" to (shippingAddress != null)
            ))
            val dto = sdk.payment.googlePayConfirm(
                checkoutId = checkout,
                googlePayToken = token,
                email = email,
                shippingAddress = shippingAddress
            )
            logResponse("sdk.payment.googlePayConfirm", mapOf("status" to dto.status))
            println("✅ [Payment] GooglePayConfirm OK status=${dto.status}")
            dto
        }
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.payment.googlePayConfirm", t)
        println("❌ [Payment] GooglePayConfirm FAIL $msg")
        null
    } finally {
        isLoading = false
    }
}

/**
 * Convierte un VioShippingContact del Manager de Google Pay al DTO de entrada del Repositorio.
 */
fun VioShippingContact.toInputDto(): ShippingAddressInputDto {
    val nameParts = name?.split(" ", limit = 2) ?: listOf("", "")
    val firstName = nameParts.getOrNull(0) ?: ""
    val lastName = nameParts.getOrNull(1) ?: ""
    
    return ShippingAddressInputDto(
        firstName = firstName,
        lastName = lastName,
        zip = postalCode,
        city = city,
        address1 = address1,
        address2 = address2,
        province = administrativeArea,
        countryCode = countryCode,
        phone = phone?.filter { it.isDigit() }
    )
}

// Fetch available payment methods from backend (lowercased names)
suspend fun CartManager.getAvailablePaymentMethodNames(): List<String> {
    return try {
        val list = sdk.payment.getAvailableMethods()
        list.mapNotNull { it?.name?.lowercase() }
    } catch (_: Throwable) {
        emptyList()
    }
}

/**
 * Procesa el resultado de Google Pay, extrae la dirección y el token, 
 * y actualiza el estado del CartManager.
 */
fun CartManager.handleGooglePayResult(data: Intent?) {
    val addressData = VioGooglePayManager.extractAddressFromPaymentData(data)
    if (addressData != null) {
        shippingAddress = addressData["shippingAddress"] as? Map<String, Any?>
        billingAddress = addressData["billingAddress"] as? Map<String, Any?>
        customerEmail = addressData["email"] as? String
        println("📍 [GooglePay] Address extracted: $addressData")
    }
    
    val token = VioGooglePayManager.handlePaymentSuccess(data)
    if (token != null) {
        println("🎫 [GooglePay] Token extracted: $token")
        // Aquí se podría disparar el confirm directamente si se desea
    }
}
