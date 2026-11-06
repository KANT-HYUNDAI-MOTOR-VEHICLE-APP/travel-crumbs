package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRequestStatus
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePinnedPlacesStore
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakePlaceStatsRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.testing.testPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** #4 실패 폴백(스펙 navigation 3.2 「실패 폴백」 · 와이어프레임 A-14 · U51 ~ U56) — fake 저장소 · 가상 시간. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRouteFallbackViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        pinned: List<String> = listOf("a", "b"),
        naviReady: Boolean = true,
        /** debug 빌드 흉내(참) · release 흉내(거짓 · T110). */
        private val dummyRoute: Boolean = true,
    ) {
        val driving = FakeDrivingSignalRepository(DrivingState.PARKED)
        val navi = FakeNaviRepository(ready = naviReady)
        val catalog = FakePlaceCatalogRepository()
        val pinnedLocal = FakePinnedPlacesStore(pinned, catalog)
        val pins = FakePinRepository()
        var vm = create()
            private set
        val state: HomeUiState get() = vm.uiState.value

        /** 같은 저장소로 ViewModel 을 새로 만듦 — 프로세스 재생성 흉내(T108 · SavedStateHandle 없음). */
        fun recreate() {
            vm = create()
        }

        private fun create() = HomeViewModel(
            recordingRepository = recordingRepositoryOf(FakeTimeSource()),
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = navi,
            placeCatalogRepository = catalog,
            pinnedPlacesRepository = pinnedLocal.repository,
            pinRepository = pins,
            placeStatsRepository = FakePlaceStatsRepository(),
            demoFeatures = DemoFeatures(dummyRoute = dummyRoute),
        )
    }

    private fun TestScope.start(fixture: Fixture): Fixture {
        subscribe(fixture)
        return fixture
    }

    private fun TestScope.subscribe(fixture: Fixture) {
        val vm = fixture.vm
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
    }

    /** [placeId] 요청 → 내비 오류 [reason] 로 실패. */
    private fun TestScope.failWith(f: Fixture, placeId: String, reason: RouteFailReason = RouteFailReason.ROUTE_NOT_FOUND) {
        f.vm.onGuide(placeId)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(reason))
        runCurrent()
    }

    /** [placeId] 요청 → 길안내 대기 한도 지남 · 대조 불일치 → TIMEOUT. */
    private fun TestScope.timeOut(f: Fixture, placeId: String) {
        f.vm.onGuide(placeId)
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()
    }

    private fun TestScope.settleNaviQuery() {
        advanceTimeBy(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 1)
        runCurrent()
    }

    private fun HomeUiState.assertFallbackShownAndPressable() {
        assertTrue("다시 시도 보임", retryVisible)
        assertTrue("다시 시도 누를 수 있음", retryEnabled)
        assertTrue("더미 경로 보임", dummyRouteVisible)
        assertTrue("더미 경로 누를 수 있음", dummyRouteEnabled)
        assertNull("패널은 누르기 전엔 없음", dummyRoute)
    }

    @Test
    fun `T97 실패 이유 7개 모두 — 다시 시도 · 더미 경로 둘 다 누를 수 있음 · 패널 없음`() = runTest(dispatcher) {
        val f = start(Fixture())
        listOf(
            RouteFailReason.TOO_CLOSE,
            RouteFailReason.TOO_FAR,
            RouteFailReason.LOW_BATTERY,
            RouteFailReason.ROUTE_NOT_FOUND,
        ).forEach { reason ->
            failWith(f, "a", reason)
            assertEquals(RouteRequestState.Failed(reason, "a"), f.state.routeRequest)
            f.state.assertFallbackShownAndPressable()
        }

        timeOut(f, "a")
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        f.state.assertFallbackShownAndPressable()

        f.navi.routeError = IllegalStateException("시험")
        f.vm.onGuide("a")
        runCurrent()
        f.navi.routeError = null
        assertEquals(RouteRequestState.Failed(RouteFailReason.REQUEST_FAILED, "a"), f.state.routeRequest)
        f.state.assertFallbackShownAndPressable()

        // 요청 중 끊김 → NOT_CONNECTED · 다시 연결되면 둘 다 누를 수 있음
        f.vm.onGuide("a")
        runCurrent()
        f.navi.setReady(false)
        runCurrent()
        f.navi.setReady(true)
        runCurrent()
        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "a"), f.state.routeRequest)
        f.state.assertFallbackShownAndPressable()
    }

    @Test
    fun `T98 실패 뒤 다시 시도 → 시작 알림 — 같은 장소 같은 값으로 요청 1건 · 시작됨 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")

        f.vm.onRetry()
        runCurrent()

        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertEquals(2, f.navi.routeRequests.size)
        assertEquals("「여기로 안내」(A)와 같은 값", f.navi.routeRequests[0], f.navi.routeRequests[1])
        assertFalse(f.state.retryVisible)
        assertFalse(f.state.dummyRouteVisible)
        assertFalse(f.state.guideEnabled)

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T99 미연결 실패 — 다시 시도 비활성 · 더미 경로 누를 수 있음 · 다시 시도 호출해도 요청 0건`() = runTest(dispatcher) {
        val f = start(Fixture(naviReady = false))
        f.vm.onGuide("a")
        runCurrent()

        assertFalse(f.state.naviReady)
        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "a"), f.state.routeRequest)
        assertTrue(f.state.retryVisible)
        assertFalse(f.state.retryEnabled)
        assertTrue(f.state.dummyRouteEnabled)

        f.vm.onRetry()
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "a"), f.state.routeRequest)
        assertEquals(0, f.navi.routeRequests.size)
    }

    @Test
    fun `T99 미연결 실패에서 더미 경로를 연 뒤 다시 시도(엇갈림) — 패널 그대로`() = runTest(dispatcher) {
        val f = start(Fixture(naviReady = false))
        f.vm.onGuide("a")
        runCurrent()
        f.vm.onShowDummyRoute()
        runCurrent()

        f.vm.onRetry()
        runCurrent()

        assertEquals(PlaceItem("a", "a 이름", "a 주소"), f.state.dummyRoute)
        assertEquals(0, f.navi.routeRequests.size)
    }

    @Test
    fun `T100 미연결 실패 뒤 연결됨 — 다시 시도 활성 → 요청 중 A · 요청 1건`() = runTest(dispatcher) {
        val f = start(Fixture(naviReady = false))
        f.vm.onGuide("a")
        runCurrent()

        f.navi.setReady(true)
        runCurrent()
        assertTrue(f.state.retryEnabled)

        f.vm.onRetry()
        runCurrent()

        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertEquals(listOf("a"), f.navi.routeRequests.map { it.id })
    }

    @Test
    fun `T101 더미 경로 — 패널에 A 이름 · 주소 · 실패 그대로 · 내비 요청 · 핀 · 저장 쓰기 0건`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")

        f.vm.onShowDummyRoute()
        runCurrent()

        assertEquals(PlaceItem("a", "a 이름", "a 주소"), f.state.dummyRoute)
        assertEquals(RouteRequestState.Failed(RouteFailReason.ROUTE_NOT_FOUND, "a"), f.state.routeRequest)
        assertFalse("열린 동안 더미 경로 비활성", f.state.dummyRouteEnabled)
        assertTrue(f.state.retryEnabled)
        assertEquals("처음 요청 1건뿐", 1, f.navi.routeRequests.size)
        assertTrue("핀 0건 · 서버 POST 0건(핀이 없으면 보낼 것이 없음)", f.pins.recorded.isEmpty())
        assertEquals("기기 저장 쓰기 0건", 0, f.pinnedLocal.writes)

        // 다시 눌러도(엇갈림) 그대로
        f.vm.onShowDummyRoute()
        runCurrent()
        assertEquals(PlaceItem("a", "a 이름", "a 주소"), f.state.dummyRoute)
    }

    @Test
    fun `T102 닫기 — 패널 닫힘 · 결과 문구 · 두 버튼 그대로 · 다시 열 수 있음`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        f.vm.onCloseDummyRoute()
        runCurrent()

        assertNull(f.state.dummyRoute)
        assertEquals(RouteRequestState.Failed(RouteFailReason.ROUTE_NOT_FOUND, "a"), f.state.routeRequest)
        f.state.assertFallbackShownAndPressable()

        f.vm.onShowDummyRoute()
        runCurrent()
        assertEquals("a", f.state.dummyRoute?.id)
    }

    @Test
    fun `T103 패널 열림 · 주행 → 다시 정차 — 패널 닫힌 채 · 실패 문구 · 두 버튼 남음`() = runTest(dispatcher) {
        listOf(DrivingState.DRIVING, DrivingState.UNKNOWN).forEach { notParked ->
            val f = start(Fixture())
            failWith(f, "a")
            f.vm.onShowDummyRoute()
            runCurrent()

            f.driving.emit(notParked)
            runCurrent()
            assertFalse(f.state.placesVisible)
            assertNull(f.state.dummyRoute)

            f.driving.emit(DrivingState.PARKED)
            runCurrent()
            assertTrue(f.state.placesVisible)
            assertNull("$notParked 뒤 정차로 돌아와도 닫힌 채", f.state.dummyRoute)
            assertEquals(RouteRequestState.Failed(RouteFailReason.ROUTE_NOT_FOUND, "a"), f.state.routeRequest)
            f.state.assertFallbackShownAndPressable()
        }
    }

    @Test
    fun `T103 홈 구독이 멈춘 동안(주행 화면) 주행이 와도 패널 닫힘`() = runTest(dispatcher) {
        val f = Fixture()
        val subscription = backgroundScope.launch { f.vm.uiState.collect { } }
        runCurrent()
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        // 주행이면 앱 틀이 주행 전용 화면을 그려 홈 uiState 구독이 끝남
        subscription.cancel()
        advanceTimeBy(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS + 1)
        f.driving.emit(DrivingState.DRIVING)
        runCurrent()
        f.driving.emit(DrivingState.PARKED)
        runCurrent()

        subscribe(f)
        assertNull(f.state.dummyRoute)
        assertTrue(f.state.retryVisible)
    }

    @Test
    fun `T104 주행 · 모름 — 다시 시도 · 더미 경로 · 닫기 호출 무시 · 요청 0건`() = runTest(dispatcher) {
        listOf(DrivingState.DRIVING, DrivingState.UNKNOWN).forEach { notParked ->
            val f = start(Fixture())
            failWith(f, "a")
            f.driving.emit(notParked)
            runCurrent()

            f.vm.onRetry()
            f.vm.onShowDummyRoute()
            f.vm.onCloseDummyRoute()
            runCurrent()

            assertEquals(RouteRequestState.Failed(RouteFailReason.ROUTE_NOT_FOUND, "a"), f.state.routeRequest)
            assertNull(f.state.dummyRoute)
            assertEquals("$notParked — 처음 요청 1건뿐", 1, f.navi.routeRequests.size)
        }
    }

    @Test
    fun `T105 요청 중 — 폴백 없음 · 다시 시도 · 더미 경로 호출 무시 · 고정 · 고정 해제는 됨(I-04)`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onGuide("b")
        runCurrent()

        f.vm.onRetry()
        f.vm.onShowDummyRoute()
        runCurrent()

        assertEquals(RouteRequestState.Requesting("b"), f.state.routeRequest)
        assertEquals(listOf("a", "b"), f.navi.routeRequests.map { it.id })
        assertFalse(f.state.guideEnabled)
        assertFalse(f.state.retryVisible)
        assertFalse(f.state.dummyRouteVisible)
        assertNull(f.state.dummyRoute)

        f.vm.onPin("c")
        f.vm.onUnpin("a")
        runCurrent()
        assertEquals(listOf("b", "c"), f.state.pinnedPlaces?.map { it.id })
    }

    @Test
    fun `T106 TIMEOUT · 패널 열림 → 재개 대조 일치 — 시작됨 A · 패널 닫힘 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture())
        timeOut(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()
        assertEquals("a", f.state.dummyRoute?.id)

        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertNull(f.state.dummyRoute)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T106 TIMEOUT · 패널 열림 → 늦은 onRouteStarted — 시작됨 A · 패널 닫힘 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture())
        timeOut(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        f.navi.guidingTo(testPlace("a"))
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        settleNaviQuery()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertNull(f.state.dummyRoute)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T107 실패 A · 패널 열림 → B 여기로 안내 — 요청 중 B · 패널 닫힘 · 요청 1건(B)`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        f.vm.onGuide("b")
        runCurrent()

        assertEquals(RouteRequestState.Requesting("b"), f.state.routeRequest)
        assertNull(f.state.dummyRoute)
        assertEquals("b", f.navi.routeRequests.last().id)
        assertEquals(2, f.navi.routeRequests.size)
    }

    @Test
    fun `T107 보충 실패 A · 패널 열림 → B 여기로 안내가 미연결로 실패 — 실패 B · 패널 닫힘(장소가 바뀜)`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()
        f.navi.setReady(false)
        runCurrent()

        f.vm.onGuide("b")
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "b"), f.state.routeRequest)
        assertNull(f.state.dummyRoute)
    }

    @Test
    fun `T108 실패 A · 패널 열림 → 프로세스 재생성 — 대기 · 패널 없음 · 고정 목록 그대로`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        f.recreate()
        subscribe(f)

        assertEquals(RouteRequestState.Idle, f.state.routeRequest)
        assertNull(f.state.dummyRoute)
        assertFalse(f.state.retryVisible)
        assertEquals(listOf("a", "b"), f.state.pinnedPlaces?.map { it.id })
    }

    @Test
    fun `T110 release 빌드 — 다시 시도만 · 더미 경로 없음 · 호출해도 패널 없음`() = runTest(dispatcher) {
        val f = start(Fixture(dummyRoute = false))
        failWith(f, "a")

        assertFalse(f.state.dummyRouteAvailable)
        assertTrue(f.state.retryVisible)
        assertTrue(f.state.retryEnabled)
        assertFalse(f.state.dummyRouteVisible)
        assertFalse(f.state.dummyRouteEnabled)

        f.vm.onShowDummyRoute()
        runCurrent()
        assertNull(f.state.dummyRoute)
    }

    @Test
    fun `T112 패널 열림 · A 고정 해제 → 다시 시도 — 패널 그대로였다가 요청 중 A · 요청 1건(A)`() = runTest(dispatcher) {
        val f = start(Fixture())
        failWith(f, "a")
        f.vm.onShowDummyRoute()
        runCurrent()

        f.vm.onUnpin("a")
        runCurrent()
        assertEquals(listOf("b"), f.state.pinnedPlaces?.map { it.id })
        assertEquals(PlaceItem("a", "a 이름", "a 주소"), f.state.dummyRoute)
        assertTrue(f.state.retryEnabled)

        f.vm.onRetry()
        runCurrent()

        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertEquals(listOf("a", "a"), f.navi.routeRequests.map { it.id })
        assertNull(f.state.dummyRoute)
    }

    @Test
    fun `T114 요청 상태 ↔ NAVI_REQ_STATUS 대응 — 대기 없음 · 요청 중 REQUESTING · 시작됨 SUCCESS · 실패 FAILED`() {
        assertNull(RouteRequestState.Idle.status)
        assertEquals(NaviRequestStatus.REQUESTING, RouteRequestState.Requesting("a").status)
        assertEquals(NaviRequestStatus.SUCCESS, RouteRequestState.Started("a 이름").status)
        RouteFailReason.entries.forEach { reason ->
            assertEquals(NaviRequestStatus.FAILED, RouteRequestState.Failed(reason, "a").status)
        }
    }
}
