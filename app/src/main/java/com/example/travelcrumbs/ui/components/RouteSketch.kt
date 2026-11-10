package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.ui.theme.Dimens

/** 장소 순서 번호 동그라미 — Brand/Primary 바탕 + Text/OnPrimary(대비 8.9). */
@Composable
fun OrderBadge(order: Int, modifier: Modifier = Modifier) {
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

/** 경로 그림 — 장소 순서대로 점선 · 번호 핀. 실제 지도 아님(좌표의 상대 위치만 · 지도 SDK 는 범위 밖). 추천 B-04 · 홈 근처 로드맵 카드(#55)가 같이 씀. */
@Composable
fun RouteSketch(points: List<SketchPoint>, modifier: Modifier = Modifier) {
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
