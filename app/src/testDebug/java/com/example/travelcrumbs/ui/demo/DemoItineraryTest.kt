package com.example.travelcrumbs.ui.demo

import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoItineraryTest {
    private val first = Place("first", "첫 장소", "", 0.0, 0.0)
    private val second = Place("second", "둘째 장소", "", 0.0, 0.0)
    private val guiding = DemoItinerary("계정", "roadmap", "로드맵", listOf(first, second), phase = DemoLegPhase.Guiding)

    @Test
    fun `현재 목적지 도착만 다음 장소를 준비하며 안내를 다시 시작하지 않음`() {
        assertSame(guiding, guiding.arrive(second.name))
        val next = guiding.arrive(first.name)
        assertEquals(1, next.arrivedCount)
        assertEquals(second, next.target)
        assertEquals(DemoLegPhase.Ready, next.phase)
        assertTrue(next.canRequest)
        assertSame(next, next.arrive(first.name))
        assertSame(next, next.arrive(second.name))
    }

    @Test
    fun `명시적 종료는 같은 목적지에서 재시도하며 뒤늦은 도착 알림을 무시`() {
        val cancelled = guiding.cancel()
        assertEquals(DemoLegPhase.Cancelled, cancelled.phase)
        assertEquals(0, cancelled.arrivedCount)
        assertEquals(first, cancelled.target)
        assertSame(cancelled, cancelled.arrive(first.name))
        assertTrue(cancelled.canRequest)
    }

    @Test
    fun `마지막 장소 도착은 완료되고 중복 도착으로 진행 수가 늘지 않음`() {
        val last = guiding.arrive(first.name).copy(phase = DemoLegPhase.Guiding)
        val complete = last.arrive(second.name)
        assertEquals(DemoLegPhase.Complete, complete.phase)
        assertEquals(2, complete.arrivedCount)
        assertNull(complete.target)
        assertFalse(complete.canRequest)
        assertSame(complete, complete.arrive(second.name))
        assertSame(complete, complete.cancel())
    }

    @Test
    fun `SDK 시작 콜백이 빠져도 실제 요청한 목적지에 도착하면 진행`() {
        val pending = guiding.copy(phase = DemoLegPhase.Requesting)
        assertSame(pending, pending.arrive(first.name))
        assertSame(pending, pending.cancel())

        val dispatched = pending.copy(requestDispatched = true)
        val timedOut = dispatched.copy(phase = DemoLegPhase.Failed(RouteFailReason.TIMEOUT))
        assertSame(dispatched, dispatched.arrive(second.name))
        assertEquals(second, dispatched.arrive(first.name).target)
        assertEquals(second, timedOut.arrive(first.name).target)
        assertFalse(timedOut.arrive(first.name).lastArrivalSimulated)
        assertTrue(timedOut.arrive(first.name, simulated = true).lastArrivalSimulated)
    }

    @Test
    fun `시작을 확인하지 못한 실제 요청도 명시적으로 취소하면 뒤늦은 도착을 무시`() {
        val timedOut = guiding.copy(
            phase = DemoLegPhase.Failed(RouteFailReason.TIMEOUT),
            requestDispatched = true,
        )
        val cancelled = timedOut.cancel()
        assertEquals(DemoLegPhase.Cancelled, cancelled.phase)
        assertEquals(first, cancelled.target)
        assertFalse(cancelled.requestDispatched)
        assertSame(cancelled, cancelled.arrive(first.name))
    }
}
