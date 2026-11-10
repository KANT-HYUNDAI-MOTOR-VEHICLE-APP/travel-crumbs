package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * B-06 별점 팝업(#41 · 디자인 시스템 18절 모달 · 13절 칩) — 「따라간 코스 어땠나요?」 · 작성자 닉네임 · 여행명 · 안내받은 날 /
 * 별 다섯 개(한 번 터치) · 고른 별의 글(「최고예요 · 5점」) / 좋았던 점 칩(선택 · 여러 개) / 「나중에」(Secondary) · 「★ 별점 주기」(Primary).
 * 바깥 누름 · 시스템 뒤로 = 「나중에」. 창 애니메이션 끔(팝업 틀 [AppDialog] · 가운데 · 폭 제한 · 뒤 화면 어둡게). 정차 화면에서만 그려짐(주행 · 모름이면 앱 틀이 이 화면을 그리지 않음).
 */
@Composable
fun RatingDialog(
    item: RatingDialogItem,
    onSelectScore: (Int) -> Unit,
    onToggleTag: (Long) -> Unit,
    onLater: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppDialog(
        title = stringResource(R.string.rating_title),
        dismissLabel = stringResource(R.string.rating_later),
        confirmLabel = stringResource(R.string.rating_submit),
        onDismiss = onLater,
        onConfirm = onSubmit,
        confirmEnabled = item.canSubmit,
        titleMaxLines = 1,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.nicknameNm,
                style = MaterialTheme.typography.titleMedium,
                color = colors.secondary,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.rating_trip_line, item.tripNm, item.guidedMonth, item.guidedDay),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.CenterHorizontally),
        ) {
            for (score in AppConfig.RATING_SCORE_MIN..AppConfig.RATING_SCORE_MAX) {
                StarButton(
                    score = score,
                    filled = item.ratingScore != null && score <= item.ratingScore,
                    onClick = { onSelectScore(score) },
                )
            }
        }
        Text(
            text = item.ratingScore?.let { stringResource(R.string.rating_score_text, stringResource(scoreWordRes(it)), it) }
                ?: stringResource(R.string.rating_pick_stars),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = if (item.ratingScore != null) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.rating_tags_label),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            item.tags.forEach { tag ->
                TagChip(tag = tag, onClick = { onToggleTag(tag.tagNo) })
            }
        }
        AppDialogBody(text = stringResource(R.string.rating_anonymous_hint), maxLines = 2)
    }
}

/** 별 하나 — 터치 칸 [Dimens.RatingStarSize] · 고른 별까지 Brand/Primary · 나머지 Border 색. */
@Composable
private fun StarButton(score: Int, filled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.rating_star_description, score)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(Dimens.RatingStarSize)
            .semantics {
                contentDescription = description
                selected = filled
            },
        shape = CircleShape,
        color = colors.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.rating_star),
                style = MaterialTheme.typography.displayLarge,
                color = if (filled) colors.primary else colors.outlineVariant,
            )
        }
    }
}

/** 좋았던 점 칩 — 고른 칩은 Brand/Secondary 바탕 + ✓(디자인 시스템 13절 · 색 · 체크 · 글자를 함께). */
@Composable
private fun TagChip(tag: RatingTagItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = Dimens.ChipHeight)
            .semantics { selected = tag.selected },
        shape = CircleShape,
        color = if (tag.selected) colors.secondary else colors.surface,
        contentColor = if (tag.selected) colors.onSecondary else colors.onSurface,
        border = if (tag.selected) null else BorderStroke(Dimens.CardBorderWidth, colors.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (tag.selected) stringResource(R.string.rating_tag_selected, tag.tagNm) else tag.tagNm,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
    }
}

/** 별 수 → 글(1 별로예요 · 2 아쉬워요 · 3 괜찮아요 · 4 좋아요 · 5 최고예요). */
fun scoreWordRes(score: Int): Int = when (score) {
    1 -> R.string.rating_word_1
    2 -> R.string.rating_word_2
    3 -> R.string.rating_word_3
    4 -> R.string.rating_word_4
    else -> R.string.rating_word_5
}
