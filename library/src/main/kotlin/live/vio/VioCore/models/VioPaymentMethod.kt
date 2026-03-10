package live.vio.VioCore.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic

/**
 * Payment methods supported by the Vio SDK.
 * Mirrors the Swift `PaymentMethod`.
 */
@Serializable
enum class VioPaymentMethod(private val value: String) {
    APPLE_PAY("apple_pay"),
    KLARNA("klarna"),
    VIPPS("vipps"),
    STRIPE("stripe");

    @JsonValue
    fun getValue(): String = value

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): VioPaymentMethod? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}
