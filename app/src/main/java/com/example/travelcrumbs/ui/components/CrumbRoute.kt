package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.domain.model.GeoPoint
import com.example.travelcrumbs.domain.model.TripRoute
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TcColors
import kotlin.math.cos
import kotlin.math.max

/**
 * Crumb 경로 — 지시서 17장을 지도 없이 점 · 선으로(지도는 나중에).
 * 이동 경로 = Oat Beige 점선 + 작은 Crumb 노드 · 출발지 = Oat Beige 링 · 중간 장소 = Secondary 핀
 * · 도착지(기록 중이면 지금 위치) = Primary 큰 핀 · 임시 기록 장소 = Lavender 링.
 * 위도 · 경도를 상자에 맞춰 늘리고 줄임(경도는 위도에 따라 좁혀 모양 유지). 경로가 없으면 [emptyText].
 */
@Composable
fun CrumbRoute(
    route: TripRoute?,
    emptyText: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(TcColors.BackgroundSurface)
            .border(Dimens.BorderWidth, TcColors.BorderSubtle, shape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid()
            if (route != null && !route.isEmpty) drawRoute(route)
        }
        if (route == null || route.isEmpty) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = TcColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Dimens.ScreenPadding),
            )
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = TcColors.TextSecondary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Dimens.CardPadding),
            )
        }
    }
}

/** 지도 대신 옅은 점 격자 — 「지도 자리」라는 느낌만. */
private fun DrawScope.drawGrid() {
    val step = 48.dp.toPx()
    val r = 1.5.dp.toPx()
    var x = step / 2
    while (x < size.width) {
        var y = step / 2
        while (y < size.height) {
            drawCircle(TcColors.BorderSubtle, radius = r, center = Offset(x, y))
            y += step
        }
        x += step
    }
}

private fun DrawScope.drawRoute(route: TripRoute) {
    val all = route.points + route.places.map { GeoPoint(it.lat, it.lng) }
    val minLat = all.minOf { it.lat }
    val maxLat = all.maxOf { it.lat }
    val minLng = all.minOf { it.lng }
    val maxLng = all.maxOf { it.lng }
    val lngScale = cos(Math.toRadians((minLat + maxLat) / 2))
    val spanX = max((maxLng - minLng) * lngScale, MIN_SPAN)
    val spanY = max(maxLat - minLat, MIN_SPAN)
    val inset = 56.dp.toPx()
    val w = size.width - inset * 2
    val h = size.height - inset * 2
    val scale = minOf(w / spanX, h / spanY)
    // 가운데 정렬
    val offX = inset + (w - spanX * scale) / 2
    val offY = inset + (h - spanY * scale) / 2
    fun project(p: GeoPoint) = Offset(
        x = (offX + (p.lng - minLng) * lngScale * scale).toFloat(),
        y = (offY + (maxLat - p.lat) * scale).toFloat(),
    )

    val line = route.points.map(::project)
    if (line.size >= 2) {
        val path = Path().apply {
            moveTo(line.first().x, line.first().y)
            line.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = TcColors.RouteOatBeige,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 14.dp.toPx())),
            ),
        )
        // Crumb 노드 — 너무 많으면 건너뜀
        val every = max(1, line.size / MAX_CRUMBS)
        line.filterIndexed { i, _ -> i % every == 0 }.forEach {
            drawCircle(TcColors.RouteOatBeige, radius = 5.dp.toPx(), center = it)
        }
    }

    // 출발지 — Oat Beige 링
    val start = line.firstOrNull() ?: route.places.firstOrNull()?.let { project(GeoPoint(it.lat, it.lng)) }
    start?.let { ring(it, TcColors.RouteOatBeige, 14.dp.toPx()) }

    // 장소 핀 — 입력 완료는 Secondary, 임시 기록은 Lavender 링
    route.places.forEach { pin ->
        val c = project(GeoPoint(pin.lat, pin.lng))
        if (pin.isDraft) {
            ring(c, TcColors.BrandLavender, 14.dp.toPx())
        } else {
            drawCircle(TcColors.BackgroundBase, radius = 18.dp.toPx(), center = c)
            drawCircle(TcColors.BrandSecondary, radius = 14.dp.toPx(), center = c)
        }
    }

    // 도착지 · 지금 위치 — Primary 큰 핀
    line.lastOrNull()?.takeIf { line.size >= 2 }?.let { end ->
        drawCircle(TcColors.BrandPrimary.copy(alpha = 0.3f), radius = 28.dp.toPx(), center = end)
        drawCircle(TcColors.BackgroundBase, radius = 20.dp.toPx(), center = end)
        drawCircle(TcColors.BrandPrimary, radius = 16.dp.toPx(), center = end)
    }
}

private fun DrawScope.ring(center: Offset, color: androidx.compose.ui.graphics.Color, radius: Float) {
    drawCircle(TcColors.BackgroundBase, radius = radius + 4.dp.toPx(), center = center)
    drawCircle(color, radius = radius, center = center, style = Stroke(width = 5.dp.toPx()))
}

/** 경로가 한 점뿐일 때도 그릴 수 있도록 최소 범위(도 단위 · 약 100m). */
private const val MIN_SPAN = 0.001
private const val MAX_CRUMBS = 40
