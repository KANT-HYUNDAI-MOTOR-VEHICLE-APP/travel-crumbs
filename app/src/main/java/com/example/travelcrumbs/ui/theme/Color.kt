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
    val BrandPrimary = Color(0xFF4F46D8)
    val BrandPressed = Color(0xFF342DAF)
    val BrandSecondary = Color(0xFF4F46D8)
    val NeutralAction = Color(0xFF44464E)

    val BackgroundBase = Color(0xFFF2F3F6)
    val BackgroundSurface = Color(0xFFFFFFFF)
    val BackgroundElevated = Color(0xFFF0F1F5)
    val BackgroundSelected = Color(0xFFE7E6FF)
    val NavigationSelected = Color(0xFFE0E1E5)

    /** v1.8 경험 태그 — 긍정은 연보라, 부정 선택은 중립 회색으로 감정을 색으로도 구분. */
    val PositiveSelected = Color(0xFFEDEDFB)
    val NegativeSelected = Color(0xFFF2F3F6)
    val NegativeBorder = Color(0xFF8B8D95)
    val NegativeText = Color(0xFF44464E)

    val TextPrimary = Color(0xFF131417)
    val TextSecondary = Color(0xFF686A72)
    val TextMuted = Color(0xFF8B8D95)
    val TextOnPrimary = Color(0xFFFFFFFF)

    val BorderDefault = Color(0xFF686A72)
    val BorderSubtle = Color(0xFFCDD0D8)

    val Danger = Color(0xFFB3261E)

    /** Background/Dim — 팝업 뒤 화면을 덮는 검은색의 불투명도 60%(창 dimAmount · AppDialog). */
    const val BackgroundDimAlpha = 0.6f
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
