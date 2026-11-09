package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.recommendations.RegionCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/** B-01 지역 고르기 — 시도 칩 → 시군구 카드(이름 · 로드맵 수 · 마지막 「전체」) · 카드 한 번 터치로 B-02. */
@Composable
fun RegionSelectContent(
    sidoNms: List<String>,
    selectedSidoNm: String?,
    sigunguCards: List<RegionCardItem>,
    onSelectSido: (String) -> Unit,
    onSelectRegion: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        RecoTitle(stringResource(R.string.reco_region_title))
        RecoSectionLabel(stringResource(R.string.reco_sido_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            sidoNms.forEach { sido ->
                RecoChip(label = sido, selected = sido == selectedSidoNm, onClick = { onSelectSido(sido) })
            }
        }
        when {
            selectedSidoNm == null -> RecoSectionLabel(stringResource(R.string.reco_pick_sido))
            sigunguCards.isEmpty() -> RecoSectionLabel(stringResource(R.string.reco_region_empty))
            else -> {
                RecoSectionLabel(stringResource(R.string.reco_sigungu_label, selectedSidoNm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    sigunguCards.forEach { card ->
                        RegionCard(card = card, onClick = { onSelectRegion(card.sigunguNm) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionCard(card: RegionCardItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = Dimens.RegionCardMinWidth)
            .height(Dimens.RegionCardHeight),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = card.sigunguNm ?: stringResource(R.string.reco_region_all),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.reco_trip_cnt, card.tripCnt),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
