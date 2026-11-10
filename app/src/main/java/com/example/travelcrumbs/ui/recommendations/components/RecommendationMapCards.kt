package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.recommendations.MapCandidateItem
import com.example.travelcrumbs.ui.recommendations.MapPlaceCardItem
import com.example.travelcrumbs.ui.theme.Dimens

/*
 * 추천 지도(#59 · 스펙 2-3) 위 카드 — 장소 카드 · 묶음 후보 목록 · 출처 안내. 지도와 같은 화면 안에 하나만.
 * 제목 · 두 주요 행동(로드맵 보기 · 여기로 안내)은 카드 맨 위 · 맨 아래에 고정 · 가운데(로드맵 후보)만 스크롤 — 창이 작아도 행동에 닿게(M28).
 * 좌표 · 기록 시각 · 계정 ID 표시 없음.
 */

/** 낱개 핀을 누른 장소 카드 — 장소명 · 태그(없으면 숨김 · M13) · 로드맵 후보(리뷰 많은 순 · M17) · 두 행동. */
@Composable
fun MapPlaceCard(
    card: MapPlaceCardItem,
    guidanceRequesting: Boolean,
    onSelectRoadmap: (Long) -> Unit,
    onOpenRoadmap: () -> Unit,
    onGuide: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapCardFrame(title = card.placeNm, onClose = onClose, modifier = modifier) {
        val selected = card.selectedRoadmap
        Text(
            text = stringResource(R.string.reco_map_by, selected.nicknameNm),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (card.tagNms.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
                card.tagNms.forEach { tag -> Text(text = "#$tag", style = MaterialTheme.typography.bodyLarge) }
            }
        }
        RecoSectionLabel(stringResource(R.string.reco_map_roadmaps_label))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
        ) {
            card.roadmaps.forEach { roadmap ->
                MapChoiceRow(
                    title = roadmap.tripNm,
                    sub = stringResource(R.string.reco_rating, roadmap.ratingAvg, roadmap.ratingCnt),
                    selected = roadmap.tripNo == card.selectedTripNo,
                    onClick = { onSelectRoadmap(roadmap.tripNo) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            OutlinedButton(
                onClick = onOpenRoadmap,
                modifier = Modifier.weight(1f).heightIn(min = Dimens.HomeButtonHeight),
            ) {
                Text(stringResource(R.string.reco_map_open_roadmap), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Button(
                onClick = onGuide,
                enabled = !guidanceRequesting,
                modifier = Modifier.weight(1f).heightIn(min = Dimens.HomeButtonHeight),
            ) {
                Text(stringResource(R.string.reco_guide_here), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

/** 묶음 핀 후보 목록 — 장소명 · 로드맵 수(리뷰 많은 순 · M35) · 누르면 그 장소 카드. */
@Composable
fun MapCandidateList(
    candidates: List<MapCandidateItem>,
    onSelect: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapCardFrame(
        title = stringResource(R.string.reco_map_candidates_title, candidates.size),
        onClose = onClose,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
        ) {
            candidates.forEach { candidate ->
                MapChoiceRow(
                    title = candidate.placeNm,
                    sub = stringResource(R.string.reco_trip_cnt, candidate.roadmapCnt),
                    selected = false,
                    onClick = { onSelect(candidate.placeNo) },
                )
            }
        }
    }
}

/** 출처 · 라이선스 안내(M32) — 「© OpenStreetMap contributors」를 누르면. */
@Composable
fun MapLicenseCard(onClose: () -> Unit, modifier: Modifier = Modifier) {
    MapCardFrame(title = stringResource(R.string.reco_map_license_title), onClose = onClose, modifier = modifier) {
        listOf(
            R.string.reco_map_license_osm,
            R.string.reco_map_license_ne,
            R.string.reco_map_license_maplibre,
            R.string.reco_map_license_offline,
        ).forEach { line ->
            Text(text = stringResource(line), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** 지도 위 카드 틀 — 제목 한 줄 + 닫기 · 내용. */
@Composable
private fun MapCardFrame(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose, modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)) {
                    Text(stringResource(R.string.reco_map_close), style = MaterialTheme.typography.titleMedium)
                }
            }
            content()
        }
    }
}

/** 고르는 줄(로드맵 후보 · 장소 후보) — 고른 줄은 테두리 강조. */
@Composable
private fun MapChoiceRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .semantics { this.selected = selected },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surface,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        border = BorderStroke(if (selected) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth, if (selected) colors.secondary else colors.outline),
    ) {
        Column(modifier = Modifier.padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.CardContentSpacing)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = sub, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}
