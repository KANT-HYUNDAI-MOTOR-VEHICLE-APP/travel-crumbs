package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.MapLayer
import com.example.travelcrumbs.domain.model.MapXY
import com.example.travelcrumbs.domain.model.RouteMap
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.RoadmapMapColors
import kotlin.math.min

/**
 * B-04 경로 칸의 시연용 도로 지도 — 앱에 묶은 OpenStreetMap 자료 위에 장소 순서대로 도로 구간 · 번호 핀.
 * 지도 · 위치 · 네트워크를 쓰지 않음(계산은 도메인 RoadmapMapRules). 출처 줄 · 「시연용」 한 줄을 늘 함께 보임.
 */
@Composable
fun RoadmapMap(map: RouteMap, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.roadmap_map_description, map.areaLabel, map.pins.size)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(Dimens.CardBorderWidth, colors.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .semantics { contentDescription = description },
            ) {
                RoadDrawing(map = map, modifier = Modifier.fillMaxSize())
                Text(
                    text = map.areaLabel,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Dimens.RoadmapMapLabelPadding)
                        .background(colors.background, MaterialTheme.shapes.small)
                        .padding(horizontal = Dimens.RoadmapMapLabelPadding, vertical = Dimens.RoadmapMapLabelPadding / 2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.roadmap_map_osm_credit),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(colors.background)
                        .padding(horizontal = Dimens.RoadmapMapLabelPadding, vertical = Dimens.RoadmapMapLabelPadding / 4),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurface,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.roadmap_map_sample),
                modifier = Modifier.padding(Dimens.RoadmapMapLabelPadding),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoadDrawing(map: RouteMap, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val route = colors.primary
    val outline = colors.background
    val pinText = colors.onPrimary
    val textMeasurer = rememberTextMeasurer()
    val pinStyle = MaterialTheme.typography.titleLarge.copy(color = pinText)
    Box(
        modifier = modifier.drawWithCache {
            val pinRadius = Dimens.SketchPinSize.toPx() / 2
            val view = MapView(
                map = map,
                canvas = size,
                side = pinRadius + Dimens.RoadmapMapEdgePadding.toPx(),
                // 위 · 아래는 지역 이름 · 출처 줄이 핀을 가리지 않게 더 비움
                top = pinRadius + Dimens.RoadmapMapLabelInset.toPx(),
                bottom = pinRadius + Dimens.RoadmapMapLabelInset.toPx(),
            )
            val layers = map.layers.mapValues { (_, lines) -> view.path(lines) }
            val roadLegs = view.path(map.legs.filter { it.onRoad }.map { it.points })
            val straightLegs = view.path(map.legs.filterNot { it.onRoad }.map { it.points })
            val pins = map.pins.map(view::position)
            val routeWidth = Dimens.SketchLineWidth.toPx()
            val routeStroke = Stroke(routeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val outlineStroke = Stroke(routeWidth * 2, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val dashed = Stroke(
                routeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(routeWidth * 2, routeWidth * 2)),
            )
            val labels = pins.indices.map { textMeasurer.measure((it + 1).toString(), pinStyle) }
            onDrawBehind {
                drawRect(RoadmapMapColors.Background)
                MapLayer.entries.forEach { layer ->
                    val path = layers[layer] ?: return@forEach
                    when (layer) {
                        MapLayer.WATER -> drawPath(path, RoadmapMapColors.Water)
                        MapLayer.PARK -> drawPath(path, RoadmapMapColors.Park)
                        MapLayer.RIVER -> drawPath(path, RoadmapMapColors.Water, style = Stroke(Dimens.RoadmapMapRiverWidth.toPx(), cap = StrokeCap.Round))
                        MapLayer.SHORE -> drawPath(path, RoadmapMapColors.Shore, style = Stroke(Dimens.RoadmapMapShoreWidth.toPx()))
                        MapLayer.LOCAL -> drawPath(path, RoadmapMapColors.LocalRoad, style = Stroke(Dimens.RoadmapMapLocalRoadWidth.toPx()))
                        MapLayer.STREET -> drawPath(path, RoadmapMapColors.Street, style = Stroke(Dimens.RoadmapMapStreetWidth.toPx()))
                        MapLayer.MAJOR -> drawPath(path, RoadmapMapColors.MajorRoad, style = Stroke(Dimens.RoadmapMapMajorRoadWidth.toPx()))
                    }
                }
                drawPath(roadLegs, outline, style = outlineStroke)
                drawPath(roadLegs, route, style = routeStroke)
                drawPath(straightLegs, route, style = dashed)
                pins.forEachIndexed { index, center ->
                    drawCircle(outline, pinRadius + Dimens.RoadmapMapPinOutline.toPx(), center)
                    drawCircle(route, pinRadius, center)
                    val label = labels[index]
                    drawText(
                        label,
                        topLeft = Offset(center.x - label.size.width / 2f, center.y - label.size.height / 2f),
                    )
                }
            }
        },
    )
}

/** 도메인이 옮긴 평면 점([MapXY])을 캔버스에 — 장소 · 구간이 여백 안에 다 들어가는 배율 하나. */
private class MapView(map: RouteMap, canvas: Size, side: Float, top: Float, bottom: Float) {
    private val centerX = canvas.width / 2
    private val centerY = top + (canvas.height - top - bottom) / 2
    private val scale = min(
        ((canvas.width / 2 - side).coerceAtLeast(canvas.width / 4)) / map.halfWidth,
        ((canvas.height - top - bottom).coerceAtLeast(canvas.height / 2) / 2) / map.halfHeight,
    )

    fun position(point: MapXY) = Offset(centerX + point.x * scale, centerY + point.y * scale)

    fun path(lines: List<List<MapXY>>): Path = Path().apply {
        lines.forEach { line ->
            if (line.size < 2) return@forEach
            val first = position(line.first())
            moveTo(first.x, first.y)
            line.drop(1).forEach { point ->
                val next = position(point)
                lineTo(next.x, next.y)
            }
        }
    }
}
