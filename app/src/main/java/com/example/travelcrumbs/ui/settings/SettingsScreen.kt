package com.example.travelcrumbs.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 설정(스펙 app-shell-v5 2-4) — 들어오는 길은 정차 홈 오른쪽 위 「설정」 버튼 하나(D21). 지금은 제목 + 「준비 중」. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_settings, modifier = modifier)
}
