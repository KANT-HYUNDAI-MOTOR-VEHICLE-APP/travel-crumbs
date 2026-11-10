package com.example.travelcrumbs.ui.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.ui.components.BackButton
import com.example.travelcrumbs.ui.components.DemoDataLine
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.RatingDialog
import com.example.travelcrumbs.ui.recommendations.components.GuidanceConfirmDialog
import com.example.travelcrumbs.ui.recommendations.components.RecommendationActions
import com.example.travelcrumbs.ui.recommendations.components.RecommendationMapCallbacks
import com.example.travelcrumbs.ui.recommendations.components.RecommendationPageContent
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 「추천」 칸 정차 화면(#37 · 와이어프레임 v6 B-01 → B-02 → B-03 → B-04 → B-05). 칸 안에서만 화면이 바뀜(레일 강조는 「추천」 그대로).
 * 시스템 뒤로 · 머리글 「뒤로」(B-02 ~ B-04 · 같은 onBack): 팝업 닫기 → 한 단계 앞 화면 · B-01 에선 이 칸이 받지 않아 앱 틀이 「기록」으로 보냄.
 * [entryKey] — 앱 틀이 레일 「추천」 · 「추천 보러 가기」를 누를 때마다 올리는 값. 바뀌면 B-01 부터(고른 것 초기화).
 * 주행 → 정차는 값이 그대로라 보던 화면이 복원됨. [openTripNo] 가 있으면(기록 칸 근처 로드맵 카드 · #55) 그 로드맵 B-04 부터 ·
 * 그 B-04 의 뒤로 · 머리글 「뒤로」는 [onLeave](앱 틀이 기록 칸으로).
 * 시드를 쓰는 동안 맨 위에 「시연용 데이터」 한 줄(모든 빌드 · 공모전 제출 APK 가 release).
 */
@Composable
fun RecommendationsScreen(
    entryKey: Int,
    modifier: Modifier = Modifier,
    openTripNo: Long? = null,
    openSidoNm: String? = null,
    onLeave: () -> Unit = {},
    viewModel: RecommendationsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(entryKey) { viewModel.onEntry(entryKey, openTripNo, openSidoNm) }
    // 기록 칸 카드로 연 B-04(#55)는 뒤로가 이 칸을 떠나 기록 칸으로(팝업이 떠 있으면 팝업부터 · N13)
    val onBack = { if (uiState.backLeavesTab) onLeave() else viewModel.onBack() }
    BackHandler(enabled = uiState.handlesBack) { onBack() }

    val actions = remember(viewModel) {
        RecommendationActions(
            onSelectSido = viewModel::selectSido,
            onSelectRegion = viewModel::selectRegion,
            onChangeRegion = viewModel::changeRegion,
            onSelectSort = viewModel::selectSort,
            onSelectCompanion = viewModel::selectCompanion,
            onOpenTags = viewModel::openTags,
            onSelectTag = viewModel::selectTag,
            onOpenTrip = viewModel::openTrip,
            onGuideFromFirst = viewModel::askGuidanceFromFirst,
            onGuideHere = viewModel::askGuidance,
            onOpenRating = viewModel::openRating,
            map = RecommendationMapCallbacks(
                onReady = viewModel::onMapReady,
                onFailed = viewModel::onMapFailed,
                onFirstFitDone = viewModel::onMapFirstFitDone,
                onMovedByUser = viewModel::onMapMovedByUser,
                onPins = viewModel::onMapPins,
                onSelectPlace = viewModel::selectMapPlace,
                onSelectCluster = viewModel::selectMapCluster,
            ),
            onSelectMapCandidate = viewModel::selectMapCandidate,
            onSelectCardRoadmap = viewModel::selectCardRoadmap,
            onOpenCardRoadmap = viewModel::openCardRoadmap,
            onCardGuidance = viewModel::askCardGuidance,
            onCloseMapCard = viewModel::closeMapCard,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        // 머리글 — B-02 · B-03 · B-04 는 맨 왼쪽 「뒤로」(시스템 뒤로와 같은 onBack · B-01 은 레일 첫 화면이라 없음)
        if (uiState.showsBackButton || uiState.demoData) {
            Row(
                modifier = Modifier.padding(top = Dimens.ItemSpacing),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.showsBackButton) BackButton(onBack = onBack)
                if (uiState.demoData) DemoDataLine()
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = Dimens.ItemSpacing),
        ) {
            RecommendationPageContent(uiState = uiState, actions = actions, entryKey = entryKey)
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    uiState.confirm?.let { confirm ->
        GuidanceConfirmDialog(
            item = confirm,
            requesting = uiState.guidanceRequesting,
            onCancel = viewModel::dismissGuidance,
            onStart = viewModel::startGuidance,
        )
    }

    uiState.rating?.let { rating ->
        RatingDialog(
            item = rating,
            onSelectScore = viewModel::selectRatingScore,
            onToggleTag = viewModel::toggleRatingTag,
            onLater = viewModel::dismissRating,
            onSubmit = viewModel::submitRating,
        )
    }
}
