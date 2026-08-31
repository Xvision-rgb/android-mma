package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.RecentExerciseEntry

@Composable
fun RecentExerciseChipsRow(
    entries: List<RecentExerciseEntry>,
    onSelect: (RecentExerciseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        Text(
            stringResource(R.string.workout_recent_exercises_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            entries.forEach { entry ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(entry) },
                    label = {
                        Text(
                            buildString {
                                append(entry.nom)
                                if (entry.modality == com.example.mmarecomp.model.ExerciseModality.Cardio) {
                                    entry.derniereDureeMin?.let {
                                        append(" · ")
                                        append(it)
                                        append(" min")
                                    }
                                    entry.derniereDistanceKm?.let {
                                        append(" · ")
                                        append(Formatting.oneDecimal(it))
                                        append(" km")
                                    }
                                } else {
                                    entry.derniereChargeKg?.let {
                                        append(" · ")
                                        append(Formatting.oneDecimal(it))
                                        append(" kg")
                                    }
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ExercisePickerSheet(
    title: String,
    entries: List<RecentExerciseEntry>,
    onSelect: (RecentExerciseEntry) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd))
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.workout_recent_exercises_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            ) {
                items(entries, key = { it.nom }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entry) }
                            .padding(vertical = Dimens.spaceSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.nom, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (entry.modality == com.example.mmarecomp.model.ExerciseModality.Cardio) {
                                    buildString {
                                        append("Cardio")
                                        entry.derniereDureeMin?.let { append(" · ${it} min") }
                                        entry.derniereDistanceKm?.let {
                                            append(" · ${Formatting.oneDecimal(it)} km")
                                        }
                                    }
                                } else {
                                    "${entry.nbSeries} séries" +
                                        (entry.derniereChargeKg?.let { " · ${Formatting.oneDecimal(it)} kg" } ?: "")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "→",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.workout_picker_cancel))
        }
    }
}
