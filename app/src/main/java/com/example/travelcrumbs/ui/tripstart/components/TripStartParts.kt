package com.example.travelcrumbs.ui.tripstart.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A-01 입력 칸 이름표(「여행 이름」 · 「동행자」 · 「목적지 · 선택」). */
@Composable
fun TripStartLabel(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
