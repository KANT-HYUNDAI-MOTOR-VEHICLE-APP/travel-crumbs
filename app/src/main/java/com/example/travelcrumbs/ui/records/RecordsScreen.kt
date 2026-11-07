package com.example.travelcrumbs.ui.records

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 기록(스펙 app-shell-v5 2-3) — 정차 「기록」 칸의 실제 내용은 다음 이슈(N14). 지금은 제목 + 「준비 중」. 주행 · 모름의 「기록」 칸은 주행 전용 화면(DrivingScreen). */
@Composable
fun RecordsScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_records, modifier = modifier)
}
