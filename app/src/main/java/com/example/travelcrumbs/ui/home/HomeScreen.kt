package com.example.travelcrumbs.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.recordingStatusText
import com.example.travelcrumbs.ui.home.components.HomeFeatureSections
import com.example.travelcrumbs.ui.home.components.LocationConsentDialog
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.placeinput.PlaceInputScreen
import com.example.travelcrumbs.ui.tripdetail.TripDetailScreen
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.tripsetup.TripSetupScreen

/**
 * 홈(정차 · 스펙 app-shell 2-2) — 위에서 아래로
 * 1 맨 위 영역(기록 상태 · 경과 · 「이 장소 기록」) → 2 기능 구역 자리 → 3 debug SDK 확인 카드.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「기록 일시정지」 · 「음성 입력」은 두지 않음(D10).
 *
 * [featureSections] · [showSdkCard] 는 테스트가 시험 구역 · 카드 표시를 고를 때만 바꿈.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    showSdkCard: Boolean = BuildConfig.DEBUG,
    featureSections: @Composable ColumnScope.() -> Unit = {
        HomeFeatureSections(onStartNewTrip = viewModel::onStartNewTrip)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
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
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                } else if (uiState.userUnavailable) {
                    Text(
                        text = stringResource(R.string.home_user_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                    )
                }
                // 기록 중인 여행(A-05) — trip_mst.trip_nm · place_cnt
                val tripName = uiState.activeTripName
                if (tripName != null) {
                    Text(
                        text = stringResource(R.string.home_active_trip, tripName, uiState.placeCount),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
                // 여행 종료(A-08)
                if (tripName != null) {
                    OutlinedButton(
                        onClick = viewModel::onEndTrip,
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    ) {
                        Text(text = stringResource(R.string.home_end_trip), style = MaterialTheme.typography.titleLarge)
                    }
                }
                // 입력 안 한 임시 기록(A-06) — 정차 중에만 보이는 홈에서 입력
                if (uiState.draftCount > 0) {
                    OutlinedButton(
                        onClick = viewModel::onOpenPlaceInput,
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    ) {
                        Text(
                            text = stringResource(R.string.home_place_input, uiState.draftCount),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Text(
                    text = recordingStatusText(uiState.recording),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = elapsedText(uiState.elapsedMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Button(
                    onClick = viewModel::onRecordPlace,
                    modifier = Modifier.height(Dimens.HomeButtonHeight),
                ) {
                    Text(
                        text = stringResource(R.string.action_record_place),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            featureSections()

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
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    if (uiState.showEndConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onContinueRecording,
            text = { Text(text = stringResource(R.string.end_trip_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = viewModel::onConfirmEndTrip) { Text(text = stringResource(R.string.end_trip_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onContinueRecording) {
                    Text(text = stringResource(R.string.end_trip_continue))
                }
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
