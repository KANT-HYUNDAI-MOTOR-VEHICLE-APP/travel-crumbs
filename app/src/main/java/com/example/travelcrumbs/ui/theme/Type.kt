@file:OptIn(ExperimentalTextApi::class)

package com.example.travelcrumbs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.travelcrumbs.R

/*
 * 글자 단계 — 디자인 시스템 6절(#34). 160 dpi 에뮬레이터 기준 1px = 1dp → px 를 sp 로.
 * 서체는 Asta Sans(42dot · SIL OFL 1.1 · 가변 굵기 · res/font/asta_sans.ttf · 라이선스 assets/licenses/asta-sans-OFL.txt · #4).
 * 서체에 없는 글자는 시스템 서체(Noto Sans CJK 계열 — 6절 대체 서체)로 보임. 글자 단계 수치는 #34 그대로.
 * 줄 높이는 글자 5 mm(160 dpi ≈ 31.5dp) 이상이 되게 본문 이상 34sp 이상(레일 · 주행 버튼 V46 · V61).
 */
private val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val ScreenTitle = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 84.sp)
private val SectionTitle = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 72.sp)
private val CardTitle = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 62.sp)
// 크기를 바꾸면 Dimens.DrivingButtonsRowMinWidth(「기록 일시정지」가 한 칸에 들어가는 폭)를 다시 계산.
private val ButtonLabel = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 72.sp)
private val Body = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 54.sp)
// 보조 단계(줄 높이 28 < 5 mm)는 주행 중 필수 정보에 쓰지 않음 · 중요 정보는 Body 이상(6절).
private val Supporting = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 48.sp)

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
