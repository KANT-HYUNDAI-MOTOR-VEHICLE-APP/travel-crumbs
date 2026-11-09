package com.example.travelcrumbs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * 글자 단계 — 디자인 시스템 6절(#34). 160 dpi 에뮬레이터 기준 1px = 1dp → px 를 sp 로.
 * 서체는 시스템 기본(한글은 Noto Sans CJK 계열 — 6절 대체 서체). Asta Sans 파일은 넣지 않음(이슈 범위 밖).
 * 줄 높이는 글자 5 mm(160 dpi ≈ 31.5dp) 이상이 되게 본문 이상 34sp 이상(레일 · 주행 버튼 V46 · V61).
 */
private val ScreenTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp)
private val SectionTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)
private val CardTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp)
// 크기를 바꾸면 Dimens.DrivingButtonsRowMinWidth(「기록 일시정지」가 한 칸에 들어가는 폭)를 다시 계산.
private val ButtonLabel = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
private val Body = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 34.sp)
// 보조 단계(줄 높이 28 < 5 mm)는 주행 중 필수 정보에 쓰지 않음 · 중요 정보는 Body 이상(6절).
private val Supporting = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp)

/**
 * Material 단계 ↔ 디자인 시스템 단계. display · headlineLarge = 화면 제목(최대 40) · headlineMedium = 섹션 제목 ·
 * headlineSmall · titleMedium = 카드 제목 · labelLarge = 주요 버튼(Material 버튼 기본 글자) ·
 * titleLarge · bodyLarge = 본문 · 나머지 = 보조(최소 20).
 */
val TravelCrumbsTypography = Typography(
    displayLarge = ScreenTitle,
    displayMedium = ScreenTitle,
    displaySmall = ScreenTitle,
    headlineLarge = ScreenTitle,
    headlineMedium = SectionTitle,
    headlineSmall = CardTitle,
    titleLarge = Body,
    titleMedium = CardTitle,
    titleSmall = Supporting,
    bodyLarge = Body,
    bodyMedium = Supporting,
    bodySmall = Supporting,
    labelLarge = ButtonLabel,
    labelMedium = Supporting,
    labelSmall = Supporting,
)
