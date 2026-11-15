package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsColors

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

/** v1.8 큰 행동 타일. */
@Composable
fun ActionTile(
    title: String,
    subtitle: String?,
    iconRes: Int,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (emphasized) colors.primary else colors.surface
    val content = if (emphasized) Color.White else colors.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = if (enabled) container else colors.surfaceVariant,
        contentColor = if (enabled) content else colors.onSurface.copy(alpha = 0.38f),
        border = if (emphasized) null else BorderStroke(Dimens.ButtonBorderWidth, colors.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.ActionTilePadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) content else colors.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(Dimens.ActionTileIconSize),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) content else colors.onSurface.copy(alpha = 0.38f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) content else colors.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
