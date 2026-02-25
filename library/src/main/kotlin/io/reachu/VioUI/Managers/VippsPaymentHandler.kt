package io.reachu.VioUI.Managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

/**
 * Kotlin analogue of Swift's VippsPaymentHandler.
 * Pure JVM: parses string/URI and exposes state via StateFlow.
 */
object VippsPaymentHandler {
    enum class PaymentStatus { UNKNOWN, IN_PROGRESS, SUCCESS, FAILED, CANCELLED }

    private val _isPaymentInProgress = MutableStateFlow(false)
    val isPaymentInProgress: StateFlow<Boolean> = _isPaymentInProgress.asStateFlow()

    private val _currentCheckoutId = MutableStateFlow<String?>(null)
    val currentCheckoutId: StateFlow<String?> = _currentCheckoutId.asStateFlow()

    private val _paymentStatus = MutableStateFlow(PaymentStatus.UNKNOWN)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    fun startPaymentTracking(checkoutId: String) {
        println("🟠 [Vipps Handler] Starting payment tracking for: $checkoutId")
        _isPaymentInProgress.value = true
        _currentCheckoutId.value = checkoutId
        _paymentStatus.value = PaymentStatus.IN_PROGRESS
    }

    fun stopPaymentTracking() {
        println("🟠 [Vipps Handler] Stopping payment tracking")
        _isPaymentInProgress.value = false
        _currentCheckoutId.value = null
        _paymentStatus.value = PaymentStatus.UNKNOWN
    }

    fun isVippsReturnUri(uri: URI): Boolean {
        val params = parseQuery(uri)
        return uri.scheme == "reachu-demo" && params["payment_method"] == "vipps"
    }

    fun isVippsReturnUrl(url: String): Boolean =
        runCatching { isVippsReturnUri(URI(url)) }.getOrDefault(false)

    fun handleReturnUri(uri: URI) {
        println("🔗 [Vipps Handler] Received URI: $uri")
        val params = parseQuery(uri)
        val checkoutId = params["checkout_id"]
        val status = params["status"]?.lowercase()
        val paymentMethod = params["payment_method"]

        println("🔗 [Vipps Handler] Extracted parameters:")
        println("   - Checkout ID: ${checkoutId ?: "nil"}")
        println("   - Status: ${status ?: "nil"}")
        println("   - Payment Method: ${paymentMethod ?: "nil"}")

        if (paymentMethod != "vipps") return

        when (status) {
            "success" -> _paymentStatus.value = PaymentStatus.SUCCESS
            "cancelled", "cancel" -> _paymentStatus.value = PaymentStatus.CANCELLED
            "failed", "error" -> _paymentStatus.value = PaymentStatus.FAILED
            else -> _paymentStatus.value = PaymentStatus.UNKNOWN
        }
    }

    fun handleReturnUrl(url: String) =
        runCatching { handleReturnUri(URI(url)) }.onFailure {
            println("❌ [Vipps Handler] Invalid return URL: ${it.message}")
        }

    private fun parseQuery(uri: URI): Map<String, String> =
        (uri.rawQuery ?: "")
            .split('&')
            .filter { it.contains('=') }
            .associate {
                val idx = it.indexOf('=')
                val k = it.substring(0, idx)
                val v = it.substring(idx + 1)
                k to java.net.URLDecoder.decode(v, Charsets.UTF_8)
            }
}
