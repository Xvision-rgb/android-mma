package com.example.mmarecomp.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.mmarecomp.ui.AppPreferencesState

/**
 * Bouton micro qui délègue la reconnaissance vocale à l'app système via
 * [RecognizerIntent] — pas de permission RECORD_AUDIO à déclarer côté app,
 * pas de dépendance ajoutée, l'app système gère le micro elle-même. Reste un
 * simple raccourci de saisie : ne bloque jamais la saisie manuelle si la
 * reconnaissance vocale n'est pas disponible sur l'appareil.
 *
 * Se rend invisible (au lieu de composer un bouton désactivé) si la
 * préférence "Afficher la dictée vocale" est coupée — un seul point de
 * contrôle pour les trois écrans qui l'utilisent.
 */
@Composable
fun VoiceInputButton(onResult: (String) -> Unit) {
    if (!AppPreferencesState.preferences.value.showVoiceInput) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) onResult(spoken)
        }
    }

    IconButton(onClick = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Reconnaissance vocale indisponible sur cet appareil.", Toast.LENGTH_SHORT).show()
        }
    }) {
        Icon(Icons.Filled.Mic, contentDescription = "Dicter")
    }
}
