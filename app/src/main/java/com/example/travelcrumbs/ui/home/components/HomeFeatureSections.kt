package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * 홈 기능 구역 자리(스펙 app-shell 2-2 · D17) — 맨 위 영역(기록 상태 · 「이 장소 기록」) 아래 · debug 카드 위.
 * 다른 기능은 여기에 자기 구역 호출 한 줄씩을 차례로 더함(예: #4 「가 볼 장소」). 이 이슈는 자리만 · 지금은 비어 있음.
 * 구역은 맨 위 영역을 밀어내거나 가리지 않음 — 홈 Column 의 맨 위 영역 다음에만 놓임.
 */
@Composable
fun ColumnScope.HomeFeatureSections() {
    // 비어 있음 — 기능 구역은 각 기능 이슈가 더함
}
