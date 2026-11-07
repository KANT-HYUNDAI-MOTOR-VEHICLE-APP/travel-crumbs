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

/**
 * 타이포그래피 — 지시서 6장. 기본 서체 Asta Sans(42dot · OFL · 가변 굵기 300–800 · res/font/asta_sans.ttf).
 * 서체에 없는 글자는 시스템 서체(Noto Sans KR 계열)로 보임. 지시서 px = dp · sp 로 그대로 씀.
 */
val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private fun style(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * LINE_HEIGHT).sp,
)

/**
 * Material 자리 ↔ 지시서 단계
 * - headlineLarge 40 Bold = 화면 제목 · headlineMedium 32 Bold = 섹션 제목
 * - titleLarge 28 SemiBold = 카드 제목 · labelLarge 28 Bold = 주요 버튼
 * - bodyLarge 24 = 본문 · bodyMedium 20 = 보조 정보(최소 20)
 */
val TcTypography = Typography(
    displayLarge = style(56, FontWeight.Bold),
    displayMedium = style(48, FontWeight.Bold),
    displaySmall = style(44, FontWeight.Bold),
    headlineLarge = style(40, FontWeight.Bold),
    headlineMedium = style(32, FontWeight.Bold),
    headlineSmall = style(28, FontWeight.Bold),
    titleLarge = style(28, FontWeight.SemiBold),
    titleMedium = style(24, FontWeight.SemiBold),
    titleSmall = style(20, FontWeight.SemiBold),
    bodyLarge = style(24, FontWeight.Normal),
    bodyMedium = style(20, FontWeight.Normal),
    bodySmall = style(20, FontWeight.Normal),
    labelLarge = style(28, FontWeight.Bold),
    labelMedium = style(24, FontWeight.SemiBold),
    labelSmall = style(20, FontWeight.Normal),
)

private const val LINE_HEIGHT = 1.3f
