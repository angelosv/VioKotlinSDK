package com.vio.viaplaydemo

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import live.vio.VioCore.managers.DeviceTokenManager
import org.json.JSONObject
import kotlin.random.Random

/**
 * Service to handle Firebase Cloud Messaging tokens and messages for Viaplay Demo.
 */
class VioFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VioFCM"
        private const val CHANNEL_ID = "vio_notifications"
        private const val CHANNEL_NAME = "Vio Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.i(TAG, "***** FCM Token Refreshed: ${token.take(15)}...")

        // Assume userId is set in the app
        val userId = "android_demo_001" // Or get from preferences
        if (!userId.isNullOrBlank()) {
            android.util.Log.i(TAG, "***** Registering refreshed token for userId: $userId")
            DeviceTokenManager.register(userId, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.i(TAG, "***** FCM message received from: ${message.from}")
        android.util.Log.i(TAG, "***** Message data: ${message.data}")
        android.util.Log.i(TAG, "***** Message data keys: ${message.data.keys.joinToString(", ")}")
        android.util.Log.i(TAG, "***** Message notification: ${message.notification?.title} / ${message.notification?.body}")
        val payload = parsePayloadJson(message.data["vio_payload"])
        android.util.Log.i(TAG, "***** Parsed vio_payload: $payload")

        val productId = firstNonBlank(
            message.data["productId"],
            message.data["vio_cartIntent_productId"],
            payload?.optString("product_id"),
        )
        val campaignId = firstNonBlank(
            message.data["campaignId"],
            message.data["vio_cartIntent_campaignId"],
            payload?.optString("campaign_id"),
        )
        val action = firstNonBlank(
            message.data["action"],
            message.data["vio_event_type"],
            message.data["vio_cartIntent_kind"],
        )
        val deepLink = firstNonBlank(
            message.data["deeplink"],
            payload?.optString("deeplink"),
        )
        val title = message.notification?.title ?: message.data["title"] ?: "Vio"
        val body = message.notification?.body ?: message.data["body"] ?: "Check out this product!"
        android.util.Log.i(
            TAG,
            "***** Parsed payload -> action=$action, productId=$productId, campaignId=$campaignId, deepLink=$deepLink, title=$title"
        )

        showNotification(
            productId = productId ?: "default",
            campaignId = campaignId,
            action = action,
            deepLink = deepLink,
            title = title,
            body = body,
        )
    }

    private fun showNotification(
        productId: String,
        campaignId: String?,
        action: String?,
        deepLink: String?,
        title: String,
        body: String,
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open MainActivity
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra("productId", productId)
            if (!campaignId.isNullOrBlank()) putExtra("campaignId", campaignId)
            if (!action.isNullOrBlank()) putExtra("action", action)
            if (!deepLink.isNullOrBlank()) putExtra("deeplink", deepLink)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        android.util.Log.i(
            TAG,
            "***** Notification intent extras -> productId=$productId, campaignId=$campaignId, action=$action, deepLink=$deepLink"
        )

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Generic icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun parsePayloadJson(raw: String?): JSONObject? {
        if (raw.isNullOrBlank()) return null
        return try {
            JSONObject(raw)
        } catch (error: Exception) {
            android.util.Log.e(TAG, "***** Failed to parse vio_payload JSON: ${error.message}")
            null
        }
    }
}