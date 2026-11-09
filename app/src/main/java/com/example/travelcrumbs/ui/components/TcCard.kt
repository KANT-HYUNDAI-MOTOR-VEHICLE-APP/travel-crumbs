package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TcColors

/** 카드 바탕 — Surface + 옅은 테두리 · 모서리 16 · 안 여백 20(지시서 7장 · 그림자 대신 테두리). */
@Composable
fun TcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(TcColors.BackgroundSurface)
            .border(
                Dimens.BorderWidth,
                if (highlighted) TcColors.BorderFocus else TcColors.BorderSubtle,
                shape,
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(Dimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** 섹션 제목 — 32 Bold(지시서 6장). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = TcColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * 여행 상태 카드 — 지시서 14장. 상태 점 + 상태명 + 경과 시간(+ 여행명).
 * 기록 중이면 Brand/Primary 점, 일시정지 · 기록 전이면 BlueGray 테두리 점(색 + 글자로 구분).
 */
@Composable
fun TripStatusCard(
    statusText: String,
    active: Boolean,
    elapsedText: String?,
    modifier: Modifier = Modifier,
    tripName: String? = null,
) {
    TcCard(modifier = modifier.fillMaxWidth(), highlighted = active) {
        if (tripName != null) {
            Text(
                text = tripName,
                style = MaterialTheme.typography.titleLarge,
                color = TcColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall),
        ) {
            StatusDot(active = active)
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineLarge,
                color = if (active) TcColors.BrandPrimary else TcColors.TextPrimary,
                maxLines = 1,
            )
            if (elapsedText != null) {
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TcColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (active) TcColors.BrandPrimary else Color.Transparent)
            .border(3.dp, if (active) TcColors.BrandPrimary else TcColors.BrandBlueGray, CircleShape),
    )
}
