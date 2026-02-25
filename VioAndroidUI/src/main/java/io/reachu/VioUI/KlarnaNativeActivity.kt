package io.reachu.VioUI

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.klarna.mobile.sdk.api.KlarnaLoggingLevel
import com.klarna.mobile.sdk.api.payments.KlarnaPaymentCategory
import com.klarna.mobile.sdk.api.payments.KlarnaPaymentView
import com.klarna.mobile.sdk.api.payments.KlarnaPaymentViewCallback
import com.klarna.mobile.sdk.api.payments.KlarnaPaymentsSDKError

/**
 * Hosts Klarna native payment UI using official SDK API.
 */
class KlarnaNativeActivity : AppCompatActivity(), KlarnaPaymentViewCallback {

    private lateinit var klarnaPaymentView: KlarnaPaymentView
    private lateinit var klarnaContainer: FrameLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvLoading: TextView
    private lateinit var btnPay: Button
    private var clientToken: String? = null
    private var autoAuthorize: Boolean = false
    private var returnUrl: String = "https://httpbin.org/status/200"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_klarna_native)

        klarnaContainer = findViewById(R.id.klarna_container)
        progressLoading = findViewById(R.id.progress_loading)
        tvLoading = findViewById(R.id.tv_loading)

        findViewById<Button>(R.id.btn_close).setOnClickListener {
            setResult(Activity.RESULT_CANCELED, Intent().putExtra("error", "User canceled"))
            finish()
        }

        btnPay = findViewById(R.id.btn_pay)
        btnPay.setOnClickListener {
            Log.i("KlarnaNative", "🚀 Authorizing payment...")
            btnPay.isEnabled = false
            klarnaPaymentView.authorize(true, null)
        }

        clientToken = intent.getStringExtra("client_token")
        autoAuthorize = intent.getBooleanExtra("auto_authorize", false)
        if (clientToken.isNullOrBlank()) {
            Log.e("KlarnaNative", "❌ Missing client token")
            setResult(Activity.RESULT_CANCELED, Intent().putExtra("error", "Missing client token"))
            finish()
            return
        }

        val category = intent.getStringExtra("category") ?: "pay_now"

        try {
            Log.i("KlarnaNative", "➡️ Initializing Klarna Payment View (category=$category)")
            klarnaPaymentView = KlarnaPaymentView(this, null).apply {
                registerPaymentViewCallback(this@KlarnaNativeActivity)
                loggingLevel = KlarnaLoggingLevel.Error
                this.category = when (category.lowercase()) {
                    "pay_later" -> KlarnaPaymentCategory.PAY_LATER
                    "slice_it", "pay_over_time" -> KlarnaPaymentCategory.SLICE_IT
                    else -> KlarnaPaymentCategory.PAY_NOW
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            klarnaContainer.addView(klarnaPaymentView)
            Log.i("KlarnaNative", "🔄 Calling initialize...")
            klarnaPaymentView.initialize(clientToken!!, returnUrl)

            android.os.Handler(mainLooper).postDelayed({
                if (progressLoading.visibility == View.VISIBLE) {
                    Log.w("KlarnaNative", "⏰ Loading timeout - view may not have initialized properly")
                    showError("Loading timeout. Please try again.")
                    android.os.Handler(mainLooper).postDelayed({
                        setResult(Activity.RESULT_CANCELED, Intent().putExtra("error", "Loading timeout"))
                        finish()
                    }, 2000)
                }
            }, 15000)
        } catch (t: Throwable) {
            Log.e("KlarnaNative", "❌ Exception during initialization: ${t.message}", t)
            showError("Error: ${t.message}")
        }
    }

    private fun hideLoading() {
        progressLoading.visibility = View.GONE
        tvLoading.visibility = View.GONE
    }

    private fun showError(message: String) {
        runOnUiThread {
            hideLoading()
            tvLoading.text = message
            tvLoading.visibility = View.VISIBLE
        }
    }

    override fun onInitialized(view: KlarnaPaymentView) {
        Log.i("KlarnaNative", "✅ Payment view initialized")
        runOnUiThread {
            hideLoading()
            try {
                view.load(null)
            } catch (e: Exception) {
                Log.e("KlarnaNative", "❌ Error during load: ${e.message}")
                showError("Load error: ${e.message}")
            }
        }
    }

    override fun onLoaded(view: KlarnaPaymentView) {
        Log.i("KlarnaNative", "✅ Payment view loaded - ready for user interaction")
        runOnUiThread {
            hideLoading()
            view.visibility = View.VISIBLE
            
            if (autoAuthorize) {
                Log.i("KlarnaNative", "🪄 Auto-authorizing payment...")
                btnPay.visibility = View.GONE
                klarnaPaymentView.authorize(true, null)
            } else {
                btnPay.visibility = View.VISIBLE
                btnPay.isEnabled = true
            }
            
            view.requestLayout()
            klarnaContainer.requestLayout()
        }
    }

    override fun onLoadPaymentReview(view: KlarnaPaymentView, showForm: Boolean) {
        Log.i("KlarnaNative", "ℹ️ Payment review loaded (showForm=$showForm)")
    }

    override fun onAuthorized(
        view: KlarnaPaymentView,
        approved: Boolean,
        authToken: String?,
        finalizedRequired: Boolean?
    ) {
        Log.i("KlarnaNative", "✅ Authorization result: approved=$approved, token=${authToken?.length ?: 0} chars")
        runOnUiThread {
            if (approved && authToken != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra("auth_token", authToken))
                finish()
            } else {
                // If auto-authorization failed, show the button so the user can try again manually
                btnPay.visibility = View.VISIBLE
                btnPay.isEnabled = true 
                if (!approved) {
                    Log.w("KlarnaNative", "⚠️ Authorization not approved")
                }
            }
        }
    }

    override fun onReauthorized(view: KlarnaPaymentView, approved: Boolean, authToken: String?) {
        Log.i("KlarnaNative", "ℹ️ Reauthorized: approved=$approved")
    }

    override fun onFinalized(view: KlarnaPaymentView, approved: Boolean, authToken: String?) {
        Log.i("KlarnaNative", "ℹ️ Finalized: approved=$approved")
    }

    override fun onErrorOccurred(view: KlarnaPaymentView, error: KlarnaPaymentsSDKError) {
        Log.e("KlarnaNative", "❌ Klarna error: ${error.name} - ${error.message}")
        val errorMessage = when {
            error.message?.contains("stack size", ignoreCase = true) == true ->
                "Memory error in Klarna SDK - please try again"
            error.message?.contains("Failed to post", ignoreCase = true) == true ->
                "Communication error - please try again"
            else -> "${error.name}: ${error.message}"
        }

        runOnUiThread {
            hideLoading()
            tvLoading.text = "Error: $errorMessage"
            tvLoading.visibility = View.VISIBLE
            android.os.Handler(mainLooper).postDelayed({
                setResult(Activity.RESULT_CANCELED, Intent().putExtra("error", errorMessage))
                finish()
            }, 3000)
        }
    }
}
