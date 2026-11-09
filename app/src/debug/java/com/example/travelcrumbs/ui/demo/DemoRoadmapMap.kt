package com.example.travelcrumbs.ui.demo

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.data.demo.DemoMapArea
import com.example.travelcrumbs.data.demo.MapPoint
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.ui.theme.TcColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun DemoRoute(
    stops: List<DemoStop>,
    showStops: Boolean = true,
    simulatedProgress: Int? = null,
) {
    val model: DemoMapViewModel = viewModel()
    val state by model.uiState.collectAsStateWithLifecycle()
    val positions = stops.mapNotNull(DemoStop::navigationTarget)
    val simulated = simulatedProgress?.let { step -> seoulStops.take(step).mapNotNull(DemoStop::navigationTarget) }
        .orEmpty()
    val positionsForArea = (positions + simulated).map(Place::mapPoint)
    val simulatedLocation = simulated.lastOrNull()?.takeUnless { location -> positions.any { it.id == location.id } }
    val map = (state as? DemoMapState.Ready)?.maps?.areaFor(positionsForArea)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            map != null -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 800.dp && stops.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MapPane(map, positions, simulated, Modifier.weight(1.9f))
                        MapLegend(stops, showStops, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MapPane(map, positions, simulated, Modifier.fillMaxWidth())
                        MapLegend(stops, showStops)
                    }
                }
            }
            state == DemoMapState.Loading -> Text(stringResource(R.string.demo_map_loading))
            else -> Text(stringResource(R.string.demo_map_unavailable))
        }
        if (map != null && simulatedLocation != null) {
            Text(
                stringResource(R.string.demo_map_sample_location, simulatedLocation.name),
                style = MaterialTheme.typography.bodyMedium,
                color = TcColors.BrandLavender,
            )
        }
        Text(stringResource(R.string.demo_map_sample), style = MaterialTheme.typography.bodyMedium, color = TcColors.TextSecondary)
    }
}

@Composable
private fun MapLegend(stops: List<DemoStop>, showDetails: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (stops.isNotEmpty()) {
            Text(stringResource(R.string.demo_map_stops), style = MaterialTheme.typography.titleMedium)
        }
        stops.forEachIndexed { index, stop ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${index + 1}", color = TcColors.BrandPrimary, style = MaterialTheme.typography.titleMedium)
                Column {
                    Text(stop.name, style = MaterialTheme.typography.titleMedium)
                    if (showDetails) {
                        Text((listOf(stop.category) + stop.tags).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        if (stop.memo.isNotBlank()) Text(stop.memo, style = MaterialTheme.typography.bodyMedium, color = TcColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPane(area: DemoMapArea, stops: List<Place>, simulated: List<Place>, modifier: Modifier) {
    val uri = LocalUriHandler.current
    Box(modifier.height(324.dp).clip(RoundedCornerShape(16.dp))) {
        Box(Modifier.matchParentSize().roadmapDrawing(area, stops, simulated))
        Text(
            area.label,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                .background(TcColors.BackgroundBase, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(if (area.overview) R.string.demo_map_overview_credit else R.string.demo_map_osm_credit),
            modifier = Modifier.align(Alignment.BottomEnd).background(TcColors.BackgroundBase)
                .clickable { runCatching { uri.openUri("https://www.openstreetmap.org/copyright") } }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TcColors.TextPrimary,
        )
    }
}

private val mapBase = Color(0xFF17262A)
private val land = Color(0xFF30403D)
private val water = Color(0xFF1B3948)
private val park = Color(0xFF24483D)
private val shore = Color(0xFF537D88)
private val localRoad = Color(0xFF465553)
private val street = Color(0xFF71817B)
private val majorRoad = Color(0xFF9C9F85)

private fun Modifier.roadmapDrawing(area: DemoMapArea, stops: List<Place>, simulated: List<Place>): Modifier =
    drawWithCache {
        val orderedPlaces = if (simulated.size > stops.size && simulated.take(stops.size) == stops) simulated else stops
        val routePlaces = orderedPlaces.ifEmpty { simulated }
        val roadSegments = routePlaces.zipWithNext().mapNotNull { (from, to) ->
            area.roadSegment(from.id, to.id).takeIf(List<MapPoint>::isNotEmpty)
        }
        val focus = (routePlaces + stops).map(Place::mapPoint) + roadSegments.flatten()
        val view = MapProjection(area, focus, size, horizontalPadding = 34.dp.toPx(), verticalPadding = 52.dp.toPx())
        val paths = area.layers.mapValues { (_, paths) -> view.path(paths) }
        val route = view.path(roadSegments)
        val markers = stops.map { view.position(it.mapPoint()) }
        val simulatedLocation = simulated.lastOrNull()?.takeUnless { location -> stops.any { it.id == location.id } }
            ?.let { view.position(it.mapPoint()) }
        val markerRadius = 15.dp.toPx()
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TcColors.BackgroundBase.toArgb()
            textSize = 17.dp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val projected = size
        onDrawBehind {
            drawRect(if (area.overview) water else mapBase, size = projected)
            paths["land"]?.let { drawPath(it, land) }
            paths["water"]?.let { drawPath(it, water) }
            paths["park"]?.let { drawPath(it, park) }
            paths["river"]?.let { drawPath(it, water, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)) }
            paths["shore"]?.let { drawPath(it, shore, style = Stroke(1.2.dp.toPx())) }
            paths["local"]?.let { drawPath(it, localRoad, style = Stroke(0.8.dp.toPx())) }
            paths["street"]?.let { drawPath(it, street, style = Stroke(1.3.dp.toPx())) }
            paths["major"]?.let { drawPath(it, majorRoad, style = Stroke(1.8.dp.toPx())) }
            drawPath(route, TcColors.BackgroundBase, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(route, TcColors.BrandPrimary, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            simulatedLocation?.let { position ->
                drawCircle(TcColors.BackgroundBase, 10.dp.toPx(), position)
                drawCircle(TcColors.BrandLavender, 7.dp.toPx(), position)
            }
            markers.forEachIndexed { index, position ->
                drawCircle(TcColors.BackgroundBase, markerRadius + 2.dp.toPx(), position)
                drawCircle(TcColors.BrandPrimary, markerRadius, position)
                drawContext.canvas.nativeCanvas.drawText(
                    (index + 1).toString(), position.x, position.y - (text.ascent() + text.descent()) / 2f, text,
                )
            }
        }
    }

private fun Place.mapPoint() = MapPoint(longitude, latitude)

/** 가까운 지도는 중심 위도의 경도 축을 보정하고, 모든 장소가 프레임 안에 남는 배율을 선택. */
private class MapProjection(
    area: DemoMapArea,
    focus: List<MapPoint>,
    private val canvas: Size,
    horizontalPadding: Float,
    verticalPadding: Float,
) {
    private val bounds = area.bounds
    private val centerLat = if (!area.overview && focus.size > 1) {
        (focus.minOf { it.latitude } + focus.maxOf { it.latitude }) / 2
    } else {
        (bounds.south + bounds.north) / 2
    }
    private val centerLon = if (!area.overview && focus.size > 1) {
        (focus.minOf { it.longitude } + focus.maxOf { it.longitude }) / 2
    } else {
        (bounds.west + bounds.east) / 2
    }
    private val longitudeFactor = cos(Math.toRadians(centerLat))
    private val worldWidth = (bounds.east - bounds.west) * longitudeFactor
    private val worldHeight = bounds.north - bounds.south
    private val widthRatio = canvas.width / worldWidth
    private val heightRatio = canvas.height / worldHeight
    private val focusX = focus.maxOfOrNull { abs(it.longitude - centerLon) * longitudeFactor } ?: 0.0
    private val focusY = focus.maxOfOrNull { abs(it.latitude - centerLat) } ?: 0.0
    private val usableHalfWidth = (canvas.width / 2 - horizontalPadding).coerceAtLeast(canvas.width / 4)
    private val usableHalfHeight = (canvas.height / 2 - verticalPadding).coerceAtLeast(canvas.height / 4)
    private val focusScale = min(
        usableHalfWidth / max(focusX, 0.00001),
        usableHalfHeight / max(focusY, 0.00001),
    )
    private val areaScale = if (area.overview) min(widthRatio, heightRatio) else max(widthRatio, heightRatio)
    private val scale = min(areaScale, focusScale)

    fun position(point: MapPoint) = Offset(
        (canvas.width / 2 + (point.longitude - centerLon) * longitudeFactor * scale).toFloat(),
        (canvas.height / 2 - (point.latitude - centerLat) * scale).toFloat(),
    )

    fun path(lines: List<List<MapPoint>>): Path = Path().apply {
        lines.forEach { line ->
            if (line.isEmpty()) return@forEach
            val first = position(line.first())
            moveTo(first.x, first.y)
            line.drop(1).forEach { point ->
                val offset = position(point)
                lineTo(offset.x, offset.y)
            }
        }
    }
}
