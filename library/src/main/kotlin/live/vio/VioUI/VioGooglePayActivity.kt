package live.vio.VioUI

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import live.vio.VioCore.managers.VioGooglePayManager
import org.json.JSONObject

/**
 * Proxy Activity to launch Google Pay and return the result to the caller.
 * This is used to bridge the Task-based Google Pay API with ActivityResultLauncher<Intent>.
 */
class VioGooglePayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestJson = intent.getStringExtra("paymentDataRequestJson")
        val environment = intent.getIntExtra("environment", 3) // Default to TEST (WalletConstants.ENVIRONMENT_TEST)

        if (requestJson != null) {
            val request = JSONObject(requestJson)
            VioGooglePayManager.launchGooglePay(this, request, environment)
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VioGooglePayManager.LOAD_PAYMENT_DATA_REQUEST_CODE) {
            setResult(resultCode, data)
            finish()
        }
    }
}
