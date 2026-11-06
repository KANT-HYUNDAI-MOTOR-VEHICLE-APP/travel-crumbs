package com.example.travelcrumbs.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 여행(스펙 app-shell 2-3) — 실제 내용(여행 목록 · 타임라인)은 #3 몫. 지금은 제목 + 「준비 중」. */
@Composable
fun TripsScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_trips, modifier = modifier)
}
