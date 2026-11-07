package com.example.travelcrumbs.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.ui.home.components.HomeFeatureSections
import com.example.travelcrumbs.ui.home.components.SdkCheckCard
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 홈(정차 · 스펙 app-shell-v5 2-2) — 위에서 아래로
 * 1 맨 위 영역(여행 상태 · 경과 · 「이 장소 기록」 · 임시 「새 여행 시작」 · 오른쪽 위 「설정」) → 2 기능 구역 자리 → 3 debug SDK 확인 카드.
 * 맨 위 영역은 늘 첫 자리 · 그 아래만 스크롤(정차 전용). 「기록 일시정지」 · 「음성」 · 「여행 종료」는 두지 않음(#6 D10 · N12).
 * 「설정」 누름 = 앱 틀의 목적지를 설정으로 바꾸는 사용자 행동([onOpenSettings] · 이벤트 Flow 없음).
 *
 * [featureSections] · [showSdkCard] 는 테스트가 시험 구역 · 카드 표시를 고를 때만 바꿈.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    showSdkCard: Boolean = BuildConfig.DEBUG,
    featureSections: @Composable ColumnScope.() -> Unit = { HomeFeatureSections() },
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
                Row(verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
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
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_nav_settings), contentDescription = null)
                        Spacer(modifier = Modifier.width(Dimens.ItemSpacing))
                        Text(
                            text = stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    HomeTopButton(
                        labelRes = R.string.action_record_place,
                        enabled = uiState.recordPlaceEnabled,
                        onClick = viewModel::onRecordPlace,
                    )
                    HomeTopButton(
                        labelRes = R.string.action_start_new_trip,
                        enabled = uiState.startNewTripEnabled,
                        onClick = viewModel::onStartNewTrip,
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
}

@Composable
private fun HomeTopButton(
    @StringRes labelRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(Dimens.HomeButtonHeight),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** 맨 위 영역 — 테스트가 위치를 재는 이름표. */
const val TOP_AREA_TAG = "home_top_area"
