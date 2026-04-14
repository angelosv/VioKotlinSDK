package live.vio.VioUI

import android.content.Intent

object CheckoutDeepLinkHandler {

    fun extractEvent(intent: Intent?): CheckoutDeepLinkBus.Event? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "vio-demo" || uri.host != "checkout") return null

        val status = when (uri.pathSegments.firstOrNull()) {
            "success" -> CheckoutDeepLinkBus.Status.Success
            "cancel" -> CheckoutDeepLinkBus.Status.Cancel
            else -> return null
        }
        return CheckoutDeepLinkBus.Event(status)
    }

    fun handleIntent(intent: Intent?): Boolean {
        val event = extractEvent(intent) ?: return false
        return CheckoutDeepLinkBus.emitNow(event)
    }
}
