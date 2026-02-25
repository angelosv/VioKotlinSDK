package io.reachu.VioUI

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Bridge to present Klarna Native (In-App) UI. Uses an Activity result to
 * deliver the authorization token back to the caller. If the SDK is not
 * available or presentation fails, the caller can fallback to Web.
 */
object KlarnaBridge {
    @Volatile private var initialized: Boolean = false
    private var hostActivity: ComponentActivity? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    private var onAuthorizedCb: ((String) -> Unit)? = null
    private var onCancelCb: (() -> Unit)? = null
    private var onErrorCb: ((Throwable) -> Unit)? = null

    fun init(activity: ComponentActivity) {
        if (initialized) return
        hostActivity = activity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { res: ActivityResult ->
            Log.i("KlarnaBridge", "⬅️ ActivityResult code=${res.resultCode}")
            when (res.resultCode) {
                Activity.RESULT_OK -> {
                    val token = res.data?.getStringExtra("auth_token")
                    if (!token.isNullOrBlank()) {
                        Log.i("KlarnaBridge", "✅ Authorized with token (len=${token.length})")
                        onAuthorizedCb?.invoke(token)
                    } else {
                        Log.e("KlarnaBridge", "❌ Missing authorization token in result")
                        onErrorCb?.invoke(IllegalStateException("Missing authorization token"))
                    }
                }
                Activity.RESULT_CANCELED -> {
                    val err = res.data?.getStringExtra("error")
                    if (!err.isNullOrBlank()) {
                        Log.e("KlarnaBridge", "❌ Canceled with error: $err")
                        onErrorCb?.invoke(IllegalStateException(err))
                    } else {
                        Log.i("KlarnaBridge", "ℹ️ Canceled by user")
                        onCancelCb?.invoke()
                    }
                }
                else -> onErrorCb?.invoke(IllegalStateException("Unknown result"))
            }
            clearCallbacks()
        }
        initialized = true
        Log.i("KlarnaBridge", "✅ Initialized")
    }

    private fun clearCallbacks() {
        onAuthorizedCb = null
        onCancelCb = null
        onErrorCb = null
    }

    fun isReady(): Boolean {
        val ready = initialized && hostActivity != null && launcher != null
        Log.d("KlarnaBridge", "🔍 isReady(): $ready (init=$initialized, activity=${hostActivity != null}, launcher=${launcher != null})")
        return ready
    }

    fun present(
        clientToken: String,
        category: String?,
        autoAuthorize: Boolean = false,
        onAuthorized: (authorizationToken: String) -> Unit,
        onCancel: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        val activity = hostActivity
        val l = launcher
        if (activity == null || l == null) {
            Log.e("KlarnaBridge", "❌ Not initialized - activity=$activity, launcher=$l")
            onError(IllegalStateException("KlarnaBridge not initialized"))
            return
        }

        Log.i("KlarnaBridge", "🔄 Setting up callbacks...")
        onAuthorizedCb = onAuthorized
        onCancelCb = onCancel
        onErrorCb = onError

        val intent = Intent(activity, KlarnaNativeActivity::class.java).apply {
            putExtra("client_token", clientToken)
            putExtra("auto_authorize", autoAuthorize)
            if (!category.isNullOrBlank()) putExtra("category", category)
        }
        Log.i("KlarnaBridge", "➡️ Launching native UI (category=${category ?: "(none)"}, autoAuthorize=$autoAuthorize, token=${clientToken.take(10)}...)")

        try {
            l.launch(intent)
            Log.i("KlarnaBridge", "✅ Activity launched successfully")
        } catch (e: Exception) {
            Log.e("KlarnaBridge", "❌ Failed to launch activity: ${e.message}")
            onError(e)
        }
    }
}
