package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import com.example.mmarecomp.ui.theme.Dimens

/** Bannière d'erreur réseau/API réutilisable. Ton factuel et calme (jamais
 *  culpabilisant) : on explique ce qui n'a pas marché et on propose de
 *  réessayer avec un libellé adapté au type d'opération (chargement,
 *  enregistrement, suppression). */
@Composable
fun ErrorBanner(
    error: ScreenError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryLabel = when (error.operation) {
        ErrorOperation.LOAD -> stringResource(R.string.error_retry_load)
        ErrorOperation.SAVE -> stringResource(R.string.error_retry_save)
        ErrorOperation.DELETE -> stringResource(R.string.error_retry_delete)
        ErrorOperation.UPDATE -> stringResource(R.string.error_retry_update)
    }
    val icon = when (error.operation) {
        ErrorOperation.DELETE -> Icons.Filled.DeleteOutline
        ErrorOperation.SAVE -> Icons.Filled.SaveAlt
        else -> Icons.Filled.CloudOff
    }
    val iconCd = when (error.operation) {
        ErrorOperation.LOAD -> stringResource(R.string.error_cd_offline)
        else -> null
    }
    val errorCd = stringResource(R.string.error_cd, error.message)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f), RoundedCornerShape(Dimens.cornerSm))
            .padding(Dimens.spaceMd)
            .semantics { contentDescription = errorCd },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Icon(
            icon,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget),
            ) {
                Text(retryLabel)
            }
        }
    }
}

/** Surcharge pour les écrans qui n'ont pas encore migré vers [ScreenError]. */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val errorCd = stringResource(R.string.error_cd, message)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f), RoundedCornerShape(Dimens.cornerSm))
            .padding(Dimens.spaceMd)
            .semantics { contentDescription = errorCd },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = stringResource(R.string.error_cd_offline),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget),
                ) {
                    Text(stringResource(R.string.error_retry_load))
                }
            }
        }
    }
}
