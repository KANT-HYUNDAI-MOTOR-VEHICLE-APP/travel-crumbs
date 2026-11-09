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
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.recommendations.PlaceItem
import com.example.travelcrumbs.ui.recommendations.TripDetailItem
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * B-04 추천 로드맵 상세 — 여행명 · 별점 · 닉네임 · 동행자 · 장소 수 · 지역 / 경로 그림 · 장소 순서(태그 · 「여기로 안내」) /
 * 맨 아래 주요 행동 하나 「첫 장소부터 안내」(Primary). 로드맵 전체 경유지 안내는 하지 않음(장소 하나씩 · 이슈 범위 밖).
 */
@Composable
fun TripDetailContent(
    detail: TripDetailItem,
    onGuideFromFirst: () -> Unit,
    onGuideHere: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecoTitle(detail.tripNm, modifier = Modifier.weight(1f))
            RatingText(detail.ratingAvg, detail.ratingCnt)
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
            RouteSketch(
                points = detail.sketch,
                modifier = Modifier
                    .weight(Dimens.DetailRouteWeight)
                    .fillMaxHeight(),
            )
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

/** 장소 순서 번호 동그라미 — Brand/Primary 바탕 + Text/OnPrimary(대비 8.9). */
@Composable
private fun OrderBadge(order: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.SketchPinSize)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** 경로 그림 — 장소 순서대로 점선 · 번호 핀. 실제 지도 아님(좌표의 상대 위치만 · 지도 SDK 는 범위 밖). */
@Composable
private fun RouteSketch(points: List<SketchPoint>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outlineVariant),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val grid = colors.outlineVariant
            val line = colors.secondary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Dimens.SketchLineWidth.toPx()
                for (i in 1 until SKETCH_GRID) {
                    val x = size.width * i / SKETCH_GRID
                    val y = size.height * i / SKETCH_GRID
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke / 2)
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke / 2)
                }
                val dash = PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))
                points.zipWithNext().forEach { (a, b) ->
                    drawLine(
                        color = line,
                        start = Offset(size.width * a.x, size.height * a.y),
                        end = Offset(size.width * b.x, size.height * b.y),
                        strokeWidth = stroke,
                        pathEffect = dash,
                    )
                }
            }
            points.forEachIndexed { index, point ->
                OrderBadge(
                    order = index + 1,
                    modifier = Modifier.offset(
                        x = maxWidth * point.x - Dimens.SketchPinSize / 2,
                        y = maxHeight * point.y - Dimens.SketchPinSize / 2,
                    ),
                )
            }
        }
    }
}

private const val SKETCH_GRID = 4
