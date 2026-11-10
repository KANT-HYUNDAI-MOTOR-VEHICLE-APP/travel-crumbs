package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 색 토큰 — 디자인 시스템 5절 이름 그대로(#34). 화면은 이 객체를 직접 쓰지 않고 [TravelCrumbsTheme] 의
 * `MaterialTheme.colorScheme` 를 거쳐 씀(매핑은 Theme.kt 한 곳).
 * 대비(WCAG 4.5:1 이상 · Base 기준): Text/Primary 16.5 · Text/Secondary 10.7 · Text/Muted 6.1 ·
 * Text/OnPrimary on Brand/Primary 8.9 · Brand/Secondary on Background/Selected 7.9 · Danger on Background/Surface 4.8.
 * Danger 는 Background/Elevated 위(4.3)에 쓰지 않음.
 */
object TravelCrumbsColors {
    val BrandPrimary = Color(0xFFF2A766)
    val BrandSecondary = Color(0xFFE8D3A8)

    val BackgroundBase = Color(0xFF101922)
    val BackgroundSurface = Color(0xFF18232D)
    val BackgroundElevated = Color(0xFF202C36)
    val BackgroundSelected = Color(0xFF3A3935)

    val TextPrimary = Color(0xFFF7F7F5)
    val TextSecondary = Color(0xFFC4CAD0)
    val TextMuted = Color(0xFF8F99A3)
    val TextOnPrimary = Color(0xFF101922)

    val BorderDefault = Color(0xFF65717C)
    val BorderSubtle = Color(0xFF35424D)

    val Danger = Color(0xFFD96C68)
}

/** 도로 지도 전용 바탕과 지형 · 도로 색. 글자 · 번호 핀 · 안내 경로는 테마 colorScheme. */
internal object RoadmapMapColors {
    val Background = Color(0xFF17262A)
    val Water = Color(0xFF1B3948)
    val Park = Color(0xFF24483D)
    val Shore = Color(0xFF537D88)
    val LocalRoad = Color(0xFF465553)
    val Street = Color(0xFF71817B)
    val MajorRoad = Color(0xFF9C9F85)
}
