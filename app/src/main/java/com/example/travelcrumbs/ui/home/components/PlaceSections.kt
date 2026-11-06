package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.ui.home.CandidateItem
import com.example.travelcrumbs.ui.home.HomeUiState
import com.example.travelcrumbs.ui.home.PlaceItem
import com.example.travelcrumbs.ui.home.RouteRequestState
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 「가 볼 장소」(스펙 navigation 2.1 · E1 ~ E6) — 정차 홈에서만 · 글 입력 · 애니메이션 · 회전 표시 없음.
 * 요청 중도 글자로만 알림.
 */
@Composable
fun PlacesToVisitSection(
    state: HomeUiState,
    onGuide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PLACES_SECTION_TAG),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        SectionTitle(stringResource(R.string.places_title))
        if (!state.naviReady) {
            BodyLine(stringResource(R.string.places_navi_not_connected))
        }
        routeResultText(state.routeRequest)?.let { BodyLine(it) }
        // 읽기 전(null)엔 E4 · E6 둘 다 그리지 않음 — 빈 안내가 깜빡이지 않게
        val pinned = state.pinnedPlaces
        if (pinned != null && pinned.isEmpty()) {
            BodyLine(stringResource(R.string.places_empty))
        }
        pinned.orEmpty().forEach { place ->
            PlaceRow(name = place.name, address = place.address) {
                GuideButton(place = place, state = state, onGuide = onGuide)
            }
        }
    }
}

/** 「장소 고르기」(스펙 navigation 2.2 · E7 ~ E10) — 고정 · 해제 · 한 번 터치 · 확인 대화상자 없음. */
@Composable
fun PlacePickerSection(
    state: HomeUiState,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PICKER_SECTION_TAG),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        SectionTitle(stringResource(R.string.picker_title))
        if (!state.canPinMore) {
            BodyLine(stringResource(R.string.picker_limit, AppConfig.PINNED_PLACES_MAX))
        }
        state.candidates.forEach { candidate ->
            PlaceRow(name = candidate.name, address = candidate.address) {
                PinButton(candidate = candidate, canPinMore = state.canPinMore, onPin = onPin, onUnpin = onUnpin)
            }
        }
    }
}

@Composable
private fun GuideButton(place: PlaceItem, state: HomeUiState, onGuide: (String) -> Unit) {
    val requesting = (state.routeRequest as? RouteRequestState.Requesting)?.placeId == place.id
    Button(
        onClick = { onGuide(place.id) },
        enabled = state.guideEnabled,
        modifier = Modifier
            .heightIn(min = Dimens.HomeButtonHeight)
            .widthIn(min = Dimens.PlaceButtonMinWidth),
    ) {
        Text(
            text = stringResource(if (requesting) R.string.places_guide_requesting else R.string.places_guide),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun PinButton(
    candidate: CandidateItem,
    canPinMore: Boolean,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
) {
    OutlinedButton(
        onClick = { if (candidate.pinned) onUnpin(candidate.id) else onPin(candidate.id) },
        enabled = candidate.pinned || canPinMore,
        modifier = Modifier
            .heightIn(min = Dimens.HomeButtonHeight)
            .widthIn(min = Dimens.PlaceButtonMinWidth),
    ) {
        Text(
            text = stringResource(if (candidate.pinned) R.string.picker_unpin else R.string.picker_pin),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
    }
}

/** 장소 이름 한 줄 · 주소 한 줄(넘치면 말줄임) · 오른쪽 버튼 — 글이 길어도 버튼 위치 · 크기 그대로(T25). */
@Composable
private fun PlaceRow(
    name: String,
    address: String,
    button: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        button()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
}

@Composable
private fun BodyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** E3 결과 문구 — 대기 · 요청 중엔 없음(요청 중엔 직전 결과를 지움). */
@Composable
private fun routeResultText(request: RouteRequestState): String? = when (request) {
    RouteRequestState.Idle, is RouteRequestState.Requesting -> null
    is RouteRequestState.Started -> stringResource(R.string.places_route_started, request.placeName)
    is RouteRequestState.Failed -> stringResource(R.string.places_route_failed, stringResource(request.reason.textRes()))
}

private fun RouteFailReason.textRes(): Int = when (this) {
    RouteFailReason.NOT_CONNECTED -> R.string.places_navi_not_connected
    RouteFailReason.TOO_CLOSE -> R.string.places_reason_too_close
    RouteFailReason.TOO_FAR -> R.string.places_reason_too_far
    RouteFailReason.LOW_BATTERY -> R.string.places_reason_low_battery
    RouteFailReason.ROUTE_NOT_FOUND -> R.string.places_reason_route_not_found
    RouteFailReason.TIMEOUT -> R.string.places_reason_timeout
    RouteFailReason.REQUEST_FAILED -> R.string.places_reason_request_failed
}

/** 테스트가 구역 위치를 재는 이름표. */
const val PLACES_SECTION_TAG = "home_places_section"
const val PICKER_SECTION_TAG = "home_picker_section"
