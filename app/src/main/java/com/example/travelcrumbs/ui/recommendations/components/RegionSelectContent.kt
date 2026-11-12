package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.MapStatus
import com.example.travelcrumbs.ui.recommendations.RecommendationsUiState
import com.example.travelcrumbs.ui.recommendations.RegionCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * B-01 지역 고르기 — 위: 시도 칩 → 시군구 카드(한 번 터치로 B-02) · 아래 남은 영역: 한국 전국 지도(#59).
 * 머리는 창 높이의 일부까지만 쓰고(넘치면 스크롤) 나머지는 지도 — 작은 창에서도 지역 선택과 카드 행동에 닿게(M28).
 */
@Composable
fun RegionSelectContent(
    uiState: RecommendationsUiState,
    actions: RecommendationActions,
    entryKey: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val headerMax = maxHeight * Dimens.RecoRegionHeaderMaxFraction
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            RegionHeader(
                sidoNms = uiState.sidoNms,
                selectedSidoNm = uiState.selectedSidoNm,
                sigunguCards = uiState.sigunguCards,
                onSelectSido = actions.onSelectSido,
                onSelectRegion = actions.onSelectRegion,
                modifier = Modifier
                    .heightIn(max = headerMax)
                    .verticalScroll(rememberScrollState()),
            )
            RegionMapArea(
                uiState = uiState,
                actions = actions,
                entryKey = entryKey,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = Dimens.ItemSpacing),
            )
        }
    }
}

@Composable
private fun RegionHeader(
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

/** 지도 영역 — 지도 · 상태 한 줄 · 출처(상시) · 카드 하나(장소 카드 · 후보 목록 · 출처 안내). */
@Composable
private fun RegionMapArea(
    uiState: RecommendationsUiState,
    actions: RecommendationActions,
    entryKey: Int,
    modifier: Modifier = Modifier,
) {
    val map = uiState.map
    var showLicense by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.clip(MaterialTheme.shapes.large)) {
        val cardModifier = Modifier
            .align(Alignment.BottomStart)
            .padding(Dimens.ItemSpacing)
            .widthIn(max = Dimens.RecoMapCardMaxWidth)
            .heightIn(max = maxHeight * Dimens.RecoMapCardMaxHeightFraction)
        if (map.status == MapStatus.FAILED) {
            // 가짜 도로 · 빈 격자 없이 안내 한 줄만 · 시도 · 시군구 · 랭킹은 그대로(N2 · M30)
            MapStatusLine(stringResource(R.string.reco_map_failed), Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }
        RecommendationMap(
            entryKey = entryKey,
            mapUi = map,
            selectedSidoNm = uiState.selectedSidoNm,
            callbacks = actions.map,
            modifier = Modifier.fillMaxSize(),
        )
        mapStatusText(map.status)?.let { text ->
            MapStatusLine(text, Modifier.align(Alignment.TopCenter).padding(top = Dimens.ItemSpacing))
        }
        // 출처 상시 표시(ODbL · M32) — 누르면 출처 · 라이선스 안내
        Surface(
            onClick = { showLicense = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.CardContentSpacing)
                .heightIn(min = Dimens.MinTouchTarget),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_ALPHA),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = Dimens.CardContentSpacing)) {
                Text(stringResource(R.string.reco_map_attribution), style = MaterialTheme.typography.bodyLarge)
            }
        }
        val card = map.card
        val candidates = map.candidates
        when {
            showLicense -> MapLicenseCard(onClose = { showLicense = false }, modifier = cardModifier)
            card != null -> MapPlaceCard(
                card = card,
                guidanceRequesting = uiState.guidanceRequesting,
                onSelectRoadmap = actions.onSelectCardRoadmap,
                onOpenRoadmap = actions.onOpenCardRoadmap,
                onGuide = actions.onCardGuidance,
                onClose = actions.onCloseMapCard,
                modifier = cardModifier,
            )
            candidates != null -> MapCandidateList(
                candidates = candidates,
                onSelect = actions.onSelectMapCandidate,
                onClose = actions.onCloseMapCard,
                modifier = cardModifier,
            )
        }
    }
}

@Composable
private fun mapStatusText(status: MapStatus): String? = when (status) {
    MapStatus.PREPARING -> stringResource(R.string.reco_map_preparing)
    MapStatus.LOCATING -> stringResource(R.string.reco_map_locating)
    MapStatus.NO_LOCATION -> stringResource(R.string.reco_map_no_location)
    MapStatus.OUTSIDE_KOREA -> stringResource(R.string.reco_map_outside)
    MapStatus.EMPTY_RANGE -> stringResource(R.string.reco_map_empty)
    MapStatus.READY, MapStatus.FAILED -> null
}

/** 지도 위 한 줄 — 정지 문구(자동 회전 · 깜박임 없음). */
@Composable
private fun MapStatusLine(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = Dimens.ItemSpacing),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.CardBorderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.CardContentSpacing),
        )
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

private const val ATTRIBUTION_ALPHA = 0.85f
