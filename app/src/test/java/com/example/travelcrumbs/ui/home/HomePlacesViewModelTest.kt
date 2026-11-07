package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.combineDrivingState
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Gear
import com.example.travelcrumbs.domain.model.PlaceStat
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.UxRestriction
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePinnedPlacesStore
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakePlaceStatsRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.guideTrackerOf
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.testing.testPlace
import kotlinx.coroutines.CompletableDeferred
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

/** #4 「가 볼 장소」 · 「장소 고르기」 · 길안내 요청(스펙 navigation 3.1 · 3.2) — fake 저장소 · 가상 시간. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomePlacesViewModelTest {

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
        pinned: List<String> = emptyList(),
        driving: DrivingState = DrivingState.PARKED,
        naviReady: Boolean = true,
        /** debug 빌드 흉내(참) · release 흉내(거짓 · T110). */
        dummyRoute: Boolean = true,
    ) {
        val driving = FakeDrivingSignalRepository(driving)
        val navi = FakeNaviRepository(ready = naviReady)
        val catalog = FakePlaceCatalogRepository()
        val pinnedLocal = FakePinnedPlacesStore(pinned, catalog)
        val pins = FakePinRepository()
        val stats = FakePlaceStatsRepository()
        val vm = HomeViewModel(
            recordingRepository = recordingRepositoryOf(FakeTimeSource()),
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = this.driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = navi,
            placeCatalogRepository = catalog,
            pinnedPlacesRepository = pinnedLocal.repository,
            guideRequests = guideTrackerOf(navi, pins),
            placeStatsRepository = stats,
            demoFeatures = DemoFeatures(dummyRoute = dummyRoute),
        )
        val state: HomeUiState get() = vm.uiState.value
    }

    private fun TestScope.start(fixture: Fixture): Fixture {
        backgroundScope.launch { fixture.vm.uiState.collect { } }
        runCurrent()
        return fixture
    }

    /** 대조 조회 전 기다림까지 지남(R1). */
    private fun TestScope.settleNaviQuery() {
        advanceTimeBy(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 1)
        runCurrent()
    }

    private fun HomeUiState.pinnedIds() = pinnedPlaces?.map { it.id }

    @Test
    fun `T1 정차 · 연결됨 · 고정 3곳 — A B C 순서 · 이름 주소 · 결과 문구 없음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b", "c")))

        assertTrue(f.state.placesVisible)
        assertEquals(listOf("a", "b", "c"), f.state.pinnedIds())
        assertEquals(PlaceItem("a", "a 이름", "a 주소"), f.state.pinnedPlaces?.first())
        assertEquals(RouteRequestState.Idle, f.state.routeRequest)
        assertTrue(f.state.naviReady)
        assertTrue(f.state.guideEnabled)
        assertEquals(5, f.state.candidates.size)
    }

    @Test
    fun `T4 연결됨 · 대기 → A 터치 — 요청 중 A · 모든 버튼 비활성 · 직전 결과 지움`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.TOO_FAR))
        runCurrent()

        f.vm.onGuide("a")
        runCurrent()

        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertFalse(f.state.guideEnabled)
    }

    @Test
    fun `T5 요청 중 A 에 B 터치 — 요청 중 A 그대로 · 내비 요청 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()

        f.vm.onGuide("b")
        runCurrent()

        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertEquals(1, f.navi.routeRequests.size)
    }

    @Test
    fun `T6 요청 중 A 에 시작 알림 — 시작됨 A 이름`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertTrue(f.state.guideEnabled)
    }

    @Test
    fun `T7 내비 연결 안 됨 — 연결 안 됨 · 버튼 비활성`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a"), naviReady = false))

        assertFalse(f.state.naviReady)
        assertFalse(f.state.guideEnabled)
    }

    @Test
    fun `T8 연결 안 됨 → 연결됨 — 버튼 누를 수 있음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a"), naviReady = false))

        f.navi.setReady(true)
        runCurrent()

        assertTrue(f.state.naviReady)
        assertTrue(f.state.guideEnabled)
    }

    @Test
    fun `T9 연결 안 됨인데 메서드 호출 — 실패 NOT_CONNECTED · 내비 요청 0건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a"), naviReady = false))

        f.vm.onGuide("a")
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "a"), f.state.routeRequest)
        assertEquals(0, f.navi.routeRequests.size)
    }

    @Test
    fun `T10 T11 T12 T13 내비 오류 이유가 그대로 실패 상태로`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        listOf(
            RouteFailReason.TOO_CLOSE,
            RouteFailReason.TOO_FAR,
            RouteFailReason.LOW_BATTERY,
            RouteFailReason.ROUTE_NOT_FOUND,
        ).forEach { reason ->
            f.vm.onGuide("a")
            runCurrent()
            f.navi.answerRoute(RouteRequestResult.Failed(reason))
            runCurrent()
            assertEquals(RouteRequestState.Failed(reason, "a"), f.state.routeRequest)
        }
    }

    @Test
    fun `T14 응답 대기 한도 안에 답 없음 — 실패 TIMEOUT · 버튼 다시 누를 수 있음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)

        advanceTimeBy(2)
        runCurrent()
        settleNaviQuery() // R1 — 한도 뒤 내비 상태 대조(안내 중 아님)
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.state.guideEnabled)
    }

    @Test
    fun `T15 요청 호출이 예외 — 실패 REQUEST_FAILED · 앱 꺼지지 않음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.navi.routeError = SecurityException("권한 없음(시험)")

        f.vm.onGuide("a")
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.REQUEST_FAILED, "a"), f.state.routeRequest)
    }

    @Test
    fun `T16 요청 중 내비 연결 끊김 — 실패 NOT_CONNECTED`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        f.navi.setReady(false)
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED, "a"), f.state.routeRequest)
    }

    @Test
    fun `T17 실패 뒤 B 시작 — 결과 문구가 B 시작으로`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()

        f.vm.onGuide("b")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(RouteRequestState.Started("b 이름"), f.state.routeRequest)
    }

    @Test
    fun `T20 주행 중 메서드 호출 — 대기 그대로 · 내비 요청 0 · 핀 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a"), driving = DrivingState.DRIVING))

        f.vm.onGuide("a")
        runCurrent()

        assertEquals(RouteRequestState.Idle, f.state.routeRequest)
        assertEquals(0, f.navi.routeRequests.size)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T21 모름 — 구역 없음 · 메서드 호출에도 내비 요청 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a"), driving = DrivingState.UNKNOWN))

        f.vm.onGuide("a")
        runCurrent()

        assertFalse(f.state.placesVisible)
        assertEquals(0, f.navi.routeRequests.size)
    }

    @Test
    fun `T22 시작됨 뒤 정차 → 주행 → 정차 — 고정 목록 · 결과 · 버튼 그대로`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        val before = f.state

        f.driving.emit(DrivingState.DRIVING)
        runCurrent()
        assertFalse(f.state.placesVisible)
        f.driving.emit(DrivingState.PARKED)
        runCurrent()

        assertEquals(before.pinnedPlaces, f.state.pinnedPlaces)
        assertEquals(before.routeRequest, f.state.routeRequest)
        assertEquals(before.guideEnabled, f.state.guideEnabled)
        assertTrue(f.state.placesVisible)
    }

    @Test
    fun `T30 정차 · 고정 0곳 — 빈 목록(빈 안내) · 후보 전부 고정 안 됨`() = runTest(dispatcher) {
        val f = start(Fixture())

        assertEquals(emptyList<PlaceItem>(), f.state.pinnedPlaces)
        assertTrue(f.state.candidates.none { it.pinned })
        assertEquals(5, f.state.candidates.size)
    }

    @Test
    fun `T31 주행 · 고정 0곳 — 구역 없음`() = runTest(dispatcher) {
        val f = start(Fixture(driving = DrivingState.DRIVING))

        assertFalse(f.state.placesVisible)
    }

    @Test
    fun `T32 고정 목록 읽기 전 — 목록 · 빈 안내 둘 다 없음 → 읽은 뒤 목록`() = runTest(dispatcher) {
        val fixture = Fixture(pinned = listOf("b"))
        fixture.pinnedLocal.readable.value = false
        val f = start(fixture)

        assertNull(f.state.pinnedPlaces)

        f.pinnedLocal.readable.value = true
        runCurrent()
        assertEquals(listOf("b"), f.state.pinnedIds())
    }

    @Test
    fun `T32 후보 읽기 전에도 목록 · 빈 안내 둘 다 없음`() = runTest(dispatcher) {
        val fixture = Fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.catalog.gate = gate
        val f = start(fixture)

        assertNull(f.state.pinnedPlaces)

        gate.complete(Unit)
        runCurrent()
        assertEquals(emptyList<PlaceItem>(), f.state.pinnedPlaces)
    }

    @Test
    fun `T33 고정 0곳에서 A 고정 — 가 볼 장소에 A · 후보 A 는 고정됨 · 기기 저장에 A`() = runTest(dispatcher) {
        val f = start(Fixture())

        f.vm.onPin("a")
        runCurrent()

        assertEquals(listOf("a"), f.state.pinnedIds())
        assertTrue(f.state.candidates.first { it.id == "a" }.pinned)
        assertEquals(listOf("a"), f.pinnedLocal.current)
    }

    @Test
    fun `T34 고정 A B 에 C 고정 — 순서 A B C`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))

        f.vm.onPin("c")
        runCurrent()

        assertEquals(listOf("a", "b", "c"), f.state.pinnedIds())
    }

    @Test
    fun `T35 고정 수 최대 — 한도 안내 · 고정 비활성(고정 해제는 됨)`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b", "c")))

        assertFalse(f.state.canPinMore)
        assertEquals(AppConfig.PINNED_PLACES_MAX, f.state.pinnedPlaces?.size)
    }

    @Test
    fun `T36 최대에서 고정 메서드 호출 — 목록 그대로 · 저장 쓰기 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b", "c")))

        f.vm.onPin("d")
        runCurrent()

        assertEquals(listOf("a", "b", "c"), f.state.pinnedIds())
        assertEquals(0, f.pinnedLocal.writes)
    }

    @Test
    fun `T37 A B C 에서 B 고정 해제 — A C 순서 · 한도 안내 사라짐 · 핀 그대로`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b", "c")))
        f.pins.recordGuidanceStart("b")

        f.vm.onUnpin("b")
        runCurrent()

        assertEquals(listOf("a", "c"), f.state.pinnedIds())
        assertTrue(f.state.canPinMore)
        assertFalse(f.state.candidates.first { it.id == "b" }.pinned)
        assertEquals(listOf("b"), f.pins.recorded)
    }

    @Test
    fun `T39 주행 · 모름에서 고정 · 해제 — 목록 그대로 · 저장 쓰기 0`() = runTest(dispatcher) {
        listOf(DrivingState.DRIVING, DrivingState.UNKNOWN).forEach { driving ->
            val f = start(Fixture(pinned = listOf("a"), driving = driving))

            f.vm.onPin("b")
            f.vm.onUnpin("a")
            runCurrent()

            assertEquals(listOf("a"), f.pinnedLocal.current)
            assertEquals(0, f.pinnedLocal.writes)
        }
    }

    @Test
    fun `T40 장소 고르기 보는 중 기어 D — 구역 없음 · 고정 목록 그대로 · P 로 돌아오면 다시`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))

        f.driving.emit(DrivingState.DRIVING)
        runCurrent()
        assertFalse(f.state.placesVisible)
        assertEquals(listOf("a"), f.state.pinnedIds())

        f.driving.emit(DrivingState.PARKED)
        runCurrent()
        assertTrue(f.state.placesVisible)
    }

    @Test
    fun `T41 저장된 id 하나가 후보에 없음 — 보이지 않고 세지 않음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("zzz", "a", "b")))

        assertEquals(listOf("a", "b"), f.state.pinnedIds())
        assertTrue(f.state.canPinMore)

        f.vm.onPin("c")
        runCurrent()
        assertEquals(listOf("a", "b", "c"), f.state.pinnedIds())
    }

    @Test
    fun `T42 요청 중 A 를 고정 해제 → 시작 알림 — 요청 그대로 · A 이름으로 결과 · 목록에 A 없음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()

        f.vm.onUnpin("a")
        runCurrent()
        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("b"), f.state.pinnedIds())
    }

    @Test
    fun `T43 시작 알림이면 핀 저장소에 그 장소 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()

        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T44 오류 · 응답 없음 · 연결 끊김 · 호출 예외 — 핀 0건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))

        f.vm.onGuide("a")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()

        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        f.vm.onGuide("a")
        runCurrent()
        f.navi.setReady(false)
        runCurrent()
        f.navi.setReady(true)
        runCurrent()

        f.navi.routeError = IllegalStateException("시험")
        f.vm.onGuide("a")
        runCurrent()

        assertEquals(RouteRequestState.Failed(RouteFailReason.REQUEST_FAILED, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T47 기기 저장 쓰기 실패 — 고정 목록 · 화면 그대로 · 앱 꺼지지 않음`() = runTest(dispatcher) {
        val f = start(Fixture())
        f.pinnedLocal.failWrites = true

        f.vm.onPin("a")
        runCurrent()

        assertEquals(emptyList<PlaceItem>(), f.state.pinnedPlaces)
        assertTrue(f.state.candidates.none { it.pinned })
    }

    @Test
    fun `T67 요청 중 정차 → 주행 · 시작 알림 — 시작됨 · 정차로 돌아오면 결과 그대로`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        f.driving.emit(DrivingState.DRIVING)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)

        f.driving.emit(DrivingState.PARKED)
        runCurrent()
        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T72 기어 P + 규제 있음 · 기어 D + 규제 없음 · 기어 모름 — 셋 다 구역 없음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        listOf(
            combineDrivingState(Gear.PARK, UxRestriction.RESTRICTED),
            combineDrivingState(Gear.DRIVE, UxRestriction.UNRESTRICTED),
            combineDrivingState(Gear.UNKNOWN, UxRestriction.UNRESTRICTED),
        ).forEach { judged ->
            f.driving.emit(judged)
            runCurrent()
            assertFalse("판정 $judged", f.state.placesVisible)
        }
    }

    @Test
    fun `T85 핀과 고정 목록은 별개 — 고정 해제가 핀을 지우지 않고 핀이 고정 목록을 바꾸지 않음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.pins.recordGuidanceStart("a")

        f.vm.onUnpin("a")
        runCurrent()
        assertEquals(listOf("a"), f.pins.recorded)

        f.vm.onPin("a")
        runCurrent()
        f.vm.onGuide("b")
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        assertEquals(listOf("b", "a"), f.state.pinnedIds())
        assertEquals(listOf("a", "b"), f.pins.recorded)
    }

    @Test
    fun `U41 debug 카드 안내 건수 — 후보 이름으로 · 조회 실패면 실패 상태`() = runTest(dispatcher) {
        val f = start(Fixture())
        f.stats.result = Result.success(listOf(PlaceStat("a", 12, 3), PlaceStat("zzz", 1, 1)))

        f.vm.refreshPlaceStats()
        runCurrent()
        assertEquals(
            SdkStatus.Ready(listOf(PlaceStatItem("a 이름", 12), PlaceStatItem("zzz", 1))),
            f.state.placeStats,
        )

        f.stats.result = Result.failure(IllegalStateException("HTTP_500"))
        f.vm.refreshPlaceStats()
        runCurrent()
        assertTrue(f.state.placeStats is SdkStatus.Failed)
    }

    @Test
    fun `T89 R1 한도 지남 · 내비가 그 장소로 안내 중 — 시작됨 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.guidingTo(testPlace("a"))

        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T90 R1 한도 지남 · 내비 목적지가 다른 곳 또는 안내 중 아님 — TIMEOUT · 핀 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.guidingTo(testPlace("b"))
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)

        f.vm.onGuide("a")
        runCurrent()
        f.navi.activeRoute = f.navi.activeRoute.copy(routing = false, destinationName = "a 이름")
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T92 R1 재개 대조로 시작됨 뒤 늦은 시작 알림 · 또 대조 — 결과 1번 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.guidingTo(testPlace("a"))

        f.vm.onScreenResumed()
        settleNaviQuery()
        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        f.navi.answerRoute(RouteRequestResult.Started)
        f.vm.onScreenResumed()
        settleNaviQuery()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T93 R1 시작 알림이 먼저 오고 재개가 뒤 — 핀 1건 · 대조 안 함`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.guidingTo(testPlace("a"))

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(listOf("a"), f.pins.recorded)
        assertEquals(0, f.navi.activeRouteQueries)
    }

    @Test
    fun `T92 R1 재개 대조가 기다리는 사이 늦은 시작 알림 — 한 번만 정해짐 · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        f.navi.guidingTo(testPlace("a"))

        f.vm.onScreenResumed()
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        settleNaviQuery()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T95 R1 재개 때 대조 — 한도 전 안내 중 아니면 요청 중 그대로 · 안내 중이면 시작됨`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()

        f.vm.onScreenResumed()
        settleNaviQuery()
        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)
        assertFalse(f.state.guideEnabled)

        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()
        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
        assertTrue(f.state.guideEnabled)
    }

    @Test
    fun `R1 요청이 없을 때 재개 — 대조하지 않음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))

        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(0, f.navi.activeRouteQueries)
        assertEquals(RouteRequestState.Idle, f.state.routeRequest)
    }

    @Test
    fun `T90 R1 한도 지남 · 내비 조회 예외 — TIMEOUT · 핀 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.navi.guidingTo(testPlace("a"))
        f.navi.activeRouteError = IllegalStateException("CAUSE_SEARCH_ROUTE")
        f.vm.onGuide("a")
        runCurrent()

        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T90 R1 한도 지남 · 내비 조회가 답하지 않음 — 조회 한도 뒤 TIMEOUT · 핀 0`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.navi.guidingTo(testPlace("a"))
        f.navi.activeRouteGate = CompletableDeferred()
        f.vm.onGuide("a")
        runCurrent()

        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()
        assertEquals(RouteRequestState.Requesting("a"), f.state.routeRequest)

        advanceTimeBy(AppConfig.SDK_CALL_TIMEOUT_MS + 1)
        runCurrent()
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T91 R1 TIMEOUT 뒤 내비가 A 안내를 시작 · 앱이 다시 앞으로 옴 — 시작됨 A · 핀 1건`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.state.guideEnabled)

        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)
        assertEquals(listOf("a"), f.pins.recorded)
    }

    @Test
    fun `T91 R1 TIMEOUT 뒤 늦은 onRouteStarted — 시작됨 A · 핀 1건 · 뒤 재개 대조로 다시 정하지 않음`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        f.navi.guidingTo(testPlace("a"))
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        settleNaviQuery() // 늦은 알림도 목적지 대조(심사 N1)
        assertEquals(RouteRequestState.Started("a 이름"), f.state.routeRequest)

        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(listOf("a"), f.pins.recorded)
        assertEquals("한도 때 1 · 늦은 알림 때 1 · 정해진 뒤 재개엔 조회 없음", 2, f.navi.activeRouteQueries)
    }

    @Test
    fun `T94 R1 TIMEOUT A 뒤 B 를 새로 요청 — A 는 대조하지 않고 늦은 A 알림도 무시 · B 흐름만`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        f.vm.onGuide("b")
        runCurrent()
        assertEquals(RouteRequestState.Requesting("b"), f.state.routeRequest)
        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()

        assertEquals(RouteRequestState.Requesting("b"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())

        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        assertEquals(RouteRequestState.Started("b 이름"), f.state.routeRequest)
        assertEquals(listOf("b"), f.pins.recorded)
        assertEquals(listOf("a", "b"), f.navi.routeRequests.map { it.id })
    }

    @Test
    fun `T91 N1 TIMEOUT A 뒤 내비가 B 로 시작 알림 — 시작됨 아님 · 핀 0 · 기억도 지움`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a", "b")))
        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()

        f.navi.guidingTo(testPlace("b"))
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        settleNaviQuery()
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())

        val queries = f.navi.activeRouteQueries
        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()
        assertEquals("기억을 지웠으니 재개 대조 없음", queries, f.navi.activeRouteQueries)
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T91 N1 TIMEOUT 뒤 기억 기한 지남 — 재개 때 조회 안 함 · 늦은 알림도 무시`() = runTest(dispatcher) {
        val f = start(Fixture(pinned = listOf("a")))
        f.vm.onGuide("a")
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + 1)
        runCurrent()
        settleNaviQuery()
        val queries = f.navi.activeRouteQueries

        advanceTimeBy(AppConfig.TIMED_OUT_REQUEST_MEMORY_MILLIS + 1)
        runCurrent()
        f.navi.guidingTo(testPlace("a"))
        f.vm.onScreenResumed()
        settleNaviQuery()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        settleNaviQuery()

        assertEquals(queries, f.navi.activeRouteQueries)
        assertEquals(RouteRequestState.Failed(RouteFailReason.TIMEOUT, "a"), f.state.routeRequest)
        assertTrue(f.pins.recorded.isEmpty())
        assertTrue(f.state.guideEnabled)
    }
}
