package io.reachu.VioUI

import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

object PaymentSheetBridge {
    private var attachedActivity: ComponentActivity? = null
    private var currentPublishableKey: String? = null
    private var paymentSheet: PaymentSheet? = null

    /** Assignado por el caller justo antes de presentar PaymentSheet */
    var onResult: (PaymentSheetResult) -> Unit = {}

    fun attach(activity: ComponentActivity) {
        if (attachedActivity === activity && paymentSheet != null) return
        attachedActivity = activity
        paymentSheet = PaymentSheet(activity) { result -> onResult(result) }
    }

    fun ensureConfigured(publishableKey: String) {
        val context = attachedActivity?.applicationContext ?: return
        if (currentPublishableKey != publishableKey) {
            PaymentConfiguration.init(context, publishableKey)
            currentPublishableKey = publishableKey
        }
    }

    fun presentPaymentIntent(clientSecret: String, configuration: PaymentSheet.Configuration) {
        val sheet = paymentSheet
            ?: throw IllegalStateException("PaymentSheetBridge.attach must be called before presenting")
        sheet.presentWithPaymentIntent(clientSecret, configuration)
    }

    fun isReady(): Boolean = paymentSheet != null
}
