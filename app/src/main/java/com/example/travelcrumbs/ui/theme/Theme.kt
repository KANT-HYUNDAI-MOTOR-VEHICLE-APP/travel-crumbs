package com.example.travelcrumbs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** 차량 화면은 어두운 바탕이 기본 — 색은 뒤 이슈에서 디자인과 함께 정함. */
@Composable
fun TravelCrumbsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
}
