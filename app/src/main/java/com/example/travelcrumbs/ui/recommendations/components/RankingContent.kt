package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.recommendations.TripCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/** B-02 동행자 칩 — 「전체」 + 표준 코드값 순서(A-01 칩과 같음 · 전체 · 혼자 · 부부 · 아이와 · 부모님 · 반려동물). */
private val COMPANION_FILTERS: List<CompanionType?> = listOf(null) + CompanionType.entries

/** 칩 문구 — 부부 · 아이와는 B-02 문구(표준 명칭 미결 M02) · 부모님 · 반려동물은 A-01 과 같은 문구 그대로. */
private fun companionFilterLabelRes(type: CompanionType?): Int = when (type) {
    null -> R.string.reco_filter_all
    CompanionType.SOLO -> R.string.reco_filter_solo
    CompanionType.SPOUSE -> R.string.reco_filter_spouse
    CompanionType.CHILD -> R.string.reco_filter_child
    CompanionType.PARENT -> R.string.companion_parent
    CompanionType.PET -> R.string.companion_pet
}

/** B-02 지역 로드맵 랭킹 — 제목 · 「지역 바꾸기」 · 별점순/최신순 · 동행자 칩 · 「# 태그별로 보기」 · 로드맵 카드. */
@Composable
fun RankingContent(
    regionTitle: String,
    sort: RankingSort,
    companion: CompanionType?,
    trips: List<TripCardItem>,
    onChangeRegion: () -> Unit,
    onSelectSort: (RankingSort) -> Unit,
    onSelectCompanion: (CompanionType?) -> Unit,
    onOpenTags: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecoTitle(regionTitle, modifier = Modifier.weight(1f, fill = false))
            TextButton(onClick = onChangeRegion, modifier = Modifier.heightIn(min = Dimens.ChipHeight)) {
                Text(
                    text = stringResource(R.string.reco_change_region),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            RecoChip(stringResource(R.string.reco_sort_rating), sort == RankingSort.RATING, { onSelectSort(RankingSort.RATING) })
            RecoChip(stringResource(R.string.reco_sort_latest), sort == RankingSort.LATEST, { onSelectSort(RankingSort.LATEST) })
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                COMPANION_FILTERS.forEach { type ->
                    RecoChip(stringResource(companionFilterLabelRes(type)), companion == type, { onSelectCompanion(type) })
                }
            }
            SecondaryButton(onClick = onOpenTags, modifier = Modifier.heightIn(min = Dimens.ChipHeight)) {
                Text(text = stringResource(R.string.reco_open_tags), style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
        }
        if (trips.isEmpty()) {
            RecoSectionLabel(stringResource(R.string.reco_ranking_empty))
        }
        trips.forEach { trip ->
            TripCard(item = trip, onClick = { onOpenTrip(trip.tripNo) })
        }
    }
}
