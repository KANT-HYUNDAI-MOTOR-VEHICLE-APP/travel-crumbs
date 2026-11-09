package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.home.HomeUiState
import com.example.travelcrumbs.ui.home.PlaceItem
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 실패 폴백 버튼 줄(스펙 navigation 2.1 E11 · E12 · 와이어프레임 A-14) — 결과 문구(E3) 바로 아래 · 실패일 때만.
 * 「다시 시도」 활성은 「여기로 안내」와 같은 식 · 「더미 경로」는 debug 빌드만 · 패널이 열려 있으면 비활성.
 */
@Composable
fun RouteFallbackButtons(
    state: HomeUiState,
    onRetry: () -> Unit,
    onShowDummyRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.retryVisible) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        FallbackButton(text = stringResource(R.string.places_retry), enabled = state.retryEnabled, onClick = onRetry)
        if (state.dummyRouteVisible) {
            FallbackButton(
                text = stringResource(R.string.places_dummy_route),
                enabled = state.dummyRouteEnabled,
                onClick = onShowDummyRoute,
            )
        }
    }
}

/**
 * 더미 경로 패널(E13) — 「가 볼 장소」 구역 안 카드 하나 · 시연용 대체 화면(실제 길안내 · 경로 아님).
 * 정지 도식(출발 「현재 위치」 → 도착 장소 이름) · 주소 한 줄 · 「닫기」. 지도 타일 · 거리 · 시간 · 애니메이션 · 패널 안 스크롤 없음.
 * 「현재 위치」는 글자뿐 — 위치를 읽지 않음(U53).
 */
@Composable
fun DummyRoutePanel(
    place: PlaceItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DUMMY_ROUTE_PANEL_TAG),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                // 시연용 표시는 잘리지 않게 줄바꿈 허용 — 「실제 길안내가 아니에요」가 늘 보여야 함(심사 1 · 2)
                Text(
                    text = stringResource(R.string.dummy_route_notice),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                FallbackButton(text = stringResource(R.string.dummy_route_close), enabled = true, onClick = onClose)
            }
            RouteDiagram()
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                DiagramLabel(text = stringResource(R.string.dummy_route_origin), align = TextAlign.Start)
                DiagramLabel(text = place.name, align = TextAlign.End)
            }
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 출발 점 · 도착 점 · 두 점을 잇는 선 하나 — 고정 모양(실제 경로 모양 아님) · 그림 높이 고정. */
@Composable
private fun RouteDiagram() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.DummyRouteDiagramHeight),
    ) {
        val radius = Dimens.DummyRouteDotRadius.toPx()
        val y = size.height / 2
        val start = Offset(radius, y)
        val end = Offset(size.width - radius, y)
        drawLine(color = color, start = start, end = end, strokeWidth = Dimens.DummyRouteLineWidth.toPx())
        drawCircle(color = color, radius = radius, center = start)
        drawCircle(color = color, radius = radius, center = end)
    }
}

@Composable
private fun RowScope.DiagramLabel(text: String, align: TextAlign) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

/** 「다시 시도」 · 「더미 경로」 · 「닫기」 — 「여기로 안내」(E5)와 같은 크기 · 한 번 터치. */
@Composable
private fun FallbackButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    SecondaryButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = Dimens.HomeButtonHeight)
            .widthIn(min = Dimens.PlaceButtonMinWidth),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 테스트가 패널 위치를 재는 이름표. */
const val DUMMY_ROUTE_PANEL_TAG = "home_dummy_route_panel"
