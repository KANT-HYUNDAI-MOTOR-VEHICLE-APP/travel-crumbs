package com.example.travelcrumbs.ui.recommendations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 「추천」 칸(#37 · 와이어프레임 v6 B-01 ~ B-05) — 목적지 안의 화면 이동 · 필터 · 태그 · 안내 요청.
 * 고른 것(화면 · 지역 · 정렬 · 동행자 · 태그 · 로드맵 · 팝업)은 SavedStateHandle 에 둬 프로세스 재생성 · 주행 뒤에도 그대로.
 * 계산(정렬 · 개수 · 대표 태그 · 경로 그림)은 도메인 [RecommendationRules] · 자료는 저장소 하나.
 */
@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val repository: RecommendationRepository,
    private val requestRouteGuidance: RequestRouteGuidanceUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selection = MutableStateFlow(restoreSelection())
    private val guidance = MutableStateFlow(GuidanceProgress())

    private val sidoRegions = MutableStateFlow<List<SidoRegion>>(emptyList())
    private val sidoTrips = MutableStateFlow(SidoTrips(sidoNm = null, trips = emptyList()))

    val uiState: StateFlow<RecommendationsUiState> = combine(
        selection,
        sidoRegions,
        sidoTrips,
        guidance,
    ) { sel, regions, loaded, progress ->
        // 시도를 바꾼 직후 옛 시도 로드맵이 한 번 섞이지 않게 — 불러온 시도가 지금 시도일 때만 씀
        val trips = if (loaded.sidoNm == sel.sidoNm) loaded.trips else emptyList()
        buildState(sel, regions, trips, progress)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = RecommendationsUiState(
            demoData = repository.isDemoData,
            guidanceAvailable = requestRouteGuidance.available,
        ),
    )

    init {
        // 자료는 화면 구독과 따로 불러 둠 — 행동(첫 장소 · 안내 목적지)이 화면이 안 보일 때도 같은 값을 보게
        viewModelScope.launch { sidoRegions.value = repository.sidoRegions() }
        viewModelScope.launch {
            selection.map { it.sidoNm }.distinctUntilChanged().collect { sido ->
                sidoTrips.value = SidoTrips(sido, if (sido == null) emptyList() else repository.publicTrips(sido))
            }
        }
    }

    /**
     * 앱 틀의 진입 값(레일 「추천」 · 「추천 보러 가기」마다 오름)이 지난번과 다르면 B-01 부터 · 고른 것 초기화.
     * 같으면(주행 → 정차 · 화면 재구성 · 프로세스 재생성) 보던 화면 그대로.
     */
    fun onEntry(entryKey: Int) {
        if (savedStateHandle.get<Int>(KEY_ENTRY) == entryKey) return
        savedStateHandle[KEY_ENTRY] = entryKey
        updateSelection { Selection.START }
        guidance.value = GuidanceProgress()
    }

    /** B-01 시도 칩. */
    fun selectSido(sidoNm: String) = updateSelection { it.copy(sidoNm = sidoNm) }

    /** B-01 시군구 카드(null = 「전체」) → B-02. 동행자 필터 · 태그는 새 지역에서 처음부터. */
    fun selectRegion(sigunguNm: String?) = updateSelection {
        it.copy(page = RecommendationPage.RANKING, sigunguNm = sigunguNm, companion = null, tagNo = null)
    }

    /** B-02 「지역 바꾸기」 → B-01. */
    fun changeRegion() = updateSelection { it.copy(page = RecommendationPage.REGION) }

    fun selectSort(sort: RankingSort) = updateSelection { it.copy(sort = sort) }

    /** B-02 동행자 칩(null = 「전체」). */
    fun selectCompanion(companion: CompanionType?) = updateSelection { it.copy(companion = companion) }

    /** B-02 「# 태그별로 보기」 → B-03(처음엔 가장 많이 붙은 태그). */
    fun openTags() = updateSelection { it.copy(page = RecommendationPage.TAGS, tagNo = null) }

    fun selectTag(tagNo: Long) = updateSelection { it.copy(tagNo = tagNo) }

    /** B-02 · B-03 로드맵 카드 → B-04. 뒤로는 온 화면으로. */
    fun openTrip(tripNo: Long) = updateSelection {
        it.copy(page = RecommendationPage.DETAIL, tripNo = tripNo, detailFrom = it.page, confirmPlaceNo = null)
    }

    /** B-04 「첫 장소부터 안내」 → B-05 팝업. */
    fun askGuidanceFromFirst() {
        val first = currentTrip()?.places?.minByOrNull { it.placeOrd } ?: return
        askGuidance(first.placeNo)
    }

    /** B-04 장소별 「여기로 안내」 → B-05 팝업. */
    fun askGuidance(placeNo: Long) = updateSelection { it.copy(confirmPlaceNo = placeNo) }

    /** B-05 「취소」. */
    fun dismissGuidance() = updateSelection { it.copy(confirmPlaceNo = null) }

    /** B-05 「안내 시작」 — 길안내 요청 포트를 한 번 부르고 결과를 한 줄 안내로. 팝업은 닫음. */
    fun startGuidance() {
        if (guidance.value.requesting) return
        val sel = selection.value
        val place = currentTrip()?.places?.firstOrNull { it.placeNo == sel.confirmPlaceNo } ?: return
        guidance.value = GuidanceProgress(requesting = true)
        viewModelScope.launch {
            val result = requestRouteGuidance(place)
            updateSelection { it.copy(confirmPlaceNo = null) }
            guidance.value = GuidanceProgress(
                message = when (result) {
                    GuidanceRequestResult.Started -> UiMessage.GUIDANCE_REQUESTED
                    GuidanceRequestResult.Failed -> UiMessage.GUIDANCE_FAILED
                },
            )
        }
    }

    fun onMessageShown() = guidance.update { it.copy(message = null) }

    /** 시스템 뒤로 — 팝업 닫기 → 상세에서 온 화면 → B-03 에서 B-02 → B-02 에서 B-01. B-01 에선 앱 틀이 받음. */
    fun onBack() = updateSelection { sel ->
        when {
            sel.confirmPlaceNo != null -> sel.copy(confirmPlaceNo = null)
            sel.page == RecommendationPage.DETAIL -> sel.copy(page = sel.detailFrom, tripNo = null)
            sel.page == RecommendationPage.TAGS -> sel.copy(page = RecommendationPage.RANKING)
            sel.page == RecommendationPage.RANKING -> sel.copy(page = RecommendationPage.REGION)
            else -> sel
        }
    }

    private fun currentTrip(): RecommendedTrip? {
        val sel = selection.value
        val loaded = sidoTrips.value
        if (sel.tripNo == null || loaded.sidoNm != sel.sidoNm) return null
        return loaded.trips.firstOrNull { it.tripNo == sel.tripNo }
    }

    private fun buildState(
        sel: Selection,
        regions: List<SidoRegion>,
        trips: List<RecommendedTrip>,
        progress: GuidanceProgress,
    ): RecommendationsUiState {
        val sigunguNms = regions.firstOrNull { it.sidoNm == sel.sidoNm }?.sigunguNms.orEmpty()
        val sigunguCnts = if (sel.sidoNm == null) emptyMap() else RecommendationRules.sigunguTripCnts(trips, sel.sidoNm, sigunguNms)
        val sigunguCards = if (sel.sidoNm == null || sigunguNms.isEmpty()) {
            emptyList()
        } else {
            sigunguNms.map { RegionCardItem(it, sigunguCnts.getValue(it)) } + RegionCardItem(null, trips.size)
        }

        val regionTrips = if (sel.sidoNm == null) emptyList() else RecommendationRules.inRegion(trips, Region(sel.sidoNm, sel.sigunguNm))
        val ranked = RecommendationRules.rank(regionTrips, sel.sort, sel.companion).toCards()

        val tagCounts = RecommendationRules.tagCounts(regionTrips)
        val selectedTagNo = sel.tagNo?.takeIf { no -> tagCounts.any { it.tag.tagNo == no } } ?: tagCounts.firstOrNull()?.tag?.tagNo
        val tagged = selectedTagNo?.let { RecommendationRules.withTag(regionTrips, it).toCards() }.orEmpty()

        val trip = trips.firstOrNull { it.tripNo == sel.tripNo }
        val confirmPlace = trip?.places?.firstOrNull { it.placeNo == sel.confirmPlaceNo }

        return RecommendationsUiState(
            // 상세를 그릴 로드맵이 없으면(시드가 바뀐 뒤 복원 등) 온 화면으로
            page = if (sel.page == RecommendationPage.DETAIL && trip == null && trips.isNotEmpty()) sel.detailFrom else sel.page,
            demoData = repository.isDemoData,
            sidoNms = regions.map { it.sidoNm },
            selectedSidoNm = sel.sidoNm,
            sigunguCards = sigunguCards,
            regionSidoNm = sel.sidoNm.orEmpty(),
            regionSigunguNm = sel.sigunguNm,
            sort = sel.sort,
            companion = sel.companion,
            rankedTrips = ranked,
            regionTripCnt = regionTrips.size,
            tagCounts = tagCounts.map { TagCountItem(it.tag.tagNo, it.tag.tagNm, it.cnt) },
            selectedTagNo = selectedTagNo,
            taggedTrips = tagged,
            detail = trip?.toDetail(),
            confirm = confirmPlace?.let { place ->
                GuidanceConfirmItem(
                    placeNo = place.placeNo,
                    placeNm = place.placeNm,
                    addr = place.addr,
                    nicknameNm = trip.nicknameNm,
                    tripNm = trip.tripNm,
                    placeOrd = place.placeOrd,
                )
            },
            guidanceRequesting = progress.requesting,
            guidanceAvailable = requestRouteGuidance.available,
            message = progress.message,
        )
    }

    private fun List<RecommendedTrip>.toCards(): List<TripCardItem> = mapIndexed { index, trip ->
        TripCardItem(
            rank = index + 1,
            tripNo = trip.tripNo,
            tripNm = trip.tripNm,
            nicknameNm = trip.nicknameNm,
            ratingAvg = trip.ratingAvg,
            ratingCnt = trip.ratingCnt,
            placeCnt = trip.placeCnt,
            representativeTagNm = RecommendationRules.representativeTag(trip)?.tagNm,
        )
    }

    private fun RecommendedTrip.toDetail(): TripDetailItem {
        val ordered = places.sortedBy { it.placeOrd }
        return TripDetailItem(
            tripNo = tripNo,
            tripNm = tripNm,
            nicknameNm = nicknameNm,
            ratingAvg = ratingAvg,
            ratingCnt = ratingCnt,
            companionTypes = CompanionType.entries.filter { it in companionTypes },
            placeCnt = placeCnt,
            sidoNm = region.sidoNm,
            sigunguNm = region.sigunguNm,
            places = ordered.map { PlaceItem(it.placeNo, it.placeOrd, it.placeNm, it.tags.map { tag -> tag.tagNm }) },
            sketch = RecommendationRules.sketch(ordered),
        )
    }

    private fun updateSelection(change: (Selection) -> Selection) {
        val next = change(selection.value)
        selection.value = next
        savedStateHandle[KEY_PAGE] = next.page.name
        savedStateHandle[KEY_SIDO] = next.sidoNm
        savedStateHandle[KEY_SIGUNGU] = next.sigunguNm
        savedStateHandle[KEY_SORT] = next.sort.name
        savedStateHandle[KEY_COMPANION] = next.companion?.name
        savedStateHandle[KEY_TAG] = next.tagNo
        savedStateHandle[KEY_TRIP] = next.tripNo
        savedStateHandle[KEY_DETAIL_FROM] = next.detailFrom.name
        savedStateHandle[KEY_CONFIRM_PLACE] = next.confirmPlaceNo
    }

    private fun restoreSelection(): Selection = Selection(
        page = enumOrNull<RecommendationPage>(savedStateHandle[KEY_PAGE]) ?: Selection.START.page,
        sidoNm = savedStateHandle[KEY_SIDO],
        sigunguNm = savedStateHandle[KEY_SIGUNGU],
        sort = enumOrNull<RankingSort>(savedStateHandle[KEY_SORT]) ?: RankingSort.RATING,
        companion = enumOrNull<CompanionType>(savedStateHandle[KEY_COMPANION]),
        tagNo = savedStateHandle[KEY_TAG],
        tripNo = savedStateHandle[KEY_TRIP],
        detailFrom = enumOrNull<RecommendationPage>(savedStateHandle[KEY_DETAIL_FROM]) ?: RecommendationPage.RANKING,
        confirmPlaceNo = savedStateHandle[KEY_CONFIRM_PLACE],
    )

    /** 사용자가 고른 것 — 화면 상태의 원본(저장 · 복원 단위). */
    private data class Selection(
        val page: RecommendationPage,
        val sidoNm: String?,
        val sigunguNm: String?,
        val sort: RankingSort,
        val companion: CompanionType?,
        val tagNo: Long?,
        val tripNo: Long?,
        val detailFrom: RecommendationPage,
        val confirmPlaceNo: Long?,
    ) {
        companion object {
            /** B-01 · 아무것도 고르지 않음. */
            val START = Selection(
                page = RecommendationPage.REGION,
                sidoNm = null,
                sigunguNm = null,
                sort = RankingSort.RATING,
                companion = null,
                tagNo = null,
                tripNo = null,
                detailFrom = RecommendationPage.RANKING,
                confirmPlaceNo = null,
            )
        }
    }

    private data class SidoTrips(val sidoNm: String?, val trips: List<RecommendedTrip>)

    private data class GuidanceProgress(
        val requesting: Boolean = false,
        val message: UiMessage? = null,
    )

    private companion object {
        const val KEY_PAGE = "reco_page"
        const val KEY_SIDO = "reco_sido_nm"
        const val KEY_SIGUNGU = "reco_sigungu_nm"
        const val KEY_SORT = "reco_sort"
        const val KEY_COMPANION = "reco_companion_type_cd"
        const val KEY_TAG = "reco_tag_no"
        const val KEY_TRIP = "reco_trip_no"
        const val KEY_DETAIL_FROM = "reco_detail_from"
        const val KEY_CONFIRM_PLACE = "reco_confirm_place_no"
        const val KEY_ENTRY = "reco_entry_key"

        inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
            enumValues<E>().firstOrNull { it.name == name }
    }
}
