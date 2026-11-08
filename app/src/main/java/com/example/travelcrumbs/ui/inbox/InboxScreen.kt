package com.example.travelcrumbs.ui.inbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 받은함(스펙 app-shell-v5 2-3) — 실제 내용(받은 여행함)은 A-12 이슈 몫. 지금은 제목 + 「준비 중」. */
@Composable
fun InboxScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_inbox, modifier = modifier)
}
