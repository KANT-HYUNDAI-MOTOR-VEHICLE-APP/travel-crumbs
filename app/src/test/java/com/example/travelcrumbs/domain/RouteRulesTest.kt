package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRulesTest {

    private val gangneungStation = RoutePoint(37.7645, 128.8996, 0L)

    @Test
    fun `첫 점은 늘 남김`() {
        assertTrue(RouteRules.shouldKeep(null, gangneungStation))
    }

    @Test
    fun `20미터 안쪽은 멈춘 것으로 보고 남기지 않음`() {
        val near = gangneungStation.copy(lat = 37.76455, recordedAtMillis = 5_000L) // 약 5.6m
        assertFalse(RouteRules.shouldKeep(gangneungStation, near))
    }

    @Test
    fun `20미터 넘게 움직이면 남김`() {
        val moved = gangneungStation.copy(lat = 37.7650, recordedAtMillis = 5_000L) // 약 55.6m
        assertTrue(RouteRules.shouldKeep(gangneungStation, moved))
    }

    @Test
    fun `거리 계산 — 위도 0_001도는 약 111미터`() {
        val north = gangneungStation.copy(lat = gangneungStation.lat + 0.001)
        assertEquals(111.2, RouteRules.distanceMeters(gangneungStation, north), 0.5)
    }
}
