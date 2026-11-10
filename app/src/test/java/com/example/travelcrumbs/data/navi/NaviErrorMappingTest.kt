package com.example.travelcrumbs.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import com.example.travelcrumbs.domain.model.RouteFailReason
import org.junit.Assert.assertEquals
import org.junit.Test

/** 내비 오류 코드 → 실패 이유(스펙 navigation 4.2 · U5) — SDK enum 그대로 대조. */
class NaviErrorMappingTest {

    @Test
    fun `T10 너무 짧은 거리 — TOO_CLOSE`() {
        assertEquals(RouteFailReason.TOO_CLOSE, NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE.toFailReason())
    }

    @Test
    fun `T11 거리 초과 — TOO_FAR`() {
        assertEquals(RouteFailReason.TOO_FAR, NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE.toFailReason())
    }

    @Test
    fun `T12 배터리 부족 — LOW_BATTERY`() {
        assertEquals(RouteFailReason.LOW_BATTERY, NaviErrorCode.CAUSE_LOW_BATTERY.toFailReason())
    }

    @Test
    fun `T13 경로 · 트립 탐색 실패와 표에 없는 값 — ROUTE_NOT_FOUND`() {
        assertEquals(RouteFailReason.ROUTE_NOT_FOUND, NaviErrorCode.CAUSE_SEARCH_ROUTE.toFailReason())
        assertEquals(RouteFailReason.ROUTE_NOT_FOUND, NaviErrorCode.CAUSE_SEARCH_TRIP.toFailReason())
        assertEquals(RouteFailReason.ROUTE_NOT_FOUND, routeFailReasonOf("CAUSE_SOMETHING_NEW"))
    }

    @Test
    fun `U5 SDK 의 모든 값이 대응됨 — 새 값이 생겨도 예외 없이 떨어짐`() {
        NaviErrorCode.entries.forEach { it.toFailReason() }
    }
}
