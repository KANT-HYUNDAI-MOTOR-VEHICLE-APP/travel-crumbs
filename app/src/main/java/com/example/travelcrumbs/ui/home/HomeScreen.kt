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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 홈(정차 · 스펙 app-shell 2-2) — 위에서 아래로
 * 1 맨 위 영역(기록 상태 · 경과 · 「이 장소 기록」) → 2 기능 구역 자리 → 3 debug SDK 확인 카드.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「기록 일시정지」 · 「음성 입력」은 두지 않음(D10).
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

            if (featureSections != null) {
                featureSections()
            } else {
                HomeFeatureSections(
                    state = uiState,
                    onGuide = viewModel::onGuide,
                    onPin = viewModel::onPin,
                    onUnpin = viewModel::onUnpin,
                )
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
