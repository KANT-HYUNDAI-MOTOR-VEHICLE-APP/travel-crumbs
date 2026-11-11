package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.recommendations.TripCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/*
 * 추천 화면(#37) 공통 부품 — 칩 · 로드맵 카드 · 별점 · debug 표시. 색 · 글자 · 모서리는 테마(#34)에서.
 * 글자는 본문 단계(24 / 줄 높이 34 · 5 mm 이상) 이상만 · 터치 칸은 48dp 이상.
 */

/** 고르는 칩(시도 · 정렬 · 동행자) — 고른 칩은 Brand/Secondary 바탕 + ✓. */
@Composable
fun RecoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = Dimens.ChipHeight)
            .semantics { this.selected = selected },
        shape = CircleShape,
        color = if (selected) colors.secondary else colors.surface,
        contentColor = if (selected) colors.onSecondary else colors.onSurface,
        border = if (selected) null else BorderStroke(Dimens.CardBorderWidth, colors.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (selected) stringResource(R.string.reco_selected, label) else label,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
    }
}

/** 별점 · 참여 수 「★ 4.8 · 12명」. */
@Composable
fun RatingText(ratingAvg: Double, ratingCnt: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.reco_rating, ratingAvg, ratingCnt),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

/** 화면 제목(시도 · 시군구 · 여행명). */
@Composable
fun RecoTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 구역 이름(「시도」 · 「강원 · 시군구」). */
@Composable
fun RecoSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 로드맵 카드(B-02 · B-03) — 순위 · 경로 미리보기 · 여행명 · 닉네임 · 장소 수 · 대표 태그 · 별점.
 * 1위는 Brand/Secondary 테두리. 카드 전체가 한 번 터치(→ B-04).
 */
@Composable
fun TripCard(
    item: TripCardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val top = item.rank == 1
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(
            if (top) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth,
            if (top) colors.secondary else colors.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.ItemSpacing),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.rank.toString(),
                modifier = Modifier.width(Dimens.RankColumnWidth),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.primary,
                textAlign = TextAlign.Center,
            )
            ThumbDots(modifier = Modifier.size(Dimens.TripThumbSize))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.tripNm,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.nicknameNm,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.secondary,
                    maxLines = 1,
                )
                Text(
                    text = item.representativeTagNm
                        ?.let { stringResource(R.string.reco_card_sub, item.placeCnt, it) }
                        ?: stringResource(R.string.reco_place_cnt, item.placeCnt),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RatingText(item.ratingAvg, item.ratingCnt)
        }
    }
}

/** 카드의 경로 미리보기 자리 — 점 네 개(실제 경로는 상세에서). */
@Composable
private fun ThumbDots(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceVariant,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outlineVariant),
    ) {
        val dot = colors.secondary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = THUMB_DOT_RADIUS.dp.toPx()
            THUMB_DOTS.forEach { (x, y) -> drawCircle(dot, radius = r, center = Offset(size.width * x, size.height * y)) }
        }
    }
}

private const val THUMB_DOT_RADIUS = 4
private val THUMB_DOTS = listOf(0.4f to 0.4f, 0.8f to 0.4f, 0.2f to 0.7f, 0.6f to 0.7f)
