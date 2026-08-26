package com.example.mmarecomp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntensityIndicator(isIntense: Boolean, modifier: Modifier = Modifier) {
    if (isIntense) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = "Séance intense",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
        )
    }
}
