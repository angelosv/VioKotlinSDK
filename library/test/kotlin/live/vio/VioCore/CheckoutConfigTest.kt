package live.vio.VioCore

import live.vio.VioCore.models.CheckoutConfig
import live.vio.VioCore.models.DynamicConfig
import live.vio.VioCore.models.PaymentMethod
import live.vio.sdk.core.helpers.JsonUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheckoutConfigTest {

    @Test
    fun `CheckoutConfig parses payment methods correctly`() {
        val json = """
            {
                "checkout": {
                    "paymentMethods": ["apple_pay", "klarna", "stripe"]
                }
            }
        """.trimIndent()

        val dynamicConfig = JsonUtils.mapper.readValue(json, DynamicConfig::class.java)
        val checkout = dynamicConfig.checkout

        org.junit.jupiter.api.Assertions.assertNotNull(checkout)
        assertEquals(3, checkout!!.paymentMethods.size)
        assertTrue(checkout.hasApplePay)
        assertTrue(checkout.hasKlarna)
        assertTrue(checkout.hasStripe)
        assertTrue(!checkout.hasVipps)
    }

    @Test
    fun `PaymentMethod fromString handles case sensitivity`() {
        assertEquals(PaymentMethod.APPLE_PAY, PaymentMethod.fromString("apple_pay"))
        assertEquals(PaymentMethod.APPLE_PAY, PaymentMethod.fromString("APPLE_PAY"))
        assertEquals(PaymentMethod.KLARNA, PaymentMethod.fromString("klarna"))
    }
}
