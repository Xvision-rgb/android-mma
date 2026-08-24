package com.example.mmarecomp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.mmarecomp.data.ThemePreferenceStore
import com.example.mmarecomp.data.UserPreferencesStore
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.RootScreen
import com.example.mmarecomp.ui.nav.PendingShortcutDestination
import com.example.mmarecomp.ui.theme.AppThemeState
import com.example.mmarecomp.ui.theme.MMARecompTheme

private const val SHORTCUT_DESTINATION_EXTRA = "destination"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppThemeState.mode.value = ThemePreferenceStore(this).load()
        AppPreferencesState.preferences.value = UserPreferencesStore(this).load()
        PendingShortcutDestination.route.value = intent?.getStringExtra(SHORTCUT_DESTINATION_EXTRA)

        setContent {
            val themeMode by AppThemeState.mode
            val preferences by AppPreferencesState.preferences
            MMARecompTheme(
                themeMode = themeMode,
                accent = preferences.accent,
                textScale = preferences.textScale.multiplier,
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RootScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingShortcutDestination.route.value = intent.getStringExtra(SHORTCUT_DESTINATION_EXTRA)
    }
}
