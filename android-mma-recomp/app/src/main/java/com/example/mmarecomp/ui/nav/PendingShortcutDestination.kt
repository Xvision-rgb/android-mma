package com.example.mmarecomp.ui.nav

import androidx.compose.runtime.mutableStateOf

/** Destination demandée par un App Shortcut (appui long sur l'icône du
 *  launcher) — consommée une fois par MainNav puis remise à null pour ne
 *  pas re-naviguer à chaque recomposition. */
object PendingShortcutDestination {
    val route = mutableStateOf<String?>(null)
}
