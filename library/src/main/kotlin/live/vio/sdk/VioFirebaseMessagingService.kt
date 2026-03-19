package live.vio.sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.managers.DeviceTokenManager
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.VioSDK
import kotlin.random.Random

/**
 * Service to handle Firebase Cloud Messaging tokens and messages.
 */
class VioFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VioFCM"
        private const val CHANNEL_ID = "vio_notifications"
        private const val CHANNEL_NAME = "Vio Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        VioLogger.info("New FCM token received: ${token.take(10)}...", TAG)
        
        val userId = VioConfiguration.shared.state.value.userId
        if (!userId.isNullOrBlank()) {
            DeviceTokenManager.register(userId, token)
        } else {
            VioLogger.debug("User ID not set, skipping device registration for now", TAG)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        VioLogger.info("FCM message received: ${message.data}", TAG)
        
        val productId = message.data["productId"]
        val title = message.notification?.title ?: message.data["title"] ?: "Vio"
        val body = message.notification?.body ?: message.data["body"] ?: "Check out this product!"

        if (!productId.isNullOrBlank()) {
            VioSDK.openProduct(productId)
            showNotification(productId, title, body)
        }
    }

    private fun showNotification(productId: String, title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel for Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open MainActivity
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra("productId", productId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Generic icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}
