package com.example.travelcrumbs.ui.recommendations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.map.OfflineMapRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.RoadmapMapRules
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.OfflineMapPack
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RatingRecords
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.RouteMap
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.ui.components.RatingDialogItem
import com.example.travelcrumbs.ui.rating.RatingPopupHolder
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
 * 계산(정렬 · 개수 · 대표 태그 · 경로 그림 · 내 별점 반영)은 도메인 [RecommendationRules] · 자료는 저장소(추천 · 별점).
 * 별점(#41 B-06): 안내 요청이 성공하면 그 로드맵을 별점 저장소에 남김(실패면 남기지 않음) · B-04 「별점 주기」 / 「내 별점 ★n · 고치기」.
 */
@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val repository: RecommendationRepository,
    private val requestRouteGuidance: RequestRouteGuidanceUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val ratingRepository: RatingRepository,
    timeSource: TimeSource,
    private val offlineMap: OfflineMapRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(restoreSelection())
    private val guidance = MutableStateFlow(GuidanceProgress())

    private val sidoRegions = MutableStateFlow<List<SidoRegion>>(emptyList())
    private val sidoTrips = MutableStateFlow(SidoTrips(sidoNm = null, trips = emptyList()))

    /** 태그 마스터 — 태그별 개수에 내 별점 태그 이름을 찾을 때. */
    private val tags = MutableStateFlow<List<Tag>>(emptyList())

    /** B-06 별점 팝업(#41) — B-04 버튼으로만 엶(자동 팝업은 기록 칸). */
    private val rating = RatingPopupHolder(
        scope = viewModelScope,
        repository = ratingRepository,
        savedStateHandle = savedStateHandle,
        timeSource = timeSource,
        autoPrompt = false,
        loadTags = repository::tags,
        onSaved = { guidance.update { it.copy(message = UiMessage.RATING_SAVED) } },
    )

    /** 별점 기록(저장소 Flow 그대로)과 팝업 · 태그 마스터를 한 덩어리로. */
    private val ratingPart = combine(ratingRepository.records, rating.item, tags) { records, dialog, tagList ->
        RatingInputs(records, dialog, tagList)
    }
    /** 상세 도로 지도 묶음(앱 에셋) — 못 읽었거나 읽기 전이면 null → 기존 경로 그림. */
    private val mapPack = MutableStateFlow<OfflineMapPack?>(null)
    private var cachedRouteMap: CachedRouteMap? = null

    val uiState: StateFlow<RecommendationsUiState> = combine(
        selection,
        sidoRegions,
        sidoTrips,
        guidance,
        combine(ratingPart, mapPack, ::Pair),
    ) { sel, regions, loaded, progress, (ratingInputs, pack) ->
        // 시도를 바꾼 직후 옛 시도 로드맵이 한 번 섞이지 않게 — 불러온 시도가 지금 시도일 때만 씀
        val trips = if (loaded.sidoNm == sel.sidoNm) loaded.trips else emptyList()
        buildState(sel, regions, trips, progress, ratingInputs, pack)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = RecommendationsUiState(
            demoData = repository.isDemoData,
        ),
    )

    init {
        // 자료는 화면 구독과 따로 불러 둠 — 행동(첫 장소 · 안내 목적지)이 화면이 안 보일 때도 같은 값을 보게
        viewModelScope.launch { sidoRegions.value = repository.sidoRegions() }
        viewModelScope.launch { tags.value = repository.tags() }
        viewModelScope.launch { mapPack.value = offlineMap.mapPack() }
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
    fun onEntry(entryKey: Int, region: Region? = null) {
        if (savedStateHandle.get<Int>(KEY_ENTRY) == entryKey) return
        savedStateHandle[KEY_ENTRY] = entryKey
        updateSelection { Selection.START }
        guidance.value = GuidanceProgress()
        rating.clear()
        // Gleo 「경주 근처 추천」(#69) — 고르기(B-01)를 건너뛰고 그 지역 순위(B-02)부터. 시군구 null = 시도 전체.
        if (region != null) {
            selectSido(region.sidoNm)
            selectRegion(region.sigunguNm)
        }
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
        // 로드맵 · 장소를 먼저 구함 — 못 구하면 「요청 중」을 켜지 않음(버튼이 잠긴 채 남지 않게)
        val trip = currentTrip() ?: return
        val place = trip.places.firstOrNull { it.placeNo == sel.confirmPlaceNo } ?: return
        guidance.value = GuidanceProgress(requesting = true)
        viewModelScope.launch {
            val result = requestRouteGuidance(place)
            // 별점 자격 — 내비가 안내를 시작했을 때만 남김(실패 · 한도 지남은 남기지 않음 · 사람 결정)
            if (result == GuidanceRequestResult.Started) {
                ratingRepository.recordGuidance(trip.tripNo, trip.tripNm, trip.nicknameNm)
            }
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

    /** B-04 「별점 주기」 · 「내 별점 ★n · 고치기」 → B-06 팝업(이미 준 별점이면 그 값으로 채움). 안내 기록이 없으면 아무 일 없음. */
    fun openRating() {
        val tripNo = selection.value.tripNo ?: return
        rating.open(tripNo)
    }

    fun selectRatingScore(score: Int) = rating.selectScore(uiState.value.rating, score)

    fun toggleRatingTag(tagNo: Long) = rating.toggleTag(uiState.value.rating, tagNo)

    /** B-06 「나중에」 · 바깥 · 뒤로 — 닫기 · 그 로드맵의 자동 팝업도 끝난 것으로. */
    fun dismissRating() = rating.dismiss(uiState.value.rating)

    /** B-06 「별점 주기」 — 저장되면 닫고 한 줄 안내 · 평균 · 참여 수 · 태그 수는 저장소 기록이 바뀌며 바로 다시 계산됨. */
    fun submitRating() = rating.submit(uiState.value.rating)

    /** 시스템 뒤로 — 팝업 닫기 → 상세에서 온 화면 → B-03 에서 B-02 → B-02 에서 B-01. B-01 에선 앱 틀이 받음. */
    fun onBack() {
        if (uiState.value.rating != null) {
            dismissRating()
            return
        }
        updateSelection { sel -> backFrom(sel) }
    }

    private fun backFrom(sel: Selection): Selection =
        when {
            sel.confirmPlaceNo != null -> sel.copy(confirmPlaceNo = null)
            sel.page == RecommendationPage.DETAIL -> sel.copy(page = sel.detailFrom, tripNo = null)
            sel.page == RecommendationPage.TAGS -> sel.copy(page = RecommendationPage.RANKING)
            sel.page == RecommendationPage.RANKING -> sel.copy(page = RecommendationPage.REGION)
            else -> sel
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
        rawTrips: List<RecommendedTrip>,
        progress: GuidanceProgress,
        ratingInputs: RatingInputs,
        pack: OfflineMapPack?,
    ): RecommendationsUiState {
        val records = ratingInputs.records
        val myRatings = records.ratingsByTrip
        // 내 별점을 평균 · 참여 수에 더한 값으로 모든 계산(정렬 · 카드 · 상세)
        val trips = RecommendationRules.withMyRatings(rawTrips, myRatings)
        val sigunguNms = regions.firstOrNull { it.sidoNm == sel.sidoNm }?.sigunguNms.orEmpty()
        val sigunguCnts = if (sel.sidoNm == null) emptyMap() else RecommendationRules.sigunguTripCnts(trips, sel.sidoNm, sigunguNms)
        val sigunguCards = if (sel.sidoNm == null || sigunguNms.isEmpty()) {
            emptyList()
        } else {
            sigunguNms.map { RegionCardItem(it, sigunguCnts.getValue(it)) } + RegionCardItem(null, trips.size)
        }

        val regionTrips = if (sel.sidoNm == null) emptyList() else RecommendationRules.inRegion(trips, Region(sel.sidoNm, sel.sigunguNm))
        val ranked = RecommendationRules.rank(regionTrips, sel.sort, sel.companion).toCards()

        val tagCounts = RecommendationRules.tagCounts(regionTrips, myRatings, ratingInputs.tags)
        val selectedTagNo = sel.tagNo?.takeIf { no -> tagCounts.any { it.tag.tagNo == no } } ?: tagCounts.firstOrNull()?.tag?.tagNo
        val tagged = selectedTagNo?.let { RecommendationRules.withTag(regionTrips, it, myRatings).toCards() }.orEmpty()

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
            detail = trip?.toDetail(records, pack),
            rating = ratingInputs.dialog,
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

    private fun RecommendedTrip.toDetail(records: RatingRecords, pack: OfflineMapPack?): TripDetailItem {
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
            canRate = RecommendationRules.canRate(records, tripNo),
            myRatingScore = records.ratingOf(tripNo)?.ratingScore,
            routeMap = pack?.let { projectedRouteMap(it, ordered) },
        )
    }

    /** 별점 · 팝업 등 상세 화면 상태가 달라져도 같은 지도 묶음과 장소 목록이면 평면 좌표를 다시 계산하지 않음. */
    private fun projectedRouteMap(pack: OfflineMapPack, places: List<RecommendedPlace>): RouteMap? {
        val cached = cachedRouteMap
        if (cached != null && cached.pack === pack && cached.places == places) return cached.map
        return RoadmapMapRules.routeMap(pack, places).also { cachedRouteMap = CachedRouteMap(pack, places, it) }
    }

    private data class CachedRouteMap(val pack: OfflineMapPack, val places: List<RecommendedPlace>, val map: RouteMap?)

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

    private data class RatingInputs(
        val records: RatingRecords,
        val dialog: RatingDialogItem?,
        val tags: List<Tag>,
    )

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
