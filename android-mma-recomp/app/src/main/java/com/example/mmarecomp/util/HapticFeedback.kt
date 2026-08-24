package com.example.mmarecomp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private fun vibrator(context: Context): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

/** Vibration courte et neutre pour célébrer un nouveau record personnel, ou
 *  pour confirmer un enregistrement réussi si la préférence correspondante
 *  est activée — jamais utilisée pour une alerte ou une notification.
 *  Contrôlée par les préférences "Vibrer sur un nouveau record" /
 *  "Vibrer à chaque enregistrement". */
fun celebrationVibration(context: Context) {
    vibrator(context).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
}

/** Double pulsation distincte pour signaler la fin du minuteur de repos —
 *  jamais un rappel ou une alerte, juste "temps écoulé". */
fun timerEndVibration(context: Context) {
    val pattern = longArrayOf(0, 120, 100, 120)
    vibrator(context).vibrate(VibrationEffect.createWaveform(pattern, -1))
}
