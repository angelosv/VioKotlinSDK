package io.reachu.VioUI

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Minimal host to render Klarna's html_snippet when checkout_url is not provided.
 * Detects success/cancel redirects and emits events so the overlay can close like Stripe.
 */
class KlarnaWebActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        setContentView(web)
        val successUrl = intent.getStringExtra("success_url")?.trim().orEmpty()
        val cancelUrl = intent.getStringExtra("cancel_url")?.trim().orEmpty()

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                Log.i("KlarnaWeb", "➡️ Navigating: $url")
                if (isSuccess(url, successUrl)) {
                    Log.i("KlarnaWeb", "✅ Detected success redirect")
                    lifecycleScope.launch { CheckoutDeepLinkBus.emit(CheckoutDeepLinkBus.Event(CheckoutDeepLinkBus.Status.Success)) }
                    finish()
                    return true
                }
                if (isCancel(url, cancelUrl)) {
                    Log.i("KlarnaWeb", "ℹ️ Detected cancel redirect")
                    lifecycleScope.launch { CheckoutDeepLinkBus.emit(CheckoutDeepLinkBus.Event(CheckoutDeepLinkBus.Status.Cancel)) }
                    finish()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val u = url.orEmpty()
                Log.i("KlarnaWeb", "⬅️ Page finished: $u")
                if (u.startsWith("http", ignoreCase = true) && isSuccess(u, successUrl)) {
                    Log.i("KlarnaWeb", "✅ Detected success (onPageFinished)")
                    lifecycleScope.launch { CheckoutDeepLinkBus.emit(CheckoutDeepLinkBus.Event(CheckoutDeepLinkBus.Status.Success)) }
                    finish()
                }
            }
        }

        val html = intent.getStringExtra("html_snippet").orEmpty()
        if (html.isNotBlank()) {
            Log.i("KlarnaWeb", "Rendering HTML snippet (${html.length} chars)")
            web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } else {
            Log.e("KlarnaWeb", "No html_snippet provided")
            finish()
        }
    }

    private fun isSuccess(url: String, expected: String): Boolean {
        if (expected.isNotBlank() && url.startsWith(expected, ignoreCase = true)) return true
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return runCatching {
            uri.isHierarchical &&
                (uri.getQueryParameter("status")?.equals("success", ignoreCase = true) == true)
        }.getOrDefault(false)
    }

    private fun isCancel(url: String, expected: String): Boolean {
        if (expected.isNotBlank() && url.startsWith(expected, ignoreCase = true)) return true
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return runCatching {
            uri.isHierarchical &&
                (uri.getQueryParameter("status")?.equals("cancel", ignoreCase = true) == true)
        }.getOrDefault(false)
    }
}
