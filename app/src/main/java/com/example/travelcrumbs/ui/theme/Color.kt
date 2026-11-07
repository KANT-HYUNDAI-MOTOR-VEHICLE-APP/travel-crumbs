package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 컬러 토큰 — 디자인 시스템 지시서 5장(docs/design/design-system.md).
 * 화면은 MaterialTheme.colorScheme 을 먼저 쓰고, Material 자리에 없는 값(선택 배경 · 지도 핀 등)만 여기서 꺼냄.
 */
object TcColors {
    // Brand
    val BrandPrimary = Color(0xFFF2A766)
    val BrandSecondary = Color(0xFFE8D3A8)
    val BrandLavender = Color(0xFFBDB8E8)
    val BrandBlueGray = Color(0xFFA9B0BA)

    // Background
    val BackgroundBase = Color(0xFF101922)
    val BackgroundSurface = Color(0xFF18232D)
    val BackgroundElevated = Color(0xFF202C36)
    val BackgroundSelected = Color(0xFF3A3935)
    val BackgroundDim = Color(0x99000000)

    // Text
    val TextPrimary = Color(0xFFF7F7F5)
    val TextSecondary = Color(0xFFC4CAD0)
    val TextMuted = Color(0xFF8F99A3)
    val TextOnPrimary = Color(0xFF101922)

    // Border
    val BorderDefault = Color(0xFF65717C)
    val BorderSubtle = Color(0xFF35424D)
    val BorderFocus = Color(0xFFE8D3A8)

    // Semantic
    val Success = Color(0xFF55A091)
    val Warning = Color(0xFFE6B85C)
    val Danger = Color(0xFFD96C68)
    val Info = Color(0xFFBDB8E8)

    /** 지도 · 경로(17장) — 이동 경로 · 출발지는 Oat Beige(= Brand/Secondary 값). */
    val RouteOatBeige = BrandSecondary
}
