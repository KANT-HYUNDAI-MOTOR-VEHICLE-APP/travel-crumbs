package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내비 목적지 ↔ 요청 장소 대조(R1). */
class RouteMatchRulesTest {

    private val place = Place("namsan-tower", "남산서울타워", "주소", 37.5512, 126.9882)

    private fun route(routing: Boolean = true, name: String = place.name, lat: Double = 37.5512, lon: Double = 126.9882) =
        NaviActiveRoute(routing, name, lat, lon)

    @Test
    fun `R1 안내 중 · 이름 같음 · 좌표 같음 — 같은 목적지`() {
        assertTrue(route().isHeadingTo(place))
    }

    @Test
    fun `R1 좌표가 반경 안에서 조금 다름(도로 위 안내점 등) — 같은 목적지`() {
        assertTrue(route(lat = 37.5512 + 0.0005).isHeadingTo(place)) // 약 56 m
    }

    @Test
    fun `R1 안내 중 아님 · 이름 다름 · 반경 밖 — 다른 목적지`() {
        assertFalse(route(routing = false).isHeadingTo(place))
        assertFalse(route(name = "경복궁").isHeadingTo(place))
        assertFalse(route(lat = 37.5512 + 0.002).isHeadingTo(place)) // 약 222 m
    }

    @Test
    fun `하버사인 — 위도 0_001도는 약 111 m`() {
        assertEquals(111.2, distanceMeters(37.0, 127.0, 37.001, 127.0), 0.5)
    }
}
