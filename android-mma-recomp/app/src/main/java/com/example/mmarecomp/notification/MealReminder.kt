package com.example.mmarecomp.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mmarecomp.MainActivity
import java.util.Calendar

private const val CHANNEL_ID = "meal_reminder"
private const val NOTIFICATION_ID = 1002
private const val REQUEST_CODE = 2002
private const val PREFS_NAME = "meal_reminder_prefs"
private const val KEY_ENABLED = "enabled"
private const val KEY_LAST_LOGGED_DATE = "last_logged_date"
private const val KEY_HOUR = "hour"
private const val KEY_MINUTE = "minute"
private const val DEFAULT_REMINDER_HOUR = 20
private const val DEFAULT_REMINDER_MINUTE = 0

/** Rappel local quotidien, doux, pour penser à loguer les repas du jour.
 *  Désactivé par défaut — opt-in explicite dans Réglages. Même structure
 *  que WeighInReminder, canal et identifiants distincts. "Intelligent" :
 *  ne se déclenche pas si un repas a déjà été loggé aujourd'hui. */
object MealReminder {
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun hour(context: Context): Int = prefs(context).getInt(KEY_HOUR, DEFAULT_REMINDER_HOUR)
    fun minute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, DEFAULT_REMINDER_MINUTE)

    /** Heure de rappel personnalisable — inspiré de Duolingo qui programme
     *  ses rappels selon l'heure habituelle d'usage plutôt qu'une heure
     *  fixe pour tout le monde. Ici en version simple : l'utilisateur choisit
     *  lui-même son heure (pas d'inférence automatique, on n'a pas
     *  l'historique de logging par heure pour ça). Reprogramme
     *  immédiatement si le rappel est déjà actif. */
    fun setTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
        if (isEnabled(context)) schedule(context)
    }

    /** À appeler après qu'un repas a été loggé avec succès pour la date du
     *  jour, pour que le rappel du soir ne se déclenche pas inutilement.
     *  Purement local (SharedPreferences), aucun appel réseau depuis le
     *  receiver. */
    fun markLoggedToday(context: Context, dateIso: String) {
        prefs(context).edit().putString(KEY_LAST_LOGGED_DATE, dateIso).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Rappel repas", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MealReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun schedule(context: Context) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour(context))
            set(Calendar.MINUTE, minute(context))
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context),
        )
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    internal fun showNotification(context: Context) {
        val today = com.example.mmarecomp.util.DateUtils.today()
        if (prefs(context).getString(KEY_LAST_LOGGED_DATE, null) == today) return

        ensureChannel(context)
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val openApp = PendingIntent.getActivity(
            context, REQUEST_CODE, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Repas du jour")
            .setContentText("Petit rappel doux : as-tu tout loggé pour aujourd'hui ? 🙂")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MealReminder.showNotification(context)
    }
}
