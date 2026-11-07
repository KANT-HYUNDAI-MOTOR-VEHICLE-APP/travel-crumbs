package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.testPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 길안내 요청 입구 단위 시험 — 화면 없이 장소 하나로 부름(음성 · 「다시 가기」 등이 같은 입구를 쓸 수 있는지).
 * 화면 쪽 의미(정차일 때만 · 문구)는 HomePlacesViewModelTest · HomeRouteFallbackViewModelTest 가 그대로 지킴.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuideRequestTrackerTest {

    private val a = testPlace("a")
    private val b = testPlace("b")

    private class Fixture(scope: TestScope, naviReady: Boolean = true) {
        val navi = FakeNaviRepository(ready = naviReady)
        val pins = FakePinRepository()
        val tracker = GuideRequestTracker(navi, pins, scope.backgroundScope)
    }

    @Test
    fun `T6 T20 입구만으로 요청 → 시작 알림 — 시작됨 A · 핀 1건 · 화면 · 주행 판정 없이`() = runTest {
        val f = Fixture(this)

        f.tracker.guide(a)
        runCurrent()
        assertEquals(GuideRequestState.Requesting(a), f.tracker.state.value)
        assertTrue(f.tracker.isRequesting)

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(GuideRequestState.Started(a), f.tracker.state.value)
        assertEquals(listOf("a"), f.pins.recorded)
        assertFalse(f.tracker.isRequesting)
    }

    @Test
    fun `T5 요청 중에 다른 장소 — 무시 · 내비 요청 1건`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(a)
        runCurrent()

        f.tracker.guide(b)
        runCurrent()

        assertEquals(GuideRequestState.Requesting(a), f.tracker.state.value)
        assertEquals(1, f.navi.routeRequests.size)
    }

    @Test
    fun `T14 한도 지남 · 대조 불일치 — 실패 TIMEOUT · 구독자가 없어도 입구 혼자 끝냄`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(a)
        runCurrent()

        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 2)
        runCurrent()

        val failed = f.tracker.state.value as GuideRequestState.Failed
        assertEquals(RouteFailReason.TIMEOUT, failed.reason)
        assertEquals(a, failed.place)
    }

    @Test
    fun `T99 T107 시도 번호 — 보낸 요청마다 늘고 · 같은 장소 즉시 미연결은 이어받고 · 다른 장소는 새 번호`() = runTest {
        val f = Fixture(this)
        f.tracker.guide(a)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()
        val first = (f.tracker.state.value as GuideRequestState.Failed).attempt

        f.navi.setReady(false)
        f.tracker.retry()
        val sameRetry = f.tracker.state.value as GuideRequestState.Failed
        assertEquals(RouteFailReason.NOT_CONNECTED, sameRetry.reason)
        assertEquals("같은 장소 즉시 실패는 같은 번호(더미 경로 패널 유지 · T99)", first, sameRetry.attempt)

        f.tracker.guide(b)
        val other = f.tracker.state.value as GuideRequestState.Failed
        assertNotEquals("다른 장소는 새 번호(패널 닫힘 · T107 보충)", first, other.attempt)

        f.tracker.guide(a)
        assertNotEquals("B 실패 뒤 A 즉시 실패도 새 번호", first, (f.tracker.state.value as GuideRequestState.Failed).attempt)

        f.navi.setReady(true)
        f.tracker.retry()
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.TOO_FAR))
        runCurrent()
        val sent = f.tracker.state.value as GuideRequestState.Failed
        assertTrue("보낸 요청은 늘 새 번호", sent.attempt > other.attempt)
        assertEquals(0, f.pins.recorded.size)
    }

    @Test
    fun `T104 T105 retry 는 실패일 때만 — 대기 · 요청 중이면 무시`() = runTest {
        val f = Fixture(this)
        f.tracker.retry()
        assertEquals(GuideRequestState.Idle, f.tracker.state.value)

        f.tracker.guide(a)
        runCurrent()
        f.tracker.retry()
        runCurrent()
        assertEquals(1, f.navi.routeRequests.size)
    }
}
