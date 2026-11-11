package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.MyRating
import com.example.travelcrumbs.domain.model.NearbyConsent
import com.example.travelcrumbs.domain.model.NearbyLookupResult
import com.example.travelcrumbs.domain.model.NearbySectionState
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 홈 「내 근처 리뷰 많은 로드맵」 순수 계산(#55 · 스펙 home-nearby 3-3 · H2 ~ H13 · H36). */
class NearbyRoadmapRulesTest {

    private val radiusM = 10_000

    private fun trip(
        tripNo: Long,
        ratingCnt: Int,
        ratingAvg: Double = 4.0,
        places: List<RecommendedPlace> = listOf(place(1, 0.0, 0.0)),
    ) = RecommendedTrip(
        tripNo = tripNo,
        tripNm = "로드맵 $tripNo",
        nicknameNm = "닉",
        region = Region("강원", "속초시"),
        companionTypes = emptySet(),
        ratingAvg = ratingAvg,
        ratingCnt = ratingCnt,
        publishedAt = 0L,
        places = places,
    )

    private fun place(ord: Int, lat: Double, lng: Double) = RecommendedPlace(
        placeNo = ord.toLong(),
        placeNm = "장소 $ord",
        placeOrd = ord,
        addr = "",
        lat = lat,
        lng = lng,
        tags = emptyList(),
    )

    @Test
    fun `H3 리뷰 수 같으면 가까운 쪽이 앞`() {
        val pick = NearbyRoadmapRules.pick(listOf(trip(1, 5), trip(2, 5)), mapOf(1L to 900, 2L to 300), radiusM, 10)!!
        assertEquals(listOf(2L, 1L), pick.roadmaps.map { it.trip.tripNo })
    }

    @Test
    fun `H4 리뷰 수 · 거리 같으면 평균 높은 쪽이 앞`() {
        val pick = NearbyRoadmapRules.pick(
            listOf(trip(1, 5, ratingAvg = 4.1), trip(2, 5, ratingAvg = 4.7)),
            mapOf(1L to 500, 2L to 500),
            radiusM,
            10,
        )!!
        assertEquals(listOf(2L, 1L), pick.roadmaps.map { it.trip.tripNo })
    }

    @Test
    fun `H5 모두 같으면 번호 작은 쪽이 앞 · 여러 번 계산해도 같은 순서`() {
        val trips = listOf(trip(9, 5), trip(3, 5), trip(7, 5))
        val distances = mapOf(9L to 500, 3L to 500, 7L to 500)
        repeat(3) {
            val pick = NearbyRoadmapRules.pick(trips.shuffled(), distances, radiusM, 10)!!
            assertEquals(listOf(3L, 7L, 9L), pick.roadmaps.map { it.trip.tripNo })
        }
    }

    @Test
    fun `H6 첫 장소까지 거리 = 반경이면 포함`() {
        val pick = NearbyRoadmapRules.pick(listOf(trip(1, 5), trip(2, 9)), mapOf(1L to radiusM, 2L to radiusM + 1), radiusM, 10)!!
        assertTrue(pick.nearby)
        assertEquals(listOf(1L), pick.roadmaps.map { it.trip.tripNo })
    }

    @Test
    fun `H7 첫 장소는 반경 밖 · 둘째 장소는 반경 안이면 빠짐`() {
        // 첫 장소 = 약 22km 북쪽 · 둘째 장소 = 내 위치
        val far = trip(1, 5, places = listOf(place(1, 0.2, 0.0), place(2, 0.0, 0.0)))
        val near = trip(2, 1, places = listOf(place(1, 0.0, 0.0)))
        val distances = NearbyRoadmapRules.firstPlaceDistances(listOf(far, near), 0.0, 0.0)
        val pick = NearbyRoadmapRules.pick(listOf(far, near), distances, radiusM, 10)!!

        assertTrue(distances.getValue(1L) > radiusM)
        assertEquals(listOf(2L), pick.roadmaps.map { it.trip.tripNo })
    }

    @Test
    fun `H8 장소 순서가 목록 순서와 다르면 place_ord 가장 작은 장소 기준`() {
        val shuffled = trip(1, 5, places = listOf(place(3, 0.3, 0.0), place(1, 0.0, 0.0), place(2, 0.2, 0.0)))
        assertEquals(1, NearbyRoadmapRules.firstPlace(shuffled)!!.placeOrd)
        assertEquals(0, NearbyRoadmapRules.firstPlaceDistances(listOf(shuffled), 0.0, 0.0).getValue(1L))
    }

    @Test
    fun `H9 반경 안 15개면 10장 · 순위 1 ~ 10`() {
        val trips = (1L..15L).map { trip(it, it.toInt()) }
        val pick = NearbyRoadmapRules.pick(trips, trips.associate { it.tripNo to 100 }, radiusM, 10)!!
        assertEquals(10, pick.roadmaps.size)
        assertEquals((1..10).toList(), pick.roadmaps.map { it.rank })
        assertEquals(15L, pick.roadmaps.first().trip.tripNo)
    }

    @Test
    fun `H10 반경 안 0개 · 전체 1개 이상이면 전국 대체 · 같은 정렬 · 거리 그대로`() {
        val pick = NearbyRoadmapRules.pick(
            listOf(trip(1, 3), trip(2, 8)),
            mapOf(1L to 50_000, 2L to 152_600),
            radiusM,
            10,
        )!!
        assertFalse(pick.nearby)
        assertEquals(listOf(2L, 1L), pick.roadmaps.map { it.trip.tripNo })
        assertEquals(listOf(152_600, 50_000), pick.roadmaps.map { it.distM })
    }

    @Test
    fun `H11 리뷰 0명뿐이면 구역 없음`() {
        assertNull(NearbyRoadmapRules.pick(listOf(trip(1, 0), trip(2, 0)), mapOf(1L to 10, 2L to 10), radiusM, 10))
        assertNull(NearbyRoadmapRules.pick(emptyList(), emptyMap(), radiusM, 10))
    }

    @Test
    fun `H12 리뷰 0명 로드맵(내 공개 여행)은 반경 안이어도 목록에 없음 · 장소 없는 로드맵도 없음`() {
        val mine = trip(AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + 1, 0)
        val empty = trip(5, 9, places = emptyList())
        val seed = trip(2, 4)
        val pick = NearbyRoadmapRules.pick(listOf(mine, empty, seed), mapOf(mine.tripNo to 10, 5L to 10, 2L to 5_000), radiusM, 10)!!
        assertEquals(listOf(2L), pick.roadmaps.map { it.trip.tripNo })
    }

    @Test
    fun `H13 내 별점이 있으면 리뷰 수 +1 · 평균은 내 별점 포함`() {
        val seeded = trip(1, 9, ratingAvg = 4.0)
        val rated = RecommendationRules.withMyRatings(listOf(seeded), mapOf(1L to MyRating(1L, 5, emptySet(), 0L)))
        val card = NearbyRoadmapRules.pick(rated, mapOf(1L to 100), radiusM, 10)!!.roadmaps.single()
        assertEquals(10, card.trip.ratingCnt)
        assertEquals((4.0 * 9 + 5) / 10, card.trip.ratingAvg, 1e-9)
    }

    @Test
    fun `H36 거리 표시 꼴 — 50m · 3249m · 152600m`() {
        assertEquals(DistanceKm(0.1, oneDecimal = true), NearbyRoadmapRules.distanceKm(50))
        assertEquals(DistanceKm(3.2, oneDecimal = true), NearbyRoadmapRules.distanceKm(3_249))
        assertEquals(DistanceKm(153.0, oneDecimal = false), NearbyRoadmapRules.distanceKm(152_600))
        assertEquals(DistanceKm(0.1, oneDecimal = true), NearbyRoadmapRules.distanceKm(100))
        assertEquals(DistanceKm(10.0, oneDecimal = false), NearbyRoadmapRules.distanceKm(10_000))
    }

    @Test
    fun `H55 판정 순서 — 숨김 넷(자격 없음 · 동의 안 함 · 대상 없음 · 위치 못 얻음)의 논리 값이 서로 다름`() {
        val located = NearbyLookupResult.Located(emptyList(), emptyMap())
        val nearby = NearbyRoadmapPick(nearby = true, roadmaps = emptyList())
        val far = NearbyRoadmapPick(nearby = false, roadmaps = emptyList())
        fun state(eligible: Boolean = true, consent: NearbyConsent = NearbyConsent.AGREED, result: NearbyLookupResult? = located, pick: NearbyRoadmapPick? = nearby) =
            NearbyRoadmapRules.sectionState(eligible, consent, result, pick)

        assertEquals(NearbySectionState.INELIGIBLE, state(eligible = false, consent = NearbyConsent.NONE))
        assertEquals(NearbySectionState.CONSENT_NEEDED, state(consent = NearbyConsent.NONE))
        assertEquals(NearbySectionState.CONSENT_DECLINED, state(consent = NearbyConsent.DECLINED))
        assertEquals(NearbySectionState.LOCATING, state(result = null))
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, state(result = NearbyLookupResult.Unavailable))
        assertEquals(NearbySectionState.NO_ROADMAP, state(pick = null))
        assertEquals(NearbySectionState.NEARBY, state())
        assertEquals(NearbySectionState.NATIONWIDE, state(pick = far))
        val hidden = NearbySectionState.entries.filterNot { it.visible }
        assertEquals(
            setOf(
                NearbySectionState.INELIGIBLE,
                NearbySectionState.CONSENT_DECLINED,
                NearbySectionState.NO_ROADMAP,
                NearbySectionState.LOCATION_UNAVAILABLE,
            ),
            hidden.toSet(),
        )
    }

    @Test
    fun `H57 H58 T16 묶음 모양 — 로드맵 추가 · 첫 장소 변경 · 빠짐은 다름 · 별점만 바뀌면 같음`() {
        val base = listOf(trip(1, 5), trip(2, 3))
        val shape = NearbyRoadmapRules.bundleShape(base)
        assertEquals(shape, NearbyRoadmapRules.bundleShape(base.map { it.copy(ratingCnt = it.ratingCnt + 1, ratingAvg = 1.0) }))
        assertTrue(shape != NearbyRoadmapRules.bundleShape(base + trip(3, 1)))
        assertTrue(shape != NearbyRoadmapRules.bundleShape(listOf(base[0])))
        assertTrue(shape != NearbyRoadmapRules.bundleShape(listOf(base[0].copy(places = listOf(place(1, 0.1, 0.0))), base[1])))
    }
}
