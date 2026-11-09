package com.example.travelcrumbs.data.navi

import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.FakeNaviRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 길안내 어댑터(#41 리뷰) — #4 NaviRepository.requestRoute 를 감쌈. */
class NaviRouteGuidanceRequesterTest {

    private val destination = GuidanceDestination(1001, "속초 중앙시장 공영주차장", "강원 속초시 중앙로", 38.2049, 128.5906)

    @Test
    fun `R31 내비가 시작을 알리면 시작 · 오류면 실패 · 장소 이름 · 좌표는 그대로 내비로`() = runTest {
        val navi = FakeNaviRepository()
        val requester = NaviRouteGuidanceRequester(navi)

        val started = async { requester.requestRoute(destination) }
        runCurrent()
        navi.answerRoute(RouteRequestResult.Started)
        assertEquals(GuidanceRequestResult.Started, started.await())
        val sent = navi.routeRequests.single()
        assertEquals("속초 중앙시장 공영주차장", sent.name)
        assertEquals(38.2049, sent.latitude, 0.0)

        val failed = async { requester.requestRoute(destination) }
        runCurrent()
        navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        assertEquals(GuidanceRequestResult.Failed, failed.await())
    }

    @Test
    fun `R31 내비 연결 전이면 요청 없이 실패`() = runTest {
        val navi = FakeNaviRepository(ready = false)
        val requester = NaviRouteGuidanceRequester(navi)

        assertEquals(GuidanceRequestResult.Failed, requester.requestRoute(destination))
        assertTrue(navi.routeRequests.isEmpty())
    }
}
