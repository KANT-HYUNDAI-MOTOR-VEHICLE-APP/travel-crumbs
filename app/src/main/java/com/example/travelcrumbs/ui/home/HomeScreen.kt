package com.example.travelcrumbs.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.ui.components.CrumbRoute
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.components.TripStatusCard
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.recordingStatusText
import com.example.travelcrumbs.ui.home.components.HomeFeatureSections
import com.example.travelcrumbs.ui.home.components.LocationConsentDialog
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.placeinput.PlaceInputScreen
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.tripdetail.TripDetailScreen
import com.example.travelcrumbs.ui.tripsetup.TripSetupScreen

/**
 * 홈(정차 · 스펙 app-shell 2-2) — 위에서 아래로
 * 1 맨 위 영역(기록 상태 · 경과 · 「이 장소 기록」) → 2 기능 구역 자리 → 3 debug SDK 확인 카드.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「기록 일시정지」 · 「음성 입력」은 두지 않음(D10).
 * 배치는 디자인 시스템 지시서 8장 — 왼쪽 경로 그림(지도 자리) · 오른쪽 정보 영역(위 순서 그대로).
 *
 * [featureSections] · [showSdkCard] 는 테스트가 시험 구역 · 카드 표시를 고를 때만 바꿈(null 이면 기본 구역).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    showSdkCard: Boolean = BuildConfig.DEBUG,
    featureSections: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 홈이 다시 보일 때 경로 그림 · 최근 여행을 새로 읽음(기록 중이면 그새 늘어난 경로)
    LaunchedEffect(Unit) { viewModel.refreshRoute() }

    // 여행 상세(A-08) — 종료 직후 홈 자리를 그대로 씀
    val detailUuid = uiState.tripDetailUuid
    if (detailUuid != null) {
        TripDetailScreen(tripUuid = detailUuid, onClose = viewModel::onTripDetailClosed, modifier = modifier)
        return
    }

    // 장소 경험 입력(A-06) — 홈 자리를 그대로 씀
    if (uiState.showPlaceInput) {
        PlaceInputScreen(onClose = viewModel::onPlaceInputClosed, modifier = modifier)
        return
    }

    // 여행 시작 설정(A-04) — 홈 자리를 그대로 씀(레일 항목이 아니라 홈에서 들어가는 단계)
    if (uiState.showTripSetup) {
        TripSetupScreen(
            onCancel = viewModel::onTripSetupCancel,
            onStarted = viewModel::onTripStarted,
            modifier = modifier,
        )
        return
    }

    // 레일 오른쪽 = 지도 자리 43 · 정보 45(디자인 시스템 지시서 8장)
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            // 경로 그림 — 기록 중이면 지금 여행, 아니면 최근 여행(지도는 나중에)
            CrumbRoute(
                route = uiState.route,
                emptyText = stringResource(R.string.home_route_empty),
                caption = when {
                    uiState.route == null -> null
                    uiState.routeIsActive -> stringResource(R.string.home_route_active)
                    else -> uiState.routeTripName?.let { stringResource(R.string.home_route_recent, it) }
                },
                modifier = Modifier
                    .weight(Dimens.MapWeight)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(Dimens.InfoWeight)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                Column(
                    modifier = Modifier.testTag(TOP_AREA_TAG),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    // 인사말(A-03) — user_mst.nickname_nm · 서버 GET /me
                    val nickname = uiState.nickname
                    if (nickname != null) {
                        Text(
                            text = stringResource(R.string.home_greeting, nickname),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                        )
                    } else if (uiState.userUnavailable) {
                        Text(
                            text = stringResource(R.string.home_user_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                        )
                    }
                    // 여행 상태 카드(지시서 14장) — 기록 중인 여행(A-05) trip_mst.trip_nm · place_cnt
                    val tripName = uiState.activeTripName
                    TripStatusCard(
                        statusText = recordingStatusText(uiState.recording),
                        active = tripName != null && uiState.recording == RecordingStatus.RECORDING,
                        elapsedText = elapsedText(uiState.elapsedMinutes),
                        tripName = tripName?.let { stringResource(R.string.home_active_trip, it, uiState.placeCount) },
                    )
                    // 이 장소 기록 — 기록 중이면 이 화면의 주요 행동(Primary)
                    TcButton(
                        text = stringResource(R.string.action_record_place),
                        onClick = viewModel::onRecordPlace,
                        kind = if (tripName != null) TcButtonKind.PRIMARY else TcButtonKind.SECONDARY,
                        iconRes = R.drawable.ic_place,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (tripName != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall)) {
                            // 입력 안 한 임시 기록(A-06) — 정차 중에만 보이는 홈에서 입력
                            if (uiState.draftCount > 0) {
                                TcButton(
                                    text = stringResource(R.string.home_place_input, uiState.draftCount),
                                    onClick = viewModel::onOpenPlaceInput,
                                    iconRes = R.drawable.ic_edit,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // 여행 종료(A-08) — 확인 창을 거침
                            TcButton(
                                text = stringResource(R.string.home_end_trip),
                                onClick = viewModel::onEndTrip,
                                kind = TcButtonKind.DANGER,
                                iconRes = R.drawable.ic_stop,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (featureSections != null) {
                    featureSections(this)
                } else {
                    HomeFeatureSections(
                        onStartNewTrip = viewModel::onStartNewTrip,
                        showStartTrip = uiState.activeTripName == null,
                        recentTrips = uiState.recentTrips,
                        onOpenTrip = viewModel::onOpenTrip,
                    )
                }

                if (showSdkCard) {
                    SdkCheckCard(
                        drivingState = uiState.drivingState,
                        naviReady = uiState.naviReady,
                        navi = uiState.navi,
                        openedByGleoAction = uiState.openedByGleoAction,
                        onRefreshNavi = viewModel::refreshNavi,
                    )
                }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    if (uiState.showEndConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onContinueRecording,
            text = { Text(text = stringResource(R.string.end_trip_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TcButton(
                    text = stringResource(R.string.end_trip_confirm),
                    onClick = viewModel::onConfirmEndTrip,
                    kind = TcButtonKind.DANGER,
                )
            },
            dismissButton = {
                TcButton(text = stringResource(R.string.end_trip_continue), onClick = viewModel::onContinueRecording)
            },
        )
    }

    if (uiState.showLocationConsent) {
        LocationConsentDialog(
            onAgree = viewModel::onAgreeLocationConsent,
            onDecline = viewModel::onDeclineLocationConsent,
        )
    }
}

/** 맨 위 영역 — 테스트가 위치를 재는 이름표. */
const val TOP_AREA_TAG = "home_top_area"
