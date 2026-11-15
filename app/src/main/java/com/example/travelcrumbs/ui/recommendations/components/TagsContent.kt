package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.recommendations.TagCountItem
import com.example.travelcrumbs.ui.components.AppStateKind
import com.example.travelcrumbs.ui.components.AppStatePanel
import com.example.travelcrumbs.ui.recommendations.TripCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/** B-03 태그별로 보기 — 태그 막대(많은 순 · 길이 = 붙은 수) · 태그 한 번 터치로 아래 목록이 그 태그 로드맵으로. */
@Composable
fun TagsContent(
    regionNm: String,
    regionTripCnt: Int,
    tagCounts: List<TagCountItem>,
    maxTagCnt: Int,
    selectedTagNo: Long?,
    selectedTagNm: String?,
    taggedTrips: List<TripCardItem>,
    onSelectTag: (Long) -> Unit,
    onOpenTrip: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        RecoTitle(stringResource(R.string.reco_tags_title, regionNm))
        RecoSectionLabel(stringResource(R.string.reco_tags_sub, regionTripCnt))
        if (tagCounts.isEmpty()) {
            AppStatePanel(title = stringResource(R.string.reco_tags_empty), kind = AppStateKind.Empty)
            return@Column
        }
        tagCounts.forEach { tag ->
            TagBar(
                item = tag,
                fraction = if (maxTagCnt == 0) 0f else tag.cnt.toFloat() / maxTagCnt,
                selected = tag.tagNo == selectedTagNo,
                onClick = { onSelectTag(tag.tagNo) },
            )
        }
        if (selectedTagNm != null) {
            Text(
                text = stringResource(R.string.reco_tagged_title, selectedTagNm, taggedTrips.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        taggedTrips.forEach { trip ->
            TripCard(item = trip, onClick = { onOpenTrip(trip.tripNo) })
        }
    }
}

@Composable
private fun TagBar(
    item: TagCountItem,
    fraction: Float,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.TagBarHeight)
            .semantics { this.selected = selected },
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(
            if (selected) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth,
            if (selected) colors.secondary else colors.outlineVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 막대 — 글자 대비를 지키려고 Background/Selected · Elevated 만 씀(밝은 브랜드 색은 테두리 · 글자에)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (selected) colors.secondaryContainer else colors.surfaceVariant, CircleShape),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selected) stringResource(R.string.reco_selected, item.tagNm) else item.tagNm,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) colors.secondary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.cnt.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.secondary else colors.onSurfaceVariant,
                )
            }
        }
    }
}
