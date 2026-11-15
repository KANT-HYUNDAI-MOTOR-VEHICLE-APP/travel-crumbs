package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.NearbyRoadmapRules
import com.example.travelcrumbs.ui.home.NearbyRoadmapCardItem
import com.example.travelcrumbs.ui.home.NearbyRoadmapsSection
import com.example.travelcrumbs.domain.model.NearbySectionState
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.AppStateKind
import com.example.travelcrumbs.ui.components.AppStatePanel
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.NeutralFilledButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 기록 칸 「내 근처 리뷰 많은 로드맵」 구역(#55 · 스펙 home-nearby 2-3 ~ 2-8) — 논리 상태가 숨김이면 아무것도 그리지 않음 ·
 * 동의 필요면 동의 카드(2-8) · 그 밖은 머리(제목 · 설명 · 「시연용 데이터」) + 가로 카드 목록.
 * 목록은 손가락으로 밀어 넘기고 놓으면 카드 한 장 단위로 왼쪽 끝에 맞춰 멈춤(E4) · 자동 넘김 · 돌아가는 표시 없음(2-1).
 * 카드 폭 고정(E5) · 목록이 새로 계산되면 첫 카드부터(E7). 카드 누름 = [onOpen](앱 틀이 「추천」 칸 B-04 로 · 3-4).
 */
@Composable
fun NearbyRoadmapsArea(
    section: NearbyRoadmapsSection,
    onOpen: (tripNo: Long, sidoNm: String) -> Unit,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!section.state.visible) return
    if (section.state == NearbySectionState.CONSENT_NEEDED) {
        NearbyConsentCard(onAgree = onAgree, onDecline = onDecline, modifier = modifier.testTag(NEARBY_SECTION_TAG))
        return
    }
    AppCard(
        modifier = modifier
            .fillMaxSize()
            .testTag(NEARBY_SECTION_TAG),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (section.state == NearbySectionState.NATIONWIDE) R.string.home_nearby_title_nationwide else R.string.home_nearby_title,
                ),
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (section.state) {
            NearbySectionState.NEARBY -> SectionDescription(stringResource(R.string.home_nearby_desc, section.radiusKm))
            NearbySectionState.NATIONWIDE -> SectionDescription(stringResource(R.string.home_nearby_desc_nationwide, section.radiusKm))
            else -> Unit
        }
        when (section.state) {
            NearbySectionState.LOCATING -> AppStatePanel(
                title = stringResource(R.string.home_nearby_locating),
                kind = AppStateKind.Loading,
            )
            NearbySectionState.NO_ROADMAP -> AppStatePanel(
                title = stringResource(R.string.home_nearby_empty),
                kind = AppStateKind.Empty,
            )
            NearbySectionState.LOCATION_UNAVAILABLE -> AppStatePanel(
                title = stringResource(R.string.home_nearby_unavailable),
                kind = AppStateKind.Error,
            )
            else ->
            Column(modifier = Modifier.fillMaxWidth().testTag(NEARBY_LIST_TAG)) {
                section.cards.take(NEARBY_COMPACT_LIMIT).forEach { card -> NearbyRoadmapCard(card = card, onClick = { onOpen(card.tripNo, card.sidoNm) }) }
            }
        }
    }
}

/**
 * 근처 로드맵 위치 이용 동의 카드(2-8 K1 ~ K5) — A-06 추천 공개 동의 블록 꼴(카드 · 제목 · 4줄 본문) + 버튼 둘.
 * 누름으로만 동의 이력에 행이 쌓임 · 보이는 동안 위치 조회 0회.
 */
@Composable
private fun NearbyConsentCard(onAgree: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
            Text(
                text = stringResource(R.string.nearby_consent_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.nearby_consent_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            SecondaryButton(onClick = onDecline, modifier = Modifier.weight(1f).heightIn(min = Dimens.BasicButtonHeight)) {
                Text(text = stringResource(R.string.nearby_consent_decline), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            NeutralFilledButton(onClick = onAgree, modifier = Modifier.weight(1f).heightIn(min = Dimens.BasicButtonHeight)) {
                Text(text = stringResource(R.string.nearby_consent_agree), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectionDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CardRow(cards: List<NearbyRoadmapCardItem>, onOpen: (Long, String) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth().testTag(NEARBY_LIST_TAG)) {
        cards.take(NEARBY_COMPACT_LIMIT).forEach { card ->
            NearbyRoadmapCard(card = card, onClick = { onOpen(card.tripNo, card.sidoNm) })
        }
    }
}

/** 카드 한 장(C1 ~ C9) — 경로 미리보기 · 순위 · 이름 · 지역 · 별점 · 리뷰 수 · 거리. 1위는 B-02 와 같은 강조 테두리. 카드 전체가 한 번 터치. */
@Composable
private fun NearbyRoadmapCard(card: NearbyRoadmapCardItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val top = card.rank == 1
    val region = card.sigunguNm?.let { stringResource(R.string.reco_region_name, card.sidoNm, it) } ?: card.sidoNm
    val distance = distanceText(card.distM)
    val description = stringResource(
        R.string.home_nearby_card_description,
        card.rank,
        card.tripNm,
        region,
        card.ratingAvg,
        card.ratingCnt,
        distance,
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(nearbyCardTag(card.tripNo))
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceVariant,
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
                text = card.rank.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primary,
                maxLines = 1,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
                Text(
                    text = card.tripNm,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        region,
                        stringResource(R.string.home_nearby_rating, card.ratingAvg),
                        stringResource(R.string.home_nearby_review_cnt, card.ratingCnt),
                        distance,
                    ).joinToString(stringResource(R.string.reco_meta_separator)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 「여기서 3.2km」 — 수치 꼴은 도메인([NearbyRoadmapRules.distanceKm] · N16). */
@Composable
private fun distanceText(distM: Int): String {
    val km = NearbyRoadmapRules.distanceKm(distM)
    return if (km.oneDecimal) {
        stringResource(R.string.home_nearby_distance_decimal, km.km)
    } else {
        stringResource(R.string.home_nearby_distance_whole, km.km.toInt())
    }
}

/** 시험이 구역 · 목록 · 카드를 찾는 이름표. */
const val NEARBY_SECTION_TAG = "home_nearby_section"
const val NEARBY_LIST_TAG = "home_nearby_list"

private const val NEARBY_COMPACT_LIMIT = 3

fun nearbyCardTag(tripNo: Long): String = "home_nearby_card_$tripNo"
