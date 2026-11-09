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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.ui.home.components.HomeFeatureSections
import com.example.travelcrumbs.ui.home.components.PlaceActions
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 기록 칸 정차 화면(스펙 app-shell-v5 2-2 · D34) — 위에서 아래로 1 맨 위 영역 → 2 기능 구역 자리 → 3 debug SDK 확인 카드(#6 D17).
 * 맨 위 영역은 진행 중 여행 여부로 둘 중 하나 —
 * 아님: 「여행 시작」 · 「최근 여행이 없어요」 · 「추천 보러 가기」 / 진행 중: 기록 상태 · 경과 · 「기록된 장소 n」 · 「이 장소 기록」.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「설정」(D35) · 「기록 일시정지」 · 「음성」 · 「여행 종료」는 두지 않음(#6 D10 · N12).
 * 「추천 보러 가기」 누름 = 앱 틀의 목적지를 「추천」으로 바꾸는 사용자 행동([onOpenRecommendations] · 이벤트 Flow 없음).
 *
 * [featureSections] · [showSdkCard] 는 테스트가 시험 구역 · 카드 표시를 고를 때만 바꿈 —
 * [featureSections] 가 null 이면 실제 기능 구역(HomeFeatureSections · #4 「가 볼 장소」 · 「장소 고르기」).
 */
@Composable
fun HomeScreen(
    onOpenRecommendations: () -> Unit,
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
                if (uiState.tripInProgress) {
                    TripInProgressTop(uiState = uiState, onRecordPlace = viewModel::onRecordPlace)
                } else {
                    NoTripTop(onStartTrip = viewModel::onStartTrip, onOpenRecommendations = onOpenRecommendations)
                }
            }

            if (featureSections != null) {
                featureSections()
            } else {
                val placeActions = remember(viewModel) {
                    PlaceActions(
                        onGuide = viewModel::onGuide,
                        onPin = viewModel::onPin,
                        onUnpin = viewModel::onUnpin,
                        onRetry = viewModel::onRetry,
                        onShowDummyRoute = viewModel::onShowDummyRoute,
                        onCloseDummyRoute = viewModel::onCloseDummyRoute,
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

/** 진행 중 여행(RECORDING · PAUSED) — 기록 상태 · 경과 · 장소 수 · 「이 장소 기록」. */
@Composable
private fun TripInProgressTop(
    uiState: HomeUiState,
    onRecordPlace: () -> Unit,
) {
    Text(
        text = tripStatusText(uiState.tripStatus),
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
    Text(
        text = stringResource(R.string.trip_place_count, uiState.placeCount),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
    PrimaryButton(onClick = onRecordPlace, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
        Text(text = stringResource(R.string.action_record_place), style = MaterialTheme.typography.labelLarge)
    }
}

/** 진행 중 여행 아님 — 주요 행동 「여행 시작」 하나 · 최근 여행 자리(늘 빈 상태 · N26) · 「추천 보러 가기」(흐름 ② 입구). */
@Composable
private fun NoTripTop(
    onStartTrip: () -> Unit,
    onOpenRecommendations: () -> Unit,
) {
    PrimaryButton(onClick = onStartTrip, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
        Text(text = stringResource(R.string.action_start_trip), style = MaterialTheme.typography.labelLarge)
    }
    Text(
        text = stringResource(R.string.recent_trip_empty),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
    SecondaryButton(onClick = onOpenRecommendations, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
        Text(text = stringResource(R.string.action_open_recommendations), style = MaterialTheme.typography.labelLarge)
    }
}

/** 맨 위 영역 — 테스트가 위치를 재는 이름표. */
const val TOP_AREA_TAG = "home_top_area"
