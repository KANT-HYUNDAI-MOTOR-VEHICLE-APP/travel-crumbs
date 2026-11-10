package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.testing.completedTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내 로드맵 계산(#51 U5 · U7) — 지역 · 경로 그림 · 대표 태그. */
class RoadmapRulesTest {

    @Test
    fun `U5-5 지역 — 시도 짧은 이름 · 시군구 · 모르면 null`() {
        assertEquals(Region("강원", "속초시"), RoadmapRules.regionOf("강원특별자치도 속초시 중앙로 147"))
        assertEquals(Region("서울", "종로구"), RoadmapRules.regionOf("서울특별시 종로구 사직로 161"))
        assertEquals(Region("경북", "경주시"), RoadmapRules.regionOf("경상북도 경주시 불국로"))
        assertEquals(Region("부산", "해운대구"), RoadmapRules.regionOf("부산 해운대구 해운대해변로 264"))
        assertEquals(Region("경기", null), RoadmapRules.regionOf("경기도 어딘가"))
        assertNull(RoadmapRules.regionOf("Seoul"))
        assertNull(RoadmapRules.regionOf(null))
    }

    @Test
    fun `U5-6 경로 그림 — 좌표 있는 장소만 순서대로 · 0 ~ 1 안 · 대표 태그는 가장 많이 붙은 것`() {
        val trip = completedTrip(places = 3)
        val pins = RoadmapRules.sketch(trip.places)
        assertEquals(listOf(1, 2, 3), pins.map { it.placeOrd })
        assertTrue(pins.all { it.point.x in 0f..1f && it.point.y in 0f..1f })
        assertTrue("북쪽(위도 큼)이 위", pins[2].point.y < pins[0].point.y)

        val noCoords = trip.places.map { it.copy(lat = null, lng = null) }
        assertTrue(RoadmapRules.sketch(noCoords).isEmpty())
        assertEquals(1L, RoadmapRules.representativeTagNo(trip.places))
        assertEquals(Region("강원", "속초시"), RoadmapRules.regionOfTrip(trip.places))
    }
}
