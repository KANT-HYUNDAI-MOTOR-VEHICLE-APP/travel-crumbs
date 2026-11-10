package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.guideTrackerOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 추천 안내를 길안내 입구 하나로(#51 U7 · 옛 R31 을 옮김). */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerRouteGuidanceRequesterTest {

    private val dispatcher = StandardTestDispatcher()
    private val destination = GuidanceDestination(1001, "속초 중앙시장 공영주차장", "강원 속초시 중앙로", 38.2049, 128.5906)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `U7-1 R31 입구로 요청 — 내비가 시작을 알리면 시작 · 이름 · 좌표 그대로 · 입구 상태도 시작됨`() = runTest(dispatcher) {
        val navi = FakeNaviRepository()
        val tracker = guideTrackerOf(navi, FakePinRepository())
        val requester = TrackerRouteGuidanceRequester(tracker)

        val answer = async { requester.requestRoute(destination) }
        runCurrent()
        val sent = navi.routeRequests.single()
        assertEquals("reco-1001", sent.id)
        assertEquals(38.2049, sent.latitude, 0.0)
        navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(GuidanceRequestResult.Started, answer.await())
        assertTrue(tracker.state.value is GuideRequestState.Started)
    }

    @Test
    fun `U7-2 R31 내비 오류 · 연결 전 · 다른 요청 중이면 실패`() = runTest(dispatcher) {
        val navi = FakeNaviRepository()
        val tracker = guideTrackerOf(navi, FakePinRepository())
        val requester = TrackerRouteGuidanceRequester(tracker)

        val failed = async { requester.requestRoute(destination) }
        runCurrent()
        navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()
        assertEquals(GuidanceRequestResult.Failed, failed.await())

        tracker.guide(Place("other", "다른 곳", "", 37.0, 127.0))
        assertEquals(GuidanceRequestResult.Failed, requester.requestRoute(destination))

        val offline = guideTrackerOf(FakeNaviRepository(ready = false), FakePinRepository())
        assertEquals(GuidanceRequestResult.Failed, TrackerRouteGuidanceRequester(offline).requestRoute(destination))
    }
}
