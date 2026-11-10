package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.data.map.OfflineMapRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.NearbyRoadmapRules
import com.example.travelcrumbs.domain.RankedRoadmap
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.RoadmapMapRules
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.NearbyConsent
import com.example.travelcrumbs.domain.model.NearbyLookupResult
import com.example.travelcrumbs.domain.model.OfflineMapPack
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.RouteMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 홈 「내 근처 리뷰 많은 로드맵」(#55 · 스펙 home-nearby 3-1 · 3-2) — 기록 칸 ViewModel 이 소유하는 상태 도우미.
 *
 * 전이의 원천은 다섯뿐: ① 화면 자격([eligible] + 화면이 보임) ② 근처 위치 동의(LOCATION_RECO 최신 이력) ③ 내비 연결 ·
 * ④ 위치 조회 결과 ⑤ 로드맵 자료(별점 변화 · 공개 로드맵 묶음 변화 [publicTripChanges]). 논리 상태는 도메인
 * [NearbyRoadmapRules.sectionState](판정 순서) · 전이 번호는 스펙 3-2 의 T1 ~ T20.
 *
 * 위치는 기존 내비 현재 위치 조회([NaviRepository.currentLocation]) 1회 · 동의함(Y)일 때만 · 결과 좌표는 로드맵별 거리(미터)로
 * 바꾼 즉시 버림 — 이 도우미 · 화면 상태 · 저장 · 로그 어디에도 내 좌표 · 주소가 남지 않음(3-5). 화면이 안 보이면 구독 · 조회 없음.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyRoadmapsHolder(
    private val scope: CoroutineScope,
    private val recommendations: RecommendationRepository,
    ratings: RatingRepository,
    private val navi: NaviRepository,
    private val users: CurrentUserRepository,
    offlineMap: OfflineMapRepository,
    eligible: Flow<Boolean>,
    publicTripChanges: Flow<Any?>,
) {
    /** 원천 ① ② ③ 의 지금 값 — 동의를 아직 못 읽었으면 [consent] null(자격 없음처럼 그림 · 동의 카드가 깜박이지 않게). */
    private data class Inputs(val eligible: Boolean, val consent: NearbyConsent?, val ready: Boolean)

    /** 화면 상태의 원본 — 마지막 조회 결과(메모리에만 · 좌표 없음)와 원천 값. */
    private data class Core(val inputs: Inputs, val result: NearbyLookupResult?)

    private val screenShown = MutableStateFlow(false)
    private val mapPack = MutableStateFlow<OfflineMapPack?>(null)
    private var inputs = Inputs(eligible = false, consent = null, ready = false)
    private var result: NearbyLookupResult? = null
    private val core = MutableStateFlow(Core(inputs, result))
    private var lookup: Job? = null
    private val routeMapCache = mutableMapOf<Long, CachedRouteMap>()

    /** 지금 그릴 구역 — 논리 상태 + 카드(근처 있음 · 전국 대체만). 별점이 바뀌면 마지막 거리로 다시 정렬(T16 · 조회 0). */
    val section: Flow<NearbyRoadmapsSection> = combine(core, ratings.records, mapPack) { current, records, pack ->
        val located = current.result as? NearbyLookupResult.Located
        val pick = located?.let {
            NearbyRoadmapRules.pick(RecommendationRules.withMyRatings(it.trips, records.ratingsByTrip), it.distances)
        }
        val consent = current.inputs.consent
        val state = NearbyRoadmapRules.sectionState(
            eligible = current.inputs.eligible && consent != null,
            consent = consent ?: NearbyConsent.NONE,
            result = current.result,
            pick = pick,
        )
        NearbyRoadmapsSection(
            state = state,
            cards = if (state.showsCards) pick?.roadmaps.orEmpty().map { it.toCard(pack) } else emptyList(),
            radiusKm = AppConfig.NEARBY_ROADMAP_RADIUS_KM,
            demoData = recommendations.isDemoData,
        )
    }

    init {
        scope.launch { mapPack.value = offlineMap.mapPack() }
        // ① ② ③ — 화면이 보일 때만 구독(안 보이면 자격 없음 · 기록 경과 시계 등도 붙잡지 않음 · H27)
        scope.launch {
            screenShown
                .flatMapLatest { shown ->
                    if (!shown) {
                        flowOf(Inputs(eligible = false, consent = null, ready = false))
                    } else {
                        combine(
                            eligible.distinctUntilChanged(),
                            users.consents.map { NearbyConsent.of(it[ConsentType.LOCATION_RECO]?.agreed) },
                            navi.ready,
                        ) { ok, consent, ready -> Inputs(ok, consent, ready) }
                    }
                }
                .collect(::onInputs)
        }
        // ⑤ 공개 로드맵 묶음 — 화면이 보일 때만
        scope.launch {
            screenShown
                .flatMapLatest { shown -> if (shown) publicTripChanges else emptyFlow() }
                .collect { onBundleMaybeChanged() }
        }
    }

    /** 기록 칸 화면이 앞에 옴(시작 · 재개). */
    fun onScreenShown() {
        screenShown.value = true
    }

    /** 기록 칸 화면이 뒤로 감 · 사라짐 — ① 없음(T1). */
    fun onScreenHidden() {
        screenShown.value = false
    }

    /** 동의 카드 「동의하고 보기」(K3 · T6) — 이력 행이 쌓이면 ② 흐름이 위치 확인 중으로 바꿈. */
    fun agree() {
        scope.launch { users.recordConsent(ConsentType.LOCATION_RECO, agreed = true) }
    }

    /** 동의 카드 「괜찮아요」(K4 · T7 · N20) — 거부 행 · 조회 0. */
    fun decline() {
        scope.launch { users.recordConsent(ConsentType.LOCATION_RECO, agreed = false) }
    }

    private fun onInputs(next: Inputs) {
        val prev = inputs
        inputs = next
        val wasActive = prev.eligible && prev.consent == NearbyConsent.AGREED
        when {
            // T1 — 진행 중 조회 취소 · 늦은 결과 버림 · 마지막 결과는 메모리에 둠
            !next.eligible || next.consent == null -> cancelLookup()
            // T2 · T3 · T7 · T9 — 동의가 없거나 철회됨: 조회 0 · 철회면 마지막 거리도 지움
            next.consent != NearbyConsent.AGREED -> {
                cancelLookup()
                result = null
            }
            // T4 · T5 · T6 · T8 — 새로 조회할 수 있게 됨: 보이던 카드(근처 있음 · 전국 대체)면 그대로 두고 새로 고침
            !wasActive -> {
                if (!showedCards()) result = null
                startLookup()
            }
            // T14 — 위치 못 얻음에서 내비가 연결됨 · T15 그 밖의 연결 변화는 그대로
            result == NearbyLookupResult.Unavailable && next.ready && !prev.ready -> {
                result = null
                startLookup()
            }
        }
        publish()
    }

    /** ⑤ 묶음이 바뀌었을 수 있음 — 조회 결과가 있을 때만 모양을 대조해 다르면 다시 조회(T18) · 조회 중(T17) · 위치 못 얻음(T19)은 그대로. */
    private suspend fun onBundleMaybeChanged() {
        if (!(inputs.eligible && inputs.consent == NearbyConsent.AGREED)) return
        if (lookup?.isActive == true) return
        val located = result as? NearbyLookupResult.Located ?: return
        val now = runCatching { allPublicTrips() }.getOrNull() ?: return
        if (lookup?.isActive == true || result !== located) return
        if (NearbyRoadmapRules.bundleShape(now) != NearbyRoadmapRules.bundleShape(located.trips)) startLookup()
    }

    /** 지금 결과가 카드를 보이는 상태(근처 있음 · 전국 대체)인가 — 별점 반영 전 값으로도 카드 유무는 같음(리뷰 0명 제외만 다를 수 있음). */
    private fun showedCards(): Boolean {
        val located = result as? NearbyLookupResult.Located ?: return false
        return NearbyRoadmapRules.pick(located.trips, located.distances) != null
    }

    private fun startLookup() {
        if (lookup?.isActive == true) return
        lookup = scope.launch {
            result = locate()
            publish()
        }
    }

    private fun cancelLookup() {
        lookup?.cancel()
        lookup = null
    }

    private fun publish() {
        core.value = Core(inputs, result)
    }

    /** 위치 1회 → 로드맵별 거리. 내비 연결 안 됨 · 오류 · 한도 지남이면 위치 못 얻음(안내 없음 · 값을 남기지 않음). */
    private suspend fun locate(): NearbyLookupResult = try {
        if (!navi.ready.value) {
            NearbyLookupResult.Unavailable
        } else {
            withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { navi.currentLocation() }?.let { here ->
                // 조회 결과가 온 때의 최신 자료로 계산(T17) · 주소는 쓰지 않음
                val trips = allPublicTrips()
                NearbyLookupResult.Located(trips, NearbyRoadmapRules.firstPlaceDistances(trips, here.lat, here.lng))
            } ?: NearbyLookupResult.Unavailable
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        NearbyLookupResult.Unavailable
    }

    /** 추천 칸과 같은 자료 — 모든 시도의 공개 로드맵(시드 + 내 공개 여행). */
    private suspend fun allPublicTrips(): List<RecommendedTrip> =
        recommendations.sidoRegions().map { it.sidoNm }.distinct()
            .flatMap { recommendations.publicTrips(it) }
            .distinctBy { it.tripNo }

    private fun RankedRoadmap.toCard(pack: OfflineMapPack?): NearbyRoadmapCardItem {
        val ordered = trip.places.sortedBy { it.placeOrd }
        return NearbyRoadmapCardItem(
            tripNo = trip.tripNo,
            rank = rank,
            tripNm = trip.tripNm,
            sidoNm = trip.region.sidoNm,
            sigunguNm = trip.region.sigunguNm,
            ratingAvg = trip.ratingAvg,
            ratingCnt = trip.ratingCnt,
            distM = distM,
            routeMap = pack?.let { routeMapOf(it, trip.tripNo, ordered) },
            sketch = RecommendationRules.sketch(ordered),
        )
    }

    /** 같은 묶음 · 같은 장소면 평면 좌표를 다시 계산하지 않음(별점이 바뀌어 다시 정렬할 때). */
    private fun routeMapOf(pack: OfflineMapPack, tripNo: Long, places: List<RecommendedPlace>): RouteMap? {
        val cached = routeMapCache[tripNo]
        if (cached != null && cached.pack === pack && cached.places == places) return cached.map
        return RoadmapMapRules.routeMap(pack, places).also { routeMapCache[tripNo] = CachedRouteMap(pack, places, it) }
    }

    private data class CachedRouteMap(val pack: OfflineMapPack, val places: List<RecommendedPlace>, val map: RouteMap?)
}
