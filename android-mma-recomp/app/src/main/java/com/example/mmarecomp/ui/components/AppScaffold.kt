package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/** Ossature commune à tous les écrans : une barre de titre qui ne défile pas
 *  avec le contenu, et un retour visible quand l'écran est empilé.
 *
 *  Avant ce composant, les titres étaient des `Text` posés en premier `item`
 *  d'une `LazyColumn` — ils disparaissaient au premier scroll — et les quatre
 *  écrans empilés (édition de plan, WOD MMA, import de plan, objectif
 *  calorique) n'offraient aucun retour en dehors du geste système.
 *
 *  `onBack` non nul ⇒ flèche de retour. Les écrans d'onglet le laissent nul :
 *  leur retour, c'est la barre de navigation. */
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        snackbarHost = {
            snackbarHostState?.let { SnackbarHost(it) }
        },
        content = content,
    )
}
