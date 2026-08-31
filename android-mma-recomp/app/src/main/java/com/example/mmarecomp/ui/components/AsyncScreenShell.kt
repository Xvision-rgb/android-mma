package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import com.example.mmarecomp.ui.theme.Dimens

/** Enveloppe UX commune : chargement, erreur, vide, contenu. */
@Composable
fun AsyncScreenShell(
    isLoading: Boolean,
    error: ScreenError?,
    isEmpty: Boolean,
    emptyTitle: String,
    emptySubtitle: String? = null,
    emptyIcon: ImageVector? = null,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when {
        isLoading && isEmpty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        isEmpty && error != null -> {
            ErrorBanner(
                error = error,
                onRetry = onRetry,
                modifier = modifier.fillMaxWidth().padding(Dimens.spaceMd),
            )
        }
        isEmpty -> {
            EmptyState(
                title = emptyTitle,
                subtitle = emptySubtitle,
                icon = emptyIcon ?: Icons.Filled.Inbox,
                modifier = modifier.fillMaxWidth().padding(Dimens.spaceMd),
            )
        }
        else -> content()
    }
}
