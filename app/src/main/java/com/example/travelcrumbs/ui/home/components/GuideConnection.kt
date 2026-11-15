package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.components.StatusMessageBar
import com.example.travelcrumbs.ui.components.StatusMessageKind
import com.example.travelcrumbs.ui.home.HomeUiState
import com.example.travelcrumbs.ui.home.PlaceItem
import com.example.travelcrumbs.ui.home.RouteRequestState
import com.example.travelcrumbs.ui.theme.Dimens

/** 길안내 연결 화면의 사용자 행동 — 모두 ViewModel 메서드(navigation 3.1). */
data class GuideScreenActions(
    val onRetry: () -> Unit,
    val onShowDummyRoute: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * 길안내 연결 화면(스펙 navigation 2.3 · 와이어프레임 A-14) — 「홈」 목적지 안 내용 영역을 통째로 바꿈(U64 (a) · #6 파일 그대로).
 * 위 → 아래: G6 「닫기」 줄 · G1 지도 박스(남은 높이 · 정지 도식) · G2 상태 줄 · G3 실패 박스(실패일 때만).
 * 화면 안 스크롤 · 애니메이션 · 지도 타일 · 글 입력 없음. 「현재 위치」 · 「목적지」는 글자뿐 — 위치를 읽지 않음(U53).
 * 더미 경로 버튼 · 시연용 표시 · 실패 박스 글자는 debug 소스 세트의 DemoGuideParts(release 엔 빈 함수 · U55 · U70).
 */
@Composable
fun GuideConnectionContent(
    place: PlaceItem,
    state: HomeUiState,
    actions: GuideScreenActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd, top = Dimens.ScreenTop)
            .testTag(GUIDE_SCREEN_TAG),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            GuideActionButton(text = stringResource(R.string.guide_close), enabled = true, onClick = actions.onClose)
        }
        GuideMapBox(
            place = place,
            dummyRoute = state.dummyRoute != null,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        GuideStatusLine(request = state.routeRequest)
        if (state.retryVisible) {
            GuideFailureBox(state = state, actions = actions)
        }
    }
}

/**
 * G1 지도 박스 — 큰 둥근 테두리 상자 · 가운데 장소 이름(크게) · 주소 한 줄 · 오른쪽 위 「목적지」 점 · 왼쪽 아래 「현재 위치」 점.
 * 두 점을 잇는 선과 시연용 표시는 더미 경로 모드(G1-D · [dummyRoute])에서만 — release 에선 늘 거짓.
 */
@Composable
private fun GuideMapBox(place: PlaceItem, dummyRoute: Boolean, modifier: Modifier = Modifier) {
    // 지도 · 경로(디자인 시스템 17절) — 목적지 Brand/Primary · 현재 위치와 이동 경로 Brand/Secondary(Oat Beige)
    val destinationColor = MaterialTheme.colorScheme.primary
    val routeColor = MaterialTheme.colorScheme.secondary
    OutlinedCard(modifier = modifier.testTag(GUIDE_MAP_TAG)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = Dimens.GuideMapDotRadius.toPx()
                val inset = Dimens.GuideMapDotInset.toPx()
                val destination = Offset(size.width - inset, inset)
                val origin = Offset(inset, size.height - inset)
                if (dummyRoute) {
                    drawLine(routeColor, origin, destination, strokeWidth = Dimens.GuideMapLineWidth.toPx())
                }
                drawCircle(color = destinationColor, radius = radius, center = destination)
                drawCircle(color = routeColor, radius = radius, center = origin)
            }
            if (dummyRoute) {
                DummyRouteNotice(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Dimens.ItemSpacing),
                )
            }
            MapLabel(
                text = stringResource(R.string.guide_destination),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Dimens.GuideMapLabelOffset, end = Dimens.ItemSpacing),
            )
            MapLabel(
                text = stringResource(R.string.guide_origin),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Dimens.ItemSpacing, bottom = Dimens.GuideMapLabelOffset),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Dimens.GuideMapTextPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MapLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier,
    )
}

/** G2 상태 줄 — 요청 중 · 전달 완료 · 실패 문구(문자열은 홈 E3 · E5 와 같은 것). 대기로는 이 화면이 열리지 않음. */
@Composable
private fun GuideStatusLine(request: RouteRequestState) {
    val text = when (request) {
        RouteRequestState.Idle -> return
        is RouteRequestState.Requesting -> stringResource(R.string.places_guide_requesting)
        is RouteRequestState.Started -> stringResource(R.string.guide_route_started)
        is RouteRequestState.Failed -> stringResource(R.string.places_route_failed, stringResource(request.reason.textRes()))
    }
    StatusMessageBar(
        description = text,
        kind = when (request) {
            is RouteRequestState.Requesting -> StatusMessageKind.Notice
            is RouteRequestState.Started -> StatusMessageKind.SuccessToast
            is RouteRequestState.Failed -> StatusMessageKind.FailureToast
            RouteRequestState.Idle -> return
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** G3 실패 박스 — 왼쪽 글자(debug 만) · 미연결 한 줄 · 오른쪽 G4 「다시 시도」 · G5 「더미 경로」(debug 만). */
@Composable
private fun GuideFailureBox(state: HomeUiState, actions: GuideScreenActions) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().testTag(GUIDE_FAILURE_TAG)) {
        Row(
            modifier = Modifier.padding(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (state.dummyRouteAvailable) FallbackLabel()
                if (!state.naviReady) {
                    Text(
                        text = stringResource(R.string.places_navi_not_connected),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            GuideActionButton(
                text = stringResource(R.string.guide_retry),
                enabled = state.retryEnabled,
                onClick = actions.onRetry,
                primary = true,
            )
            if (state.dummyRouteVisible) {
                DummyRouteButton(enabled = state.dummyRouteEnabled, onClick = actions.onShowDummyRoute)
            }
        }
    }
}

/** 「다시 시도」 · 「더미 경로」 · 「닫기」 — 「여기로 안내」(E5)와 같은 크기 · 한 번 터치. */
@Composable
internal fun GuideActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val modifier = Modifier
        .heightIn(min = Dimens.HomeButtonHeight)
        .widthIn(min = Dimens.PlaceButtonMinWidth)
    val label: @Composable RowScope.() -> Unit = {
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    // 「다시 시도」만 Primary(실패 박스의 주요 행동 하나 · 디자인 시스템 12절) · 나머지는 Secondary
    if (primary) {
        PrimaryButton(onClick = onClick, enabled = enabled, modifier = modifier, content = label)
    } else {
        SecondaryButton(onClick = onClick, enabled = enabled, modifier = modifier, content = label)
    }
}

/** 테스트가 화면 · 박스 위치를 재는 이름표. */
const val GUIDE_SCREEN_TAG = "home_guide_screen"
const val GUIDE_MAP_TAG = "home_guide_map"
const val GUIDE_FAILURE_TAG = "home_guide_failure"
