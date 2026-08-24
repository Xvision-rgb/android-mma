package com.example.mmarecomp.util

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.mmarecomp.DailyReminderReceiver
import java.util.Calendar

/** API 33+ exige la permission runtime POST_NOTIFICATIONS ; en dessous elle
 *  est implicite. Centralisé ici pour que l'écran de réglages n'ait pas à
 *  connaître ce détail de version. */
val needsNotificationPermission: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun hasNotificationPermission(context: Context): Boolean {
    if (!needsNotificationPermission) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private const val REQUEST_CODE = 4200

private fun reminderPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DailyReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Programme un rappel quotidien générique ("log ta journée") — jamais
 *  spécifique à la pesée, jamais culpabilisant, toujours optionnel et
 *  déclenché uniquement si l'utilisateur l'a explicitement activé dans
 *  Réglages. Approximatif (setRepeating, pas exact) : un rappel d'habitude
 *  n'a pas besoin d'être à la seconde près, et ça évite la permission
 *  d'alarmes exactes. */
fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        reminderPendingIntent(context),
    )
}

fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    alarmManager.cancel(reminderPendingIntent(context))
}
