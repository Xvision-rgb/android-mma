package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PullToRefreshContainer
import androidx.compose.material3.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Poignée "tirer pour rafraîchir" partagée par Dashboard et Progression.
 * `isLoading` piloté par le ViewModel décide seul quand relâcher l'indicateur
 * (y compris quand le chargement initial de l'écran est déjà en cours).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()

    if (state.isRefreshing) {
        LaunchedEffect(Unit) { onRefresh() }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading && state.isRefreshing) {
            state.endRefresh()
        }
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection)) {
        content()
        PullToRefreshContainer(state = state, modifier = Modifier.align(Alignment.TopCenter))
    }
}
