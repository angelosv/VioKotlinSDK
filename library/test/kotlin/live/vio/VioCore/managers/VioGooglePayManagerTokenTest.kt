package live.vio.VioCore.managers

import android.content.Intent
import com.google.android.gms.wallet.PaymentData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VioGooglePayManagerTokenTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(PaymentData::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(PaymentData::class)
    }

    private fun createMockPaymentDataIntent(jsonString: String): Intent {
        val mockIntent = mockk<Intent>()
        val mockPaymentData = mockk<PaymentData>()
        every { PaymentData.getFromIntent(mockIntent) } returns mockPaymentData
        every { mockPaymentData.toJson() } returns jsonString
        return mockIntent
    }

    @Test
    fun `extractFullPaymentData extracts simple token string`() {
        val json = JSONObject().apply {
            put("paymentMethodData", JSONObject().apply {
                put("tokenizationData", JSONObject().apply {
                    put("token", "tok_123")
                })
            })
        }
        
        val intent = createMockPaymentDataIntent(json.toString())
        val result = VioGooglePayManager.extractFullPaymentData(intent)
        
        assertEquals("tok_123", result?.token)
    }

    @Test
    fun `extractFullPaymentData extracts token ID from JSON string`() {
        val tokenJson = JSONObject().apply {
            put("id", "tok_456")
        }
        val json = JSONObject().apply {
            put("paymentMethodData", JSONObject().apply {
                put("tokenizationData", JSONObject().apply {
                    put("token", tokenJson.toString())
                })
            })
        }
        
        val intent = createMockPaymentDataIntent(json.toString())
        val result = VioGooglePayManager.extractFullPaymentData(intent)
        
        assertEquals("tok_456", result?.token)
    }

    @Test
    fun `extractFullPaymentData extracts token ID from nested JSON string (user case)`() {
        // The payload from the user:
        // {"type":"card","card":{"token":"{\n  \"id\": \"tok_1TAVX2BjfRnXLEB4ptDoexHM\",\n  \"object\": \"token\", ... }"}}
        
        val nestedTokenJson = JSONObject().apply {
            put("id", "tok_nested_123")
        }
        val cardJson = JSONObject().apply {
            put("token", nestedTokenJson.toString())
        }
        val tokenAttrJson = JSONObject().apply {
            put("type", "card")
            put("card", cardJson)
        }
        
        val json = JSONObject().apply {
            put("paymentMethodData", JSONObject().apply {
                put("tokenizationData", JSONObject().apply {
                    put("token", tokenAttrJson.toString())
                })
            })
        }
        
        val intent = createMockPaymentDataIntent(json.toString())
        val result = VioGooglePayManager.extractFullPaymentData(intent)
        
        assertEquals("tok_nested_123", result?.token)
    }

    @Test
    fun `extractFullPaymentData returns raw token if JSON parsing fails`() {
        val json = JSONObject().apply {
            put("paymentMethodData", JSONObject().apply {
                put("tokenizationData", JSONObject().apply {
                    put("token", "{ invalid json")
                })
            })
        }
        
        val intent = createMockPaymentDataIntent(json.toString())
        val result = VioGooglePayManager.extractFullPaymentData(intent)
        
        assertEquals("{ invalid json", result?.token)
    }
}
