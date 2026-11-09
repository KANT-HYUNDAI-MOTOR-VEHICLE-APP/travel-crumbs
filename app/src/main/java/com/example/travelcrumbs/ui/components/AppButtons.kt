package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.travelcrumbs.ui.theme.Dimens

/*
 * 버튼 세 모양 — 디자인 시스템 12절(#34). 색 · 모서리 · 글자는 테마에서 가져옴(수치는 theme 한 곳).
 * 높이는 쓰는 화면이 정함(주요 버튼 72 이상 — Dimens).
 */

/** Primary — Brand/Primary 배경 + Text/OnPrimary 글자 · 화면의 주요 행동 1개에만. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        content = content,
    )
}

/** Secondary — Background/Surface 배경 + Border/Default 테두리 + Text/Primary 글자. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.surface,
        ),
        border = BorderStroke(Dimens.ButtonBorderWidth, if (enabled) colors.outline else colors.outlineVariant),
        content = content,
    )
}

/** Danger — Background/Surface 배경 + Danger 테두리 · 글자 · 삭제 · 여행 종료(확인)에만. */
@Composable
fun DangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.error,
            disabledContainerColor = colors.surface,
        ),
        border = BorderStroke(Dimens.ButtonBorderWidth, if (enabled) colors.error else colors.outlineVariant),
        content = content,
    )
}
