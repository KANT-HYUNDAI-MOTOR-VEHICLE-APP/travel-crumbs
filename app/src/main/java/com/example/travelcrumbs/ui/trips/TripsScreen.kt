package com.example.travelcrumbs.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 내 여행(스펙 app-shell-v5 2-3 · D33) — 나중 흐름(여행 목록 → 로드맵 → 저장 · 공개)은 다음 이슈 몫. 지금은 제목 + 「준비 중」. */
@Composable
fun TripsScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_trips, modifier = modifier)
}
