package com.example.travelcrumbs.ui.places

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PlaceholderContent

/** 찍힌 장소(레일 · 디자인 시스템 지시서 10장). 지금은 제목 + 「준비 중」. */
@Composable
fun PlacesScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(titleRes = R.string.nav_places, modifier = modifier)
}
