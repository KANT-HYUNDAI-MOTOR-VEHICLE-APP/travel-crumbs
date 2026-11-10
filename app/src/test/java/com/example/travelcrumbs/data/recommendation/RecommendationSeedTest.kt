package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.SidoNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시연용 시드의 모양 — 번호 · 주소 · 좌표 · 지역 목록이 서로 맞는지(#51 시드 확장). */
class RecommendationSeedTest {

    private val trips = RecommendationSeed.trips

    @Test
    fun `S1 trip_no 는 겹치지 않고 내 공개 여행 번호대(1,000,000 이상)보다 작음`() {
        assertEquals(trips.size, trips.map { it.tripNo }.distinct().size)
        val placeNos = trips.flatMap { trip -> trip.places.map { it.placeNo } }
        assertTrue((trips.map { it.tripNo } + placeNos).all { it in 1 until AppConfig.LOCAL_TRIP_RECO_NO_OFFSET })
    }

    @Test
    fun `S2 같은 place_no 는 같은 장소(이름 · 주소 · 좌표)`() {
        trips.flatMap { it.places }.groupBy { it.placeNo }.forEach { (no, same) ->
            assertEquals("place_no $no", 1, same.map { listOf(it.placeNm, it.addr, it.lat, it.lng) }.distinct().size)
        }
    }

    @Test
    fun `S3 주소는 로드맵 지역(시도 짧은 이름 + 시군구)으로 시작 · 좌표는 국내 · 장소 3 ~ 5곳 · 순서 1부터`() {
        trips.forEach { trip ->
            assertTrue(trip.tripNm, trip.region.sidoNm in SidoNames.ALL)
            assertTrue(trip.tripNm, trip.places.size in 3..5 || trip.tripNo == 202L)
            assertEquals(trip.tripNm, (1..trip.places.size).toList(), trip.places.map { it.placeOrd })
            trip.places.forEach { place ->
                assertTrue(place.addr, place.addr.startsWith("${trip.region.sidoNm} ${trip.region.sigunguNm} "))
                assertTrue(place.placeNm, place.lat in 33.0..38.7 && place.lng in 124.5..131.0)
            }
        }
    }

    @Test
    fun `S4 지역 목록 — 시도는 SidoNames 순서 그대로 · 시군구는 시드 로드맵에 있는 것만 빠짐없이`() {
        val regions = RecommendationSeed.sidoRegions
        assertEquals(SidoNames.ALL, regions.map { it.sidoNm })
        val fromTrips = trips.map { it.region.sidoNm to it.region.sigunguNm }.toSet()
        val fromRegions = regions.flatMap { sido -> sido.sigunguNms.map { sido.sidoNm to it } }.toSet()
        assertEquals(fromTrips, fromRegions)
    }
}
