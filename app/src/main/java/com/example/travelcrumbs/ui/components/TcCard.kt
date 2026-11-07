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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
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

/** 작은 머리말(입력 칸 이름 등) — 보조 정보 크기. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TcColors.TextSecondary,
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

/**
 * 여행 카드 — 지시서 15장. 여행명 · 날짜가 먼저, 그 아래 한 줄 요약. 카드 전체가 한 터치 영역 + 이동 아이콘.
 */
@Composable
fun TripCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TcCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TcColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TcColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TcColors.TextSecondary,
                modifier = Modifier
                    .padding(start = Dimens.ItemSpacingSmall)
                    .size(Dimens.IconSize),
            )
        }
    }
}

/**
 * 장소 경험 카드 — 지시서 16장. 방문 시각 · 장소명 · 유형/후기 · 태그 · 짧은 메모(최대 3줄).
 * 임시 기록은 Lavender 테두리 점 + 「입력 안 한 장소」로 입력 완료와 구분.
 */
@Composable
fun PlaceCard(
    time: String,
    name: String,
    details: String?,
    tags: List<String>,
    memo: String?,
    isDraft: Boolean,
    modifier: Modifier = Modifier,
) {
    TcCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isDraft) Color.Transparent else TcColors.BrandSecondary)
                    .border(3.dp, if (isDraft) TcColors.BrandLavender else TcColors.BrandSecondary, CircleShape),
            )
            Text(text = time, style = MaterialTheme.typography.bodyMedium, color = TcColors.TextSecondary)
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = if (isDraft) TcColors.BrandLavender else TcColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!details.isNullOrEmpty()) {
            Text(text = details, style = MaterialTheme.typography.bodyLarge, color = TcColors.TextSecondary, maxLines = 1)
        }
        if (tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { TagPill(it) }
            }
        }
        if (!memo.isNullOrBlank()) {
            Text(
                text = memo,
                style = MaterialTheme.typography.bodyLarge,
                color = TcColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 읽기 전용 태그 — 칩 모양(모서리 999)이지만 누를 수 없음. */
@Composable
fun TagPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TcColors.BrandSecondary,
        maxLines = 1,
        modifier = modifier
            .border(Dimens.BorderWidth, TcColors.BorderSubtle, RoundedCornerShape(percent = 50))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
