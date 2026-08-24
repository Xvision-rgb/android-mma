package com.example.mmarecomp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibration courte et neutre pour célébrer un nouveau record personnel —
 *  jamais utilisée pour une alerte, un rappel ou une notification, seulement
 *  une célébration ponctuelle déclenchée explicitement après une sauvegarde
 *  réussie. Contrôlée par la préférence "Vibrer sur un nouveau record". */
fun celebrationVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
}
