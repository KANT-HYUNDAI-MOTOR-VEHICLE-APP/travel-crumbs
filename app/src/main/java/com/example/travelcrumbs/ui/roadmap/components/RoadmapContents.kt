package com.example.travelcrumbs.ui.roadmap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.BackButton
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.RoadmapPlaceCard
import com.example.travelcrumbs.ui.components.RouteSketchBox
import com.example.travelcrumbs.ui.components.durationText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.roadmap.RoadmapReviewUiState
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * A-05 로드맵 검토 — 제목 「{여행명} · 로드맵」 · 기록 시간 · 장소 수 · 간소화 경로 · 장소 카드(누르면 A-03 시트) · 「로드맵 저장」.
 * 넓은 칸: 경로 왼쪽 · 장소 오른쪽 / 좁은 칸: 경로 위 · 장소 아래(세로 스크롤).
 */
@Composable
fun ReviewContent(
    state: RoadmapReviewUiState,
    onBack: () -> Unit,
    onEditPlace: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack = onBack)
            Text(
                text = stringResource(R.string.review_title, tripNameText(state.tripNm)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = durationText(state.durationMinutes) + stringResource(R.string.reco_meta_separator) +
                stringResource(R.string.trip_place_cnt_short, state.placeCnt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= Dimens.RoadmapWideMinWidth
            if (wide) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    RouteSketchBox(pins = state.sketch, modifier = Modifier.weight(Dimens.DetailRouteWeight).fillMaxHeight())
                    PlaceList(state, onEditPlace, Modifier.weight(1f - Dimens.DetailRouteWeight).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    RouteSketchBox(pins = state.sketch, modifier = Modifier.fillMaxWidth().height(Dimens.RoadmapSketchHeight))
                    state.places.forEach { place ->
                        RoadmapPlaceCard(item = place, onEdit = { onEditPlace(place.tripPlaceNo) })
                    }
                }
            }
        }
        PrimaryButton(onClick = onSave, modifier = Modifier.fillMaxWidth().height(Dimens.HomeButtonHeight)) {
            Text(text = stringResource(R.string.review_save), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun PlaceList(state: RoadmapReviewUiState, onEditPlace: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        state.places.forEach { place ->
            RoadmapPlaceCard(item = place, onEdit = { onEditPlace(place.tripPlaceNo) })
        }
    }
}

/**
 * A-06 저장 · 공개 — 「로드맵을 저장할게요」 · 비공개(나만 볼 수 있어요) / 공개(추천에 닉네임 「x」로 보여요) ·
 * 처음 공개면 추천 공개 동의 카드 · 미리보기 카드(여행명 · 닉네임 · 지역 · 장소 수 · 대표 태그) · 「정확한 위치 기록과 시각은 공개되지 않아요」 · 「저장」.
 */
@Composable
fun PublishContent(
    state: RoadmapReviewUiState,
    onBack: () -> Unit,
    onChoosePublic: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack = onBack)
                Text(
                    text = stringResource(R.string.publish_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            ChoiceRow(
                title = stringResource(R.string.publish_private),
                sub = stringResource(R.string.publish_private_sub),
                selected = !state.publicChosen,
                onClick = { onChoosePublic(false) },
            )
            ChoiceRow(
                title = stringResource(R.string.publish_public),
                sub = stringResource(R.string.publish_public_sub, state.nicknameNm),
                selected = state.publicChosen,
                onClick = { onChoosePublic(true) },
            )
            if (state.publicChosen && state.recoConsentNeeded) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.publish_consent_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.publish_consent_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                    )
                }
            }
            Text(
                text = stringResource(R.string.publish_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tripNameText(state.tripNm),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = state.nicknameNm,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                }
                val separator = stringResource(R.string.reco_meta_separator)
                val region = listOfNotNull(state.regionSidoNm, state.regionSigunguNm).joinToString(" ")
                    .ifEmpty { stringResource(R.string.region_unknown) }
                Text(
                    text = listOfNotNull(
                        region,
                        stringResource(R.string.trip_place_cnt_short, state.placeCnt),
                        state.representativeTagNm,
                    ).joinToString(separator),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.publish_privacy_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        PrimaryButton(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(top = Dimens.ItemSpacing).height(Dimens.HomeButtonHeight)) {
            Text(
                text = stringResource(
                    if (state.publicChosen && state.recoConsentNeeded) R.string.action_consent_and_save else R.string.action_save,
                ),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** 비공개 · 공개 고르는 카드 한 장(라디오) — 카드 전체가 한 번 터치. */
@Composable
private fun ChoiceRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TripThumbSize)
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else colors.surface)
            .border(if (selected) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth, if (selected) colors.secondary else colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Dimens.CardPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1)
            Text(text = sub, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
