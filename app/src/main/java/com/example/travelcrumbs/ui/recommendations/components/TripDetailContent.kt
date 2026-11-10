package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.ui.components.OrderBadge
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.RoadmapMap
import com.example.travelcrumbs.ui.components.RouteSketch
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.recommendations.PlaceItem
import com.example.travelcrumbs.ui.recommendations.TripDetailItem
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * B-04 추천 로드맵 상세 — 여행명 · 별점(안내받은 로드맵이면 「별점 주기」 / 「내 별점 ★n · 고치기」 · #41) · 닉네임 · 동행자 · 장소 수 · 지역 / 경로 그림 · 장소 순서(태그 · 「여기로 안내」) /
 * 맨 아래 주요 행동 하나 「첫 장소부터 안내」(Primary). 로드맵 전체 경유지 안내는 하지 않음(장소 하나씩 · 이슈 범위 밖).
 */
@Composable
fun TripDetailContent(
    detail: TripDetailItem,
    onGuideFromFirst: () -> Unit,
    onGuideHere: (Long) -> Unit,
    onOpenRating: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecoTitle(detail.tripNm, modifier = Modifier.weight(1f))
            RatingText(detail.ratingAvg, detail.ratingCnt)
            if (detail.canRate) {
                // 안내받은 로드맵만(#41) — 주요 행동은 맨 아래 「첫 장소부터 안내」 하나라 Secondary
                SecondaryButton(
                    onClick = onOpenRating,
                    modifier = Modifier.heightIn(min = Dimens.ChipHeight),
                ) {
                    Text(
                        text = detail.myRatingScore
                            ?.let { stringResource(R.string.rating_mine, it) }
                            ?: stringResource(R.string.rating_open),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detail.nicknameNm,
                style = MaterialTheme.typography.titleMedium,
                color = colors.secondary,
                maxLines = 1,
            )
            Text(
                text = metaText(detail),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            val routeModifier = Modifier
                .weight(Dimens.DetailRouteWeight)
                .fillMaxHeight()
            // 앱에 묶은 도로 지도가 이 로드맵을 덮으면 지도 · 아니면 기존 경로 그림(점선)
            val routeMap = detail.routeMap
            if (routeMap != null) {
                RoadmapMap(map = routeMap, modifier = routeModifier)
            } else {
                RouteSketch(points = detail.sketch, modifier = routeModifier)
            }
            Column(
                modifier = Modifier
                    .weight(1f - Dimens.DetailRouteWeight)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                detail.places.forEach { place ->
                    PlaceRow(place = place, onGuideHere = { onGuideHere(place.placeNo) })
                }
            }
        }
        PrimaryButton(
            onClick = onGuideFromFirst,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HomeButtonHeight),
        ) {
            Text(text = stringResource(R.string.reco_guide_from_first), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun metaText(detail: TripDetailItem): String {
    val separator = stringResource(R.string.reco_meta_separator)
    val companions = detail.companionTypes.map { stringResource(it.labelRes()) }
    val region = detail.sigunguNm
        ?.let { stringResource(R.string.reco_region_name, detail.sidoNm, it) }
        ?: detail.sidoNm
    return (companions + stringResource(R.string.reco_place_cnt, detail.placeCnt) + region).joinToString(separator)
}

private fun CompanionType.labelRes(): Int = when (this) {
    CompanionType.SOLO -> R.string.companion_solo
    CompanionType.SPOUSE -> R.string.companion_spouse
    CompanionType.CHILD -> R.string.companion_child
    CompanionType.PARENT -> R.string.companion_parent
    CompanionType.PET -> R.string.companion_pet
}

@Composable
private fun PlaceRow(place: PlaceItem, onGuideHere: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.ItemSpacing),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrderBadge(place.placeOrd)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.placeNm,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.tagNms.isNotEmpty()) {
                    Text(
                        text = place.tagNms.joinToString(stringResource(R.string.reco_meta_separator)),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SecondaryButton(
                onClick = onGuideHere,
                modifier = Modifier
                    .widthIn(min = Dimens.GuideHereButtonMinWidth)
                    .heightIn(min = Dimens.ChipHeight),
            ) {
                Text(text = stringResource(R.string.reco_guide_here), style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
        }
    }
}
