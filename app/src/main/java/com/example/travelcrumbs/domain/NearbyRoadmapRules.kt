package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NearbyConsent
import com.example.travelcrumbs.domain.model.NearbyLookupResult
import com.example.travelcrumbs.domain.model.NearbySectionState
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import kotlin.math.roundToInt

/** 정렬 · 자른 뒤 카드 한 장의 값 — 로드맵 · 내 위치에서 첫 장소까지 직선 거리(미터) · 순위(1부터). */
data class RankedRoadmap(
    val trip: RecommendedTrip,
    val distM: Int,
    val rank: Int,
)

/** 고른 결과 — [nearby] 거짓이면 반경 안이 0장이라 전국 대체(N2). */
data class NearbyRoadmapPick(
    val nearby: Boolean,
    val roadmaps: List<RankedRoadmap>,
)

/** 거리 표시 꼴의 수치 부분(N16) — [oneDecimal] 참이면 소수 한 자리 km · 거짓이면 정수 km. */
data class DistanceKm(
    val km: Double,
    val oneDecimal: Boolean,
)

/**
 * 홈 「내 근처 리뷰 많은 로드맵」(#55 · 스펙 home-nearby 3-3)의 순수 계산 — 첫 장소 거리 · 거르기 · 정렬 · 자르기 · 순위 · 거리 표시 꼴.
 * 내 위치 좌표는 [firstPlaceDistances] 의 인자로만 들어오고 결과(로드맵별 거리)에 남지 않음(3-5).
 */
object NearbyRoadmapRules {

    /** 로드맵의 첫 장소 — 목록 순서가 아니라 장소 순서(place_ord)가 가장 작은 것(H8). 장소가 없으면 null. */
    fun firstPlace(trip: RecommendedTrip): RecommendedPlace? = trip.places.minByOrNull { it.placeOrd }

    /** 로드맵 번호 → 내 위치에서 첫 장소까지 하버사인 직선 거리(미터 · 반올림 정수). 장소가 없는 로드맵은 빠짐. */
    fun firstPlaceDistances(trips: List<RecommendedTrip>, lat: Double, lng: Double): Map<Long, Int> =
        trips.mapNotNull { trip ->
            firstPlace(trip)?.let { place -> trip.tripNo to distanceMeters(lat, lng, place.lat, place.lng).roundToInt() }
        }.toMap()

    /**
     * 카드 목록 — 장소 없음 · 리뷰 0명 · 거리 모름은 뺌(N3) → 반경([radiusM] · 경계 포함) 안이 1장 이상이면 그것만 · 아니면 전국(N2) →
     * 리뷰 수 많은 순 → 가까운 순 → 별점 평균 높은 순 → 로드맵 번호 작은 순 → 최대 [maxCnt] 장 · 순위 1부터.
     * 대상이 하나도 없으면 null(구역 없음).
     */
    fun pick(
        trips: List<RecommendedTrip>,
        distances: Map<Long, Int>,
        radiusM: Int = AppConfig.NEARBY_ROADMAP_RADIUS_KM * METERS_PER_KM,
        maxCnt: Int = AppConfig.NEARBY_ROADMAP_MAX_CNT,
    ): NearbyRoadmapPick? {
        val candidates = trips
            .filter { it.places.isNotEmpty() && it.ratingCnt > 0 }
            .mapNotNull { trip -> distances[trip.tripNo]?.let { trip to it } }
            .distinctBy { (trip, _) -> trip.tripNo }
        if (candidates.isEmpty()) return null
        val inRadius = candidates.filter { (_, dist) -> dist <= radiusM }
        val nearby = inRadius.isNotEmpty()
        val chosen = (if (nearby) inRadius else candidates)
            .sortedWith(ORDER)
            .take(maxCnt)
            .mapIndexed { index, (trip, dist) -> RankedRoadmap(trip, dist, index + 1) }
        return NearbyRoadmapPick(nearby, chosen)
    }

    /**
     * 구역 논리 상태 판정(스펙 3-2 판정 순서) — ① 화면 자격 없음 → 자격 없음 · ② 이력 없음 → 동의 필요 · 동의 안 함 →
     * 동의 안 함 · 동의함일 때만 마지막 조회 결과로 나머지 다섯([result] null = 결과 없음 → 위치 확인 중 · [pick] = 그 결과를 지금 자료로 고른 것).
     */
    fun sectionState(
        eligible: Boolean,
        consent: NearbyConsent,
        result: NearbyLookupResult?,
        pick: NearbyRoadmapPick?,
    ): NearbySectionState = when {
        !eligible -> NearbySectionState.INELIGIBLE
        consent == NearbyConsent.NONE -> NearbySectionState.CONSENT_NEEDED
        consent == NearbyConsent.DECLINED -> NearbySectionState.CONSENT_DECLINED
        result == null -> NearbySectionState.LOCATING
        result == NearbyLookupResult.Unavailable -> NearbySectionState.LOCATION_UNAVAILABLE
        pick == null -> NearbySectionState.NO_ROADMAP
        pick.nearby -> NearbySectionState.NEARBY
        else -> NearbySectionState.NATIONWIDE
    }

    /**
     * 공개 로드맵 묶음의 모양(로드맵 번호 · 장소 번호 · 장소 순서 · 장소 좌표) — 이전 조회 때와 다르면 위치를 다시 조회(T18).
     * 별점 · 이름만 바뀐 것은 같은 모양(T16 무조회 재정렬).
     */
    fun bundleShape(trips: List<RecommendedTrip>): Set<Pair<Long, List<RecommendedPlace>>> =
        trips.map { trip -> trip.tripNo to trip.places.sortedBy { it.placeOrd }.map { it.copy(placeNm = "", addr = "", tags = emptyList()) } }
            .toSet()

    /** 거리 표시 꼴(N16) — 100m 미만 0.1km · 10km 미만 소수 한 자리 반올림 · 그 이상 정수 반올림. */
    fun distanceKm(distM: Int): DistanceKm = when {
        distM < MIN_SHOWN_M -> DistanceKm(MIN_SHOWN_KM, oneDecimal = true)
        distM < WHOLE_KM_FROM_M -> DistanceKm((distM / HUNDRED_M).roundToInt() / TENTHS, oneDecimal = true)
        else -> DistanceKm((distM / METERS_PER_KM.toDouble()).roundToInt().toDouble(), oneDecimal = false)
    }

    private val ORDER: Comparator<Pair<RecommendedTrip, Int>> =
        compareByDescending<Pair<RecommendedTrip, Int>> { it.first.ratingCnt }
            .thenBy { it.second }
            .thenByDescending { it.first.ratingAvg }
            .thenBy { it.first.tripNo }

    const val METERS_PER_KM: Int = 1_000
    private const val MIN_SHOWN_M = 100
    private const val MIN_SHOWN_KM = 0.1
    private const val WHOLE_KM_FROM_M = 10_000
    private const val HUNDRED_M = 100.0
    private const val TENTHS = 10.0
}
