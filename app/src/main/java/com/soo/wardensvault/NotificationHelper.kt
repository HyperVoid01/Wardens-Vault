// Code Attribution
// Based on: Create a notification
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/notifications/build-notification
// Accessed: 27 October 2026
// Used for: Notification channel and NotificationCompat builder

package com.soo.wardensvault

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

//Posts system notifications for budget, bill, goal, achievement and cash flow events
object NotificationHelper {
    private const val CHANNEL_ID = "warden_alerts"

    //returns true when a notification was actually posted
    fun notify(
        context: Context,
        settingKey: String,
        id: Int,
        title: String,
        text: String
    ): Boolean {
        if (!AppSettings.isEnabled(context, AppSettings.KEY_ALL)) return false
        if (!AppSettings.isEnabled(context, settingKey)) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Warden's Vault alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
        return true
    }
}
