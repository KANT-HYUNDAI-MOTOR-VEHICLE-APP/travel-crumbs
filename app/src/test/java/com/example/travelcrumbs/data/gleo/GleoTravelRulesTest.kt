package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.domain.GleoTravelRules
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #69 스펙 3.3 · T8 T9 T10 ~ T10c — 기기 없이 도는 순수 규칙. */
class GleoTravelRulesTest {
    private val trips = RecommendationSeed.trips
    private val gyeongju = Region("경북", "경주시")

    @Test
    fun `T10 「경주」 · 「경주시」 · 「경북 경주」 · 「경상북도 경주시」는 모두 경주시`() {
        for (spoken in listOf("경주", "경주시", "경북 경주", "경상북도 경주시", " 경주 ")) {
            assertEquals(spoken, listOf(gyeongju), GleoTravelRules.matchRegions(spoken, trips))
        }
    }

    @Test
    fun `T10a 시도만 말하면 그 시도 전체`() {
        assertEquals(listOf(Region("제주", null)), GleoTravelRules.matchRegions("제주", trips))
        assertEquals(listOf(Region("서울", null)), GleoTravelRules.matchRegions("서울특별시", trips))
        val jeju = GleoTravelRules.inAny(trips, listOf(Region("제주", null)))
        assertTrue(jeju.isNotEmpty())
        assertTrue(jeju.all { it.region.sidoNm == "제주" })
    }

    @Test
    fun `T10b 시드에 없는 지역은 빈 목록 — 다른 지역으로 바꾸지 않음`() {
        assertTrue(GleoTravelRules.matchRegions("대구", trips).isEmpty())
        assertTrue(GleoTravelRules.matchRegions("뉴욕", trips).isEmpty())
        assertTrue(GleoTravelRules.matchRegions("  ", trips).isEmpty())
    }

    @Test
    fun `함께 말한 시도로 같은 이름의 시군구를 가림`() {
        val busanJung = GleoTravelRules.matchRegions("부산 중구", trips)
        assertEquals(listOf(Region("부산", "중구")), busanJung)
        assertTrue(GleoTravelRules.matchRegions("서울 중구", trips).let { it.isEmpty() || it == listOf(Region("서울", null)) })
    }

    @Test
    fun `T10c 위치가 있으면 첫 장소가 가장 가까운 로드맵의 지역`() {
        val gangnamStation = CurrentLocation(37.4979, 127.0276, null)
        assertEquals(Region("서울", "강남구"), GleoTravelRules.nearestRegion(gangnamStation, trips))
        val bulguksa = CurrentLocation(35.79, 129.33, null)
        assertEquals(gyeongju, GleoTravelRules.nearestRegion(bulguksa, trips))
        assertNull(GleoTravelRules.nearestRegion(null, trips))
    }

    @Test
    fun `T8 T9 오늘 장소 수는 기기 시간대 자정 이후만 셈`() {
        val zone = ZoneId.of("Asia/Seoul")
        fun at(h: Int, m: Int, day: Int = 10) = LocalDateTime.of(2026, 11, day, h, m).atZone(zone).toInstant().toEpochMilli()
        val now = at(15, 0)
        fun place(no: Long, at: Long) = TripPlace(no, 1L, no.toInt(), null, null, true, null, null, null, at, emptyList())
        fun trip(no: Long, vararg places: TripPlace) =
            TripRecord(no, null, TripStatus.RECORDING, Visibility.PRIVATE, 0L, null, null, null, emptySet(), places.toList())
        val records = listOf(
            trip(1L, place(1, at(9, 0)), place(2, at(23, 59, day = 9))),
            trip(2L, place(3, at(10, 0)), place(4, at(14, 59))),
        )
        assertEquals(3, GleoTravelRules.todayPlaceCount(records, now, zone))
    }
}
