package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.RoadmapRules
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.ui.theme.Dimens

/** 내 로드맵 장소 한 줄(A-05 · A-08 · #51) — 좌표 없음. */
data class RoadmapPlaceItem(
    val tripPlaceNo: Long,
    val placeOrd: Int,
    /** 사용자가 정한 이름 — 없으면 「장소 n」. */
    val userPlaceNm: String?,
    val tagNms: List<String>,
    val memo: String?,
    /** 임시저장(태그 안 단 장소) — 「미완성」 · 「+ 태그 달기」. */
    val draft: Boolean,
)

/** 경로 그림 핀 — 상대 위치(0 ~ 1) · 장소 순서. */
data class SketchPinItem(val x: Float, val y: Float, val placeOrd: Int)

/** 여행 장소 → 화면 줄(태그 번호 → 이름은 [tagNames] · 모르는 번호는 빠짐). */
fun TripRecord.roadmapPlaces(tagNames: Map<Long, String>): List<RoadmapPlaceItem> = places.sortedBy { it.placeOrd }.map { place ->
    RoadmapPlaceItem(
        tripPlaceNo = place.tripPlaceNo,
        placeOrd = place.placeOrd,
        userPlaceNm = place.userPlaceNm,
        tagNms = place.tagNos.mapNotNull { tagNames[it] },
        memo = place.memo,
        draft = place.draft,
    )
}

/** 여행 → 경로 그림 핀(좌표는 상대 위치로만). */
fun TripRecord.sketchPins(): List<SketchPinItem> =
    RoadmapRules.sketch(places).map { SketchPinItem(it.point.x, it.point.y, it.placeOrd) }

/** 「4시간 20분」 · 「35분」 — 기록한 시간(분). */
@Composable
fun durationText(totalMinutes: Long): String {
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

private const val MINUTES_PER_HOUR = 60L

/**
 * 간소화 경로(A-05 · A-08) — 장소 좌표를 순서대로 잇는 점선 + 번호 핀. 지도 · 가짜 배경 없음 · 움직임 없음.
 * 좌표가 하나도 없으면 「위치 기록이 없어요」 한 줄.
 */
@Composable
fun RouteSketchBox(pins: List<SketchPinItem>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(Dimens.CardBorderWidth, colors.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (pins.isEmpty()) {
            Text(
                text = stringResource(R.string.roadmap_no_location),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
            return@BoxWithConstraints
        }
        val lineColor = colors.secondary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = pins.map { Offset(it.x * size.width, it.y * size.height) }
            points.zipWithNext().forEach { (a, b) ->
                drawLine(
                    color = lineColor,
                    start = a,
                    end = b,
                    strokeWidth = Dimens.SketchLineWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 12.dp.toPx())),
                )
            }
        }
        val pin = Dimens.SketchPinSize
        pins.forEach { item ->
            OrderPin(
                order = item.placeOrd,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * item.x - pin / 2, y = maxHeight * item.y - pin / 2),
            )
        }
    }
}

/** 순서 번호 동그라미. */
@Composable
fun OrderPin(order: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.SketchPinSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

/**
 * 로드맵 장소 카드 — 번호 · 이름 · 태그(없으면 「+ 태그 달기」) · 「미완성」 표시. 카드 전체가 한 번 터치 → A-03 시트로 고침.
 */
@Composable
fun RoadmapPlaceCard(
    item: RoadmapPlaceItem,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TripThumbSize)
            .clip(shape)
            .background(colors.surface)
            .border(
                if (item.draft) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth,
                if (item.draft) colors.primary else colors.outlineVariant,
                shape,
            )
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(Dimens.CardPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrderPin(order = item.placeOrd)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
            Text(
                text = item.userPlaceNm ?: stringResource(R.string.roadmap_place_default, item.placeOrd),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.tagNms.isEmpty()) {
                Text(
                    text = stringResource(R.string.roadmap_add_tag),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = item.tagNms.joinToString(stringResource(R.string.reco_meta_separator)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.draft) {
            Text(
                text = stringResource(R.string.roadmap_draft_badge),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
                maxLines = 1,
            )
        }
    }
}
