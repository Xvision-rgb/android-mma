package com.example.mmarecomp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mmarecomp.data.UserPreferencesStore

const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder"
private const val NOTIFICATION_ID = 4201

/**
 * Rappel quotidien optionnel — volontairement générique ("log ta journée"),
 * jamais spécifique à la pesée et jamais culpabilisant : c'est une simple
 * invitation, pas une alerte. N'émet rien si la permission notifications
 * n'a pas été accordée (API 33+) plutôt que de planter.
 */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val silent = UserPreferencesStore(context).load().dailyReminderSilent
        val channelId = if (silent) DAILY_REMINDER_SILENT_CHANNEL_ID else DAILY_REMINDER_CHANNEL_ID

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recomp & MMA")
            .setContentText("Une minute pour logger ta journée si tu veux 👋")
            .setAutoCancel(true)
            .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
