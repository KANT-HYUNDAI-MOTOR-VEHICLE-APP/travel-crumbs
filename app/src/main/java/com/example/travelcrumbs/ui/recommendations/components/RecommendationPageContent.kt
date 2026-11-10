package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.ui.recommendations.RecommendationPage
import com.example.travelcrumbs.ui.recommendations.RecommendationsUiState

/** 「추천」 칸의 사용자 행동 — 화면 부품은 상태와 이 람다만 받음(ViewModel 을 모름). */
class RecommendationActions(
    val onSelectSido: (String) -> Unit,
    val onSelectRegion: (String?) -> Unit,
    val onChangeRegion: () -> Unit,
    val onSelectSort: (RankingSort) -> Unit,
    val onSelectCompanion: (CompanionType?) -> Unit,
    val onOpenTags: () -> Unit,
    val onSelectTag: (Long) -> Unit,
    val onOpenTrip: (Long) -> Unit,
    val onGuideFromFirst: () -> Unit,
    val onGuideHere: (Long) -> Unit,
    /** B-04 「별점 주기」 · 「내 별점 ★n · 고치기」(#41). */
    val onOpenRating: () -> Unit,
)

/** 지금 화면(B-01 ~ B-04) 하나를 그림. B-01 ~ B-03 은 세로 스크롤 · B-04 는 장소 목록만 스크롤. */
@Composable
fun RecommendationPageContent(
    uiState: RecommendationsUiState,
    actions: RecommendationActions,
) {
    val scrolling = Modifier.verticalScroll(rememberScrollState())
    val regionNm = uiState.regionSigunguNm
        ?.let { stringResource(R.string.reco_region_name, uiState.regionSidoNm, it) }
        ?: uiState.regionSidoNm
    when (uiState.page) {
        RecommendationPage.REGION -> RegionSelectContent(
            sidoNms = uiState.sidoNms,
            selectedSidoNm = uiState.selectedSidoNm,
            sigunguCards = uiState.sigunguCards,
            onSelectSido = actions.onSelectSido,
            onSelectRegion = actions.onSelectRegion,
            modifier = scrolling,
        )
        RecommendationPage.RANKING -> RankingContent(
            regionTitle = regionNm,
            sort = uiState.sort,
            companion = uiState.companion,
            trips = uiState.rankedTrips,
            onChangeRegion = actions.onChangeRegion,
            onSelectSort = actions.onSelectSort,
            onSelectCompanion = actions.onSelectCompanion,
            onOpenTags = actions.onOpenTags,
            onOpenTrip = actions.onOpenTrip,
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
            onSelectTag = actions.onSelectTag,
            onOpenTrip = actions.onOpenTrip,
            modifier = scrolling,
        )
        RecommendationPage.DETAIL -> uiState.detail?.let { detail ->
            TripDetailContent(
                detail = detail,
                onGuideFromFirst = actions.onGuideFromFirst,
                onGuideHere = actions.onGuideHere,
                onOpenRating = actions.onOpenRating,
            )
        }
    }
}
