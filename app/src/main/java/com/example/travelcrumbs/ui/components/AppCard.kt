package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.ui.theme.Dimens

/*
 * 카드 · 섹션 제목 — 디자인 시스템 7절 · 11절(#34 토큰). 짝은 AppButtons(버튼 세 모양).
 * 색 · 모서리 · 글자는 테마에서 가져옴(수치는 theme 한 곳 · 그림자 대신 테두리).
 */

/** 카드 바탕 — Background/Surface + Border/Subtle 테두리 · 모서리 16 · 안 여백 20. 카드 자체는 누를 수 없음(버튼만). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(Dimens.CardBorderWidth, colors.outlineVariant, shape)
            .padding(Dimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
        content = content,
    )
}

/** 섹션 제목 — 32 Bold(디자인 시스템 6절 · Material headlineMedium). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
