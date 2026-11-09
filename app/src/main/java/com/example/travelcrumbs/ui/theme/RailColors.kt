package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 레일 색 — 디자인 시스템 10절이 이름을 든 토큰 세 개만 레일에 한정해 씀(스펙 app-shell-v5 2-1 · D38 · N29).
 * 기본 칸 = 투명 배경 + 흰 아이콘 · 글자 / 선택 칸 = 배경 · 아이콘 · 글자가 함께 바뀜(색만으로 구분하지 않음 — 5절).
 * 색 토큰 · 서체 전체 적용은 별도 이슈(범위 밖) — 다른 화면은 Theme 의 기본 색 그대로.
 */
object RailColors {
    /** `Background/Base` — 레일 바탕. */
    val BackgroundBase = Color(0xFF101922)

    /** `Background/Selected` — 선택 칸 배경. */
    val BackgroundSelected = Color(0xFF3A3935)

    /** `Brand/Secondary` — 선택 칸 아이콘 · 글자. */
    val BrandSecondary = Color(0xFFE8D3A8)

    /** 기본 칸 아이콘 · 글자(흰 아이콘 · 10절). */
    val Unselected = Color.White

    /** 기본 칸 배경(투명 · 10절). */
    val UnselectedBackground = Color.Transparent

    /** Material 선택 알약(아이콘 뒤) — 쓰지 않음(선택 배경은 칸 전체 [BackgroundSelected]) · 투명. */
    val Indicator = Color.Transparent
}
