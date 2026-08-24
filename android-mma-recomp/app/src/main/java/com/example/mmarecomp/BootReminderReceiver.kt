package com.example.mmarecomp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mmarecomp.data.UserPreferencesStore
import com.example.mmarecomp.util.scheduleDailyReminder

/** Les alarmes répétées ne survivent pas à un redémarrage — ce receiver les
 *  reprogramme si le rappel quotidien était activé, à partir de la
 *  préférence locale (rien à synchroniser, pas d'appel réseau). */
class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val preferences = UserPreferencesStore(context).load()
        if (preferences.dailyReminderEnabled) {
            scheduleDailyReminder(context, preferences.dailyReminderHour, preferences.dailyReminderMinute)
        }
    }
}
