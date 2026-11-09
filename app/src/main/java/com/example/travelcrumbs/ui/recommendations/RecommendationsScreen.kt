package com.example.travelcrumbs.ui.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.recommendations.components.DemoDataLine
import com.example.travelcrumbs.ui.recommendations.components.GuidanceConfirmDialog
import com.example.travelcrumbs.ui.recommendations.components.RankingContent
import com.example.travelcrumbs.ui.recommendations.components.RegionSelectContent
import com.example.travelcrumbs.ui.recommendations.components.TagsContent
import com.example.travelcrumbs.ui.recommendations.components.TripDetailContent
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 「추천」 칸 정차 화면(#37 · 와이어프레임 v6 B-01 → B-02 → B-03 → B-04 → B-05). 칸 안에서만 화면이 바뀜(레일 강조는 「추천」 그대로).
 * 시스템 뒤로: 팝업 닫기 → 한 단계 앞 화면 · B-01 에선 이 칸이 받지 않아 앱 틀이 「기록」으로 보냄.
 * 주행 중에는 앱 틀이 이 화면 대신 「정차 후 볼 수 있어요」를 그림(#21) — 고른 것은 ViewModel 에 남아 정차 뒤 그대로.
 *
 * [showDemoDataLine] 은 테스트가 debug 표시를 고를 때만 바꿈.
 */
@Composable
fun RecommendationsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecommendationsViewModel = viewModel(),
    showDemoDataLine: Boolean = BuildConfig.DEBUG,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.handlesBack) { viewModel.onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        if (showDemoDataLine && uiState.demoData) {
            DemoDataLine(modifier = Modifier.padding(top = Dimens.ItemSpacing))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = Dimens.ItemSpacing),
        ) {
            PageContent(uiState = uiState, viewModel = viewModel)
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
}

@Composable
private fun PageContent(uiState: RecommendationsUiState, viewModel: RecommendationsViewModel) {
    val scrolling = Modifier.verticalScroll(rememberScrollState())
    val regionNm = uiState.regionSigunguNm
        ?.let { stringResource(R.string.reco_region_name, uiState.regionSidoNm, it) }
        ?: uiState.regionSidoNm
    when (uiState.page) {
        RecommendationPage.REGION -> RegionSelectContent(
            sidoNms = uiState.sidoNms,
            selectedSidoNm = uiState.selectedSidoNm,
            sigunguCards = uiState.sigunguCards,
            onSelectSido = viewModel::selectSido,
            onSelectRegion = viewModel::selectRegion,
            modifier = scrolling,
        )
        RecommendationPage.RANKING -> RankingContent(
            regionTitle = regionNm,
            sort = uiState.sort,
            companion = uiState.companion,
            trips = uiState.rankedTrips,
            onChangeRegion = viewModel::changeRegion,
            onSelectSort = viewModel::selectSort,
            onSelectCompanion = viewModel::selectCompanion,
            onOpenTags = viewModel::openTags,
            onOpenTrip = viewModel::openTrip,
            modifier = scrolling,
        )
        RecommendationPage.TAGS -> TagsContent(
            regionNm = regionNm,
            regionTripCnt = uiState.regionTripCnt,
            tagCounts = uiState.tagCounts,
            maxTagCnt = uiState.maxTagCnt,
            selectedTagNo = uiState.selectedTagNo,
            selectedTagNm = uiState.selectedTagNm,
            taggedTrips = uiState.taggedTrips,
            onSelectTag = viewModel::selectTag,
            onOpenTrip = viewModel::openTrip,
            modifier = scrolling,
        )
        RecommendationPage.DETAIL -> uiState.detail?.let { detail ->
            TripDetailContent(
                detail = detail,
                onGuideFromFirst = viewModel::askGuidanceFromFirst,
                onGuideHere = viewModel::askGuidance,
            )
        }
    }
}
