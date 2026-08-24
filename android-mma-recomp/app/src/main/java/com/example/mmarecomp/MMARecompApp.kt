package com.example.mmarecomp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

const val DAILY_REMINDER_SILENT_CHANNEL_ID = "daily_reminder_silent"

class MMARecompApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Rappel quotidien",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Rappel optionnel pour logger ta journée — jamais lié à la pesée."
            }
            val silentChannel = NotificationChannel(
                DAILY_REMINDER_SILENT_CHANNEL_ID,
                "Rappel quotidien (silencieux)",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Même rappel, sans son ni vibration — préférence \"Son du rappel\"."
                setSound(null, null)
            }
            manager?.createNotificationChannel(channel)
            manager?.createNotificationChannel(silentChannel)
        }
    }
}
