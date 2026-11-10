package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.ScreenPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 지도(#59) 순수 계산 — 핀 원본 · 리뷰 수 순위(N12) · 로드맵 후보 순서 · 묶음 · 50개 한도. */
class RecommendationMapRulesTest {

    private fun place(no: Long, ord: Int) = RecommendedPlace(no, "장소 $no", ord, "", 38.2 + no / 100_000.0, 128.59, emptyList())

    private fun trip(no: Long, ratingCnt: Int, vararg placeNos: Long) = RecommendedTrip(
        tripNo = no,
        tripNm = "로드맵 $no",
        nicknameNm = "닉네임 $no",
        region = Region("강원", "속초시"),
        companionTypes = emptySet(),
        ratingAvg = 4.0,
        ratingCnt = ratingCnt,
        publishedAt = 0,
        places = placeNos.mapIndexed { index, placeNo -> place(placeNo, index + 1) },
    )

    // 장소 1001 은 로드맵 둘(101 · 102)이 같이 씀
    private val trips = listOf(trip(102, 9, 1011, 1001), trip(101, 12, 1001, 1002))

    @Test
    fun `M17 같은 장소를 쓰는 로드맵은 핀 하나 · 후보는 리뷰 많은 순`() {
        val places = RecommendationMapRules.mapPlaces(trips)
        assertEquals(1, places.count { it.placeNo == 1001L })

        val roadmaps = RecommendationMapRules.roadmapsOfPlace(trips, 1001L)
        assertEquals(listOf(101L, 102L), roadmaps.map { it.tripNo })
    }

    @Test
    fun `N12 장소 리뷰 수 = 그 장소를 쓰는 공개 로드맵 별점 참여 수 합`() {
        val counts = RecommendationMapRules.placeReviewCnts(trips)
        val expected = trips.filter { trip -> trip.places.any { it.placeNo == 1001L } }.sumOf { it.ratingCnt }
        assertEquals(expected, counts[1001L])
        assertEquals(12 + 9, counts[1001L])
    }

    @Test
    fun `M10 좌표가 없는 장소(0, 0 · 숫자 아님)는 핀에서 뺌`() {
        val base = trips.first()
        val broken = base.copy(
            tripNo = 9_999,
            places = listOf(
                RecommendedPlace(77, "좌표 없음", 1, "", 0.0, 0.0, emptyList()),
                RecommendedPlace(78, "숫자 아님", 2, "", Double.NaN, 128.0, emptyList()),
            ),
        )
        val places = RecommendationMapRules.mapPlaces(listOf(base, broken))
        assertTrue(places.none { it.placeNo == 77L || it.placeNo == 78L })
        assertEquals(base.places.size, places.size)
    }

    @Test
    fun `M34 화면에서 가까운 장소는 묶음 · 멀면 낱개`() {
        val near = listOf(
            ScreenPlace(1, 100f, 100f, reviewCnt = 5),
            ScreenPlace(2, 110f, 105f, reviewCnt = 3),
            ScreenPlace(3, 500f, 500f, reviewCnt = 1),
        )
        val markers = RecommendationMapRules.markers(near, radiusPx = 40f, limit = 50)
        assertEquals(listOf(listOf(1L, 2L), listOf(3L)), markers.map { it.placeNos })

        // 확대하면(화면 거리가 벌어지면) 풀림
        val zoomed = near.map { it.copy(x = it.x * 8, y = it.y * 8) }
        assertEquals(3, RecommendationMapRules.markers(zoomed, radiusPx = 40f, limit = 50).size)
    }

    @Test
    fun `M36 표시는 최대 50개 · 리뷰 많은 장소부터`() {
        val spread = (1..60).map { no -> ScreenPlace(no.toLong(), x = no * 100f, y = 0f, reviewCnt = no) }
        val markers = RecommendationMapRules.markers(spread, radiusPx = 40f, limit = AppConfig.RECO_MAP_MARKER_LIMIT)

        assertEquals(50, markers.size)
        // 리뷰 가장 적은 10곳(1 ~ 10)이 빠짐
        assertEquals((11L..60L).toSet(), markers.flatMap { it.placeNos }.toSet())

        val result = RecommendationMapRules.pinResult(markers, inRangeCnt = spread.size)
        assertEquals(50, result.singlePlaceNos.size)
        assertEquals(60, result.inRangeCnt)
    }

    @Test
    fun `M35 후보 장소는 리뷰 많은 순 · 같으면 장소 번호 순`() {
        val ranked = RecommendationMapRules.rankPlaceNos(listOf(3L, 1L, 2L), mapOf(1L to 5, 2L to 9, 3L to 5))
        assertEquals(listOf(2L, 1L, 3L), ranked)
    }

    @Test
    fun `M03 반경 10km 범위 — 위도 폭은 반경 · 경도 폭은 위도만큼 넓힘`() {
        val center = MapCoord(lng = 128.59, lat = 38.2)
        val bounds = RecommendationMapRules.fitBounds(center, radiusKm = 10.0)

        val latDelta = 10.0 / AppConfig.KM_PER_DEGREE_LAT
        assertEquals(center.lat - latDelta, bounds.south, 1e-9)
        assertEquals(center.lat + latDelta, bounds.north, 1e-9)
        // 북위 38.2도에서 경도 1도는 위도 1도보다 짧음 → 경도 폭이 더 넓음
        val lngDelta = (bounds.east - bounds.west) / 2
        assertEquals(latDelta / kotlin.math.cos(Math.toRadians(38.2)), lngDelta, 1e-9)
        assertTrue(lngDelta > latDelta)
        assertTrue(bounds.contains(center))

        // 적도에서는 같음 · 반경 경계(10km 북쪽 점)는 범위 끝
        val equator = RecommendationMapRules.fitBounds(MapCoord(lng = 0.0, lat = 0.0), radiusKm = 10.0)
        assertEquals(equator.north - 0.0, (equator.east - equator.west) / 2, 1e-9)
        assertTrue(bounds.contains(MapCoord(lng = center.lng, lat = center.lat + latDelta)))
    }
}
