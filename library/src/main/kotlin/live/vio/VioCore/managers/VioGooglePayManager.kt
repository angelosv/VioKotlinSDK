package live.vio.VioCore.managers

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manager for handling Google Pay integration using the Google Pay API.
 */
object VioGooglePayManager {
    private const val TAG = "VioGooglePayManager"
    
    // Request code for Google Pay activity result
    const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private fun getBaseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                put("allowedCardNetworks", JSONArray(listOf("AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA")))
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                })
            })
        }
    }

    private fun getIsReadyToPayRequest(): JSONObject {
        return baseRequest.apply {
            put("allowedPaymentMethods", JSONArray().put(getBaseCardPaymentMethod()))
        }
    }

    /**
     * Check if Google Pay is available for the user.
     */
    fun isGooglePayAvailable(activity: Activity, callback: (Boolean) -> Unit) {
        val paymentsClient = Wallet.getPaymentsClient(
            activity,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // TODO: Toggle based on config
                .build()
        )
        
        val request = IsReadyToPayRequest.fromJson(getIsReadyToPayRequest().toString())
        paymentsClient.isReadyToPay(request).addOnCompleteListener { task ->
            try {
                callback(task.getResult(ApiException::class.java) == true)
            } catch (exception: ApiException) {
                VioLogger.error("isReadyToPay failed: ${exception.message}", TAG)
                callback(false)
            }
        }
    }

    /**
     * Create a payment data request for Google Pay.
     * @param gateway Vio backend gateway identifier.
     * @param gatewayMerchantId Vio backend merchant identifier.
     * @param price Total price in decimal format (e.g., "10.00").
     * @param currency Currency code (e.g., "USD").
     */
    fun createPaymentDataRequest(
        gateway: String,
        gatewayMerchantId: String,
        price: String,
        currency: String
    ): JSONObject {
        val cardPaymentMethod = getBaseCardPaymentMethod().apply {
            put("tokenizationSpecification", JSONObject().apply {
                put("type", "PAYMENT_GATEWAY")
                put("parameters", JSONObject().apply {
                    put("gateway", gateway)
                    put("gatewayMerchantId", gatewayMerchantId)
                })
            })
        }

        return JSONObject(baseRequest.toString()).apply {
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", JSONObject().apply {
                put("totalPrice", price)
                put("totalPriceStatus", "FINAL")
                put("currencyCode", currency)
            })
            put("merchantInfo", JSONObject().apply {
                put("merchantName", "Vio Merchant")
            })
            put("shippingAddressRequired", true)
            put("shippingAddressParameters", JSONObject().apply {
                put("allowedCountryCodes", JSONArray(listOf("US", "GB", "NO", "ES"))) // Example countries
                put("phoneNumberRequired", true)
            })
        }
    }

    /**
     * Launch the Google Pay sheet.
     */
    fun launchGooglePay(activity: Activity, paymentDataRequestJson: JSONObject) {
        val paymentsClient = Wallet.getPaymentsClient(
            activity,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )

        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            activity,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    /**
     * Handle the result from the Google Pay activity.
     */
    fun handlePaymentSuccess(data: Intent?): String? {
        val paymentData = PaymentData.getFromIntent(data ?: return null) ?: return null
        val paymentInformation = paymentData.toJson()
        
        return try {
            val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val token = paymentMethodData.getJSONObject("tokenizationData").getString("token")
            token
        } catch (e: Exception) {
            VioLogger.error("Error parsing Google Pay token: ${e.message}", TAG)
            null
        }
    }

    /**
     * Extrae información de dirección del resultado de Google Pay.
     */
    fun extractAddressFromPaymentData(data: Intent?): Map<String, Any?>? {
        val paymentData = PaymentData.getFromIntent(data ?: return null) ?: return null
        val paymentInformation = paymentData.toJson()
        
        return try {
            val root = JSONObject(paymentInformation)
            val result = mutableMapOf<String, Any?>()
            
            // Shipping Address
            if (root.has("shippingAddress")) {
                val sa = root.getJSONObject("shippingAddress")
                result["shippingAddress"] = mapOf(
                    "name" to sa.optString("name"),
                    "address1" to sa.optString("address1"),
                    "address2" to sa.optString("address2"),
                    "city" to sa.optString("locality"),
                    "countryCode" to sa.optString("countryCode"),
                    "postalCode" to sa.optString("postalCode"),
                    "phone" to sa.optString("phoneNumber")
                )
            }
            
            // Billing Address (comes inside paymentMethodData.info.billingAddress)
            val pmd = root.getJSONObject("paymentMethodData")
            if (pmd.has("info")) {
                val info = pmd.getJSONObject("info")
                if (info.has("billingAddress")) {
                    val ba = info.getJSONObject("billingAddress")
                    result["billingAddress"] = mapOf(
                        "name" to ba.optString("name"),
                        "address1" to ba.optString("address1"),
                        "address2" to ba.optString("address2"),
                        "city" to ba.optString("locality"),
                        "countryCode" to ba.optString("countryCode"),
                        "postalCode" to ba.optString("postalCode"),
                        "phone" to ba.optString("phoneNumber")
                    )
                }
            }
            
            val email = root.optString("email")
            if (email.isNotEmpty()) result["email"] = email
            
            result
        } catch (e: Exception) {
            VioLogger.error("Error extracting address: ${e.message}", TAG)
            null
        }
    }
}
