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

    fun handleCartIntent(
        context: Context,
        targetUserId: String?,
        currentUserId: String?,
        productId: String?,
        campaignId: String?,
        title: String,
        body: String,
    ) {
        if (!targetUserId.isNullOrBlank() && targetUserId != currentUserId) {
            return
        }

        showNotification(
            context = context,
            productId = productId,
            campaignId = campaignId,
            action = "cart_intent",
            title = title,
            body = body,
        )
    }

    private fun showNotification(
        context: Context,
        productId: String?,
        campaignId: String?,
        action: String,
        title: String,
        body: String,
    ) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("productId", productId)
            putExtra("campaignId", campaignId)
            putExtra("action", action)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } ?: return

        if (isAppInForeground(context)) {
            context.startActivity(launchIntent)
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val running = activityManager.runningAppProcesses ?: return false
        return running.any { process ->
            process.processName == context.packageName &&
                process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
}
