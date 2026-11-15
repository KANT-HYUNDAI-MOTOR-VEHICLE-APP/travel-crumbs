package com.example.travelcrumbs.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Figma v1.8 Brand Indigo 기반 밝은 차량 화면 색 역할. */
private val TravelCrumbsColorScheme = lightColorScheme(
    primary = TravelCrumbsColors.BrandPrimary,
    onPrimary = TravelCrumbsColors.TextOnPrimary,
    primaryContainer = TravelCrumbsColors.BrandPrimary,
    onPrimaryContainer = TravelCrumbsColors.TextOnPrimary,
    secondary = TravelCrumbsColors.BrandSecondary,
    onSecondary = TravelCrumbsColors.TextOnPrimary,
    // 선택 상태(레일 선택 칸 · 띠) = Background/Selected + Brand/Secondary(10절)
    secondaryContainer = TravelCrumbsColors.BackgroundSelected,
    onSecondaryContainer = Color(0xFF29237D),
    background = TravelCrumbsColors.BackgroundBase,
    onBackground = TravelCrumbsColors.TextPrimary,
    surface = TravelCrumbsColors.BackgroundSurface,
    onSurface = TravelCrumbsColors.TextPrimary,
    surfaceVariant = TravelCrumbsColors.BackgroundElevated,
    onSurfaceVariant = TravelCrumbsColors.TextSecondary,
    surfaceDim = TravelCrumbsColors.BackgroundBase,
    surfaceBright = TravelCrumbsColors.BackgroundElevated,
    surfaceContainerLowest = TravelCrumbsColors.BackgroundBase,
    surfaceContainerLow = TravelCrumbsColors.BackgroundSurface,
    surfaceContainer = TravelCrumbsColors.BackgroundSurface,
    surfaceContainerHigh = TravelCrumbsColors.BackgroundElevated,
    surfaceContainerHighest = TravelCrumbsColors.BackgroundElevated,
    inverseSurface = TravelCrumbsColors.TextPrimary,
    inverseOnSurface = TravelCrumbsColors.BackgroundBase,
    outline = TravelCrumbsColors.BorderDefault,
    outlineVariant = TravelCrumbsColors.BorderSubtle,
    error = TravelCrumbsColors.Danger,
    onError = TravelCrumbsColors.TextOnPrimary,
)

/** 버튼 · 카드 · 팝업 모서리 16(7절). 칩(999)은 쓰는 화면이 생길 때. */
private val TravelCrumbsShapes = Shapes(
    small = RoundedCornerShape(Dimens.Corner),
    medium = RoundedCornerShape(Dimens.Corner),
    large = RoundedCornerShape(Dimens.Corner),
    extraLarge = RoundedCornerShape(Dimens.Corner),
)

@Composable
fun TravelCrumbsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TravelCrumbsColorScheme,
        typography = TravelCrumbsTypography,
        shapes = TravelCrumbsShapes,
        content = content,
    )
}
