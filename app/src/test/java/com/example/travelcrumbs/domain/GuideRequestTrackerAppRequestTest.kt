package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.testPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 길안내 요청 입구의 읽기 전용 질의 「이 앱 요청인가」(#66 3.6 · 3.8) — 상태 흐름 모양 · 동작은 그대로. */
@OptIn(ExperimentalCoroutinesApi::class)
class GuideRequestTrackerAppRequestTest {

    private val x = testPlace("x")
    private val dx = NaviDestination(x.name, x.address, x.latitude, x.longitude)

    private class Fixture(scope: TestScope) {
        val time = FakeTimeSource(now = 1_000_000L)
        val navi = FakeNaviRepository()
        val pins = FakePinRepository()
        val tracker = GuideRequestTracker(navi, pins, scope.backgroundScope, time)
    }

    @Test
    fun `T27 T47b 요청 중(내부 칸 X) — 이 앱 요청`() = runTest {
        val f = Fixture(this)
        assertFalse(f.tracker.isAppRequest(dx))
        f.tracker.guide(x)
        runCurrent()
        assertTrue(f.tracker.isAppRequest(dx))
    }

    @Test
    fun `T47a T54 T53 시작됨 직후(20초)는 이 앱 요청 · 3시간 뒤는 아님 · 상태 흐름은 시작됨 그대로`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(x)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        assertTrue(f.tracker.isAppRequest(dx))
        f.time.now += 20_000
        assertTrue(f.tracker.isAppRequest(dx))
        f.time.now += 3 * 60 * 60_000L
        assertFalse(f.tracker.isAppRequest(dx))
        assertEquals(x, (f.tracker.state.value as GuideRequestState.Started).place)
    }

    @Test
    fun `T55 시작됨 뒤 정확히 60초 = 앱 요청 · 60초 + 1밀리초 = 직접`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(x)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        f.time.now += AppConfig.AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS
        assertTrue(f.tracker.isAppRequest(dx))
        f.time.now += 1
        assertFalse(f.tracker.isAppRequest(dx))
    }

    @Test
    fun `T56 T57 TIMEOUT 뒤 기억 중(3분)은 앱 요청 · 기억 기한(5분)이 지나면 아님 · 상태는 실패(TIMEOUT · X)`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(x)
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 2)
        runCurrent()
        assertEquals(RouteFailReason.TIMEOUT, (f.tracker.state.value as GuideRequestState.Failed).reason)
        advanceTimeBy(3 * 60_000L)
        runCurrent()
        assertTrue(f.tracker.isAppRequest(dx))
        advanceTimeBy(AppConfig.TIMED_OUT_REQUEST_MEMORY_MILLIS)
        runCurrent()
        assertFalse(f.tracker.isAppRequest(dx))
        assertEquals(RouteFailReason.TIMEOUT, (f.tracker.state.value as GuideRequestState.Failed).reason)
    }

    @Test
    fun `T58 일반 실패(ROUTE_NOT_FOUND · 내부 칸 비움) — 아님`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(x)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()
        assertFalse(f.tracker.isAppRequest(dx))
    }

    @Test
    fun `T29 시작됨(X) 10초 뒤 1 km 떨어진 B — 아님`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(x)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        f.time.now += 10_000
        assertFalse(f.tracker.isAppRequest(NaviDestination("B", null, x.latitude + 0.009, x.longitude)))
    }
}
