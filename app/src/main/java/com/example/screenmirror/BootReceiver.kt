package com.example.screenmirror

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Android does not allow re-granting the MediaProjection (screen capture) permission
 * automatically after a reboot - the user must tap "Start now" again for security
 * reasons. This receiver just makes that one remaining tap as easy as possible by
 * posting a notification that opens MainActivity directly.
 *
 * The RemoteControlAccessibilityService does NOT need this - once enabled in
 * Settings > Accessibility, Android restarts it automatically after boot on its own.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "screen_mirror_resume_channel"
        const val NOTIF_ID = 2
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Resume Screen Mirroring", NotificationManager.IMPORTANCE_HIGH
            )
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Screen Mirror")
            .setContentText("Device restarted - tap to resume mirroring")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, notification)
    }
}
