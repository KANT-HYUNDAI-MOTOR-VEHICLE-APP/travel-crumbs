package com.example.travelcrumbs.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.components.TripStatusCard
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.recordingStatusText
import com.example.travelcrumbs.ui.home.components.GuideConnectionContent
import com.example.travelcrumbs.ui.home.components.GuideScreenActions
import com.example.travelcrumbs.ui.home.components.HomeFeatureSections
import com.example.travelcrumbs.ui.home.components.PlaceActions
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 홈(정차 · 스펙 app-shell 2-2) — 위에서 아래로
 * 1 맨 위 영역(기록 상태 · 경과 · 「이 장소 기록」) → 2 기능 구역 자리 → 3 debug SDK 확인 카드.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「기록 일시정지」 · 「음성 입력」은 두지 않음(D10).
 * 길안내 연결 화면(#4 navigation 2.3)이 열려 있으면 위 셋 대신 그 화면을 그림(U64 (a) · U66).
 *
 * [featureSections] · [showSdkCard] 는 테스트가 시험 구역 · 카드 표시를 고를 때만 바꿈 —
 * [featureSections] 가 null 이면 실제 기능 구역(HomeFeatureSections · #4 「가 볼 장소」 · 「장소 고르기」).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    showSdkCard: Boolean = BuildConfig.DEBUG,
    featureSections: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 내비에서 돌아옴 — 놓친 길안내 시작 알림을 내비 상태로 대조(#4 R1)
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    val guidePlace = uiState.guideScreen
    Column(modifier = modifier.fillMaxSize()) {
        if (guidePlace != null) {
            // 길안내 연결 화면(navigation 2.3) — 「홈」 목적지 안 내용 영역을 통째로 바꿈 · 레일 · 앱 틀은 #6 그대로(U64 (a)).
            // 열려 있을 때만 시스템 뒤로가 이 화면을 먼저 닫음 — 닫혀 있으면 #6 규칙(앱을 뒤로) 그대로(T122 · T123).
            BackHandler(onBack = viewModel::onCloseGuideScreen)
            val guideActions = remember(viewModel) {
                GuideScreenActions(
                    onRetry = viewModel::onRetry,
                    onShowDummyRoute = viewModel::onShowDummyRoute,
                    onClose = viewModel::onCloseGuideScreen,
                )
            }
            GuideConnectionContent(
                place = guidePlace,
                state = uiState,
                actions = guideActions,
                modifier = Modifier.weight(1f),
            )
        } else Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            Column(
                modifier = Modifier.testTag(TOP_AREA_TAG),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                TripStatusCard(
                    statusText = recordingStatusText(uiState.recording),
                    active = uiState.recording == RecordingStatus.RECORDING,
                    elapsedText = elapsedText(uiState.elapsedMinutes),
                )
                TcButton(
                    text = stringResource(R.string.action_record_place),
                    onClick = viewModel::onRecordPlace,
                    kind = TcButtonKind.SECONDARY,
                    iconRes = R.drawable.ic_place,
                )
            }

            if (featureSections != null) {
                featureSections()
            } else {
                val placeActions = remember(viewModel) {
                    PlaceActions(
                        onGuide = viewModel::onGuide,
                        onPin = viewModel::onPin,
                        onUnpin = viewModel::onUnpin,
                    )
                }
                HomeFeatureSections(state = uiState, placeActions = placeActions)
            }

            if (showSdkCard) {
                SdkCheckCard(
                    drivingState = uiState.drivingState,
                    naviReady = uiState.naviReady,
                    navi = uiState.navi,
                    openedByGleoAction = uiState.openedByGleoAction,
                    onRefreshNavi = viewModel::refreshNavi,
                    placeStats = uiState.placeStats,
                    onRefreshPlaceStats = viewModel::refreshPlaceStats,
                )
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}

/** 맨 위 영역 — 테스트가 위치를 재는 이름표. */
const val TOP_AREA_TAG = "home_top_area"
