package com.example.travelcrumbs.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 차량 화면은 어두운 바탕 하나 — 지시서 5장 토큰을 Material 자리에 넣음(주간 · 야간 같은 값). */
private val TcColorScheme = darkColorScheme(
    primary = TcColors.BrandPrimary,
    onPrimary = TcColors.TextOnPrimary,
    primaryContainer = TcColors.BrandPrimary,
    onPrimaryContainer = TcColors.TextOnPrimary,
    secondary = TcColors.BrandSecondary,
    onSecondary = TcColors.TextOnPrimary,
    secondaryContainer = TcColors.BrandSecondary,
    onSecondaryContainer = TcColors.TextOnPrimary,
    tertiary = TcColors.BrandLavender,
    onTertiary = TcColors.TextOnPrimary,
    background = TcColors.BackgroundBase,
    onBackground = TcColors.TextPrimary,
    surface = TcColors.BackgroundSurface,
    onSurface = TcColors.TextPrimary,
    surfaceVariant = TcColors.BackgroundElevated,
    onSurfaceVariant = TcColors.TextSecondary,
    surfaceContainerLowest = TcColors.BackgroundBase,
    surfaceContainerLow = TcColors.BackgroundSurface,
    surfaceContainer = TcColors.BackgroundSurface,
    surfaceContainerHigh = TcColors.BackgroundElevated,
    surfaceContainerHighest = TcColors.BackgroundElevated,
    outline = TcColors.BorderDefault,
    outlineVariant = TcColors.BorderSubtle,
    error = TcColors.Danger,
    onError = TcColors.TextOnPrimary,
    scrim = Color.Black,
)

private val TcShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.ButtonCorner),
    small = RoundedCornerShape(Dimens.ButtonCorner),
    medium = RoundedCornerShape(Dimens.CardCorner),
    large = RoundedCornerShape(Dimens.CardCorner),
    extraLarge = RoundedCornerShape(Dimens.CardCorner),
)

@Composable
fun TravelCrumbsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TcColorScheme,
        typography = TcTypography,
        shapes = TcShapes,
        content = content,
    )
}
