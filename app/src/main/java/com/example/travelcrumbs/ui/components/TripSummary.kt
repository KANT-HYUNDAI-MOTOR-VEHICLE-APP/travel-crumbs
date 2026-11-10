package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.ui.theme.Dimens
import java.time.Instant
import java.time.ZoneId

/** 내 여행 카드 한 장(C-02 최근 여행 · A-07 보관함 · #51) — 좌표 · 주소 없음. */
data class TripSummaryItem(
    val tripNo: Long,
    val tripNm: String?,
    val year: Int,
    val month: Int,
    val day: Int,
    val placeCnt: Int,
    val companions: List<CompanionType>,
    val public: Boolean,
    /** 받은 별점 평균 · 참여 수(공개 로드맵 · 없으면 0). */
    val ratingAvg: Double = 0.0,
    val ratingCnt: Int = 0,
)

/** 여행 → 카드(시작 날짜는 [zone] 기준). */
fun TripRecord.toSummary(zone: ZoneId, ratingAvg: Double = 0.0, ratingCnt: Int = 0): TripSummaryItem {
    val date = Instant.ofEpochMilli(startAt).atZone(zone).toLocalDate()
    return TripSummaryItem(
        tripNo = tripNo,
        tripNm = tripNm,
        year = date.year,
        month = date.monthValue,
        day = date.dayOfMonth,
        placeCnt = placeCnt,
        companions = CompanionType.entries.filter { it in companions },
        public = visibility == Visibility.ANONYMOUS_PUBLIC,
        ratingAvg = ratingAvg,
        ratingCnt = ratingCnt,
    )
}

/** 동행자 유형 → 화면 글자. */
fun companionLabelRes(type: CompanionType): Int = when (type) {
    CompanionType.SOLO -> R.string.companion_solo
    CompanionType.SPOUSE -> R.string.companion_spouse
    CompanionType.CHILD -> R.string.companion_child
    CompanionType.PARENT -> R.string.companion_parent
    CompanionType.PET -> R.string.companion_pet
}

/** 「2026.10.12 · 장소 4곳 · 배우자 · 아이」 한 줄. */
@Composable
fun tripMetaText(item: TripSummaryItem): String {
    val separator = stringResource(R.string.reco_meta_separator)
    val parts = listOf(
        stringResource(R.string.trip_date, item.year, item.month, item.day),
        stringResource(R.string.trip_place_cnt_short, item.placeCnt),
    ) + item.companions.map { stringResource(companionLabelRes(it)) }
    return parts.joinToString(separator)
}

/**
 * 내 여행 카드 — 카드 전체가 한 번 터치(→ 상세) · 오른쪽 [trailing](공개 중 표시 · 공개 토글).
 * 글자는 한 줄씩 · 좁은 칸(약 700dp)에서도 잘림만 있고 가로 스크롤 없음.
 */
@Composable
fun TripSummaryCard(
    item: TripSummaryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TripThumbSize)
            .clip(shape)
            .background(colors.surface)
            .border(Dimens.CardBorderWidth, colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.CardPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
            Text(
                text = tripNameText(item.tripNm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tripMetaText(item),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box { trailing() }
    }
}

/** 「공개 중」 · 「비공개」 표시(누를 수 없음). */
@Composable
fun VisibilityBadge(public: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = stringResource(if (public) R.string.trip_public_badge else R.string.trip_private_badge),
        style = MaterialTheme.typography.titleMedium,
        color = if (public) colors.onSecondaryContainer else colors.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (public) colors.secondaryContainer else colors.surfaceVariant)
            .padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.CardContentSpacing),
    )
}
