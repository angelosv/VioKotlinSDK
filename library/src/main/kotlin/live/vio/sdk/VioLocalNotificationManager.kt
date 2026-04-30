package live.vio.sdk

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

object VioLocalNotificationManager {
    private const val CHANNEL_ID = "vio_notifications"
    private const val CHANNEL_NAME = "Vio Notifications"

    /**
     * Helper to build a NotificationCompat for cart intent.
     * The host app is responsible for calling notify() on the NotificationManager.
     * The SDK does NOT schedule local notifications to avoid duplication with FCM.
     */
    fun buildCartIntentNotification(
        context: Context,
        productId: String?,
        campaignId: String?,
        title: String,
        body: String,
    ): NotificationCompat.Builder {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("productId", productId)
            putExtra("campaignId", campaignId)
            putExtra("action", "cart_intent")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } ?: throw IllegalStateException("No launch intent found for package")

        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
    }

    /**
     * Helper to create notification channel for Vio notifications.
     * Call this during app initialization if targeting API 26+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
