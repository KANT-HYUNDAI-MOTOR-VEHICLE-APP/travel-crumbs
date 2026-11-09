package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.ui.components.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        recording: RecordingRepository = recordingRepositoryOf(FakeTimeSource()),
        navi: FakeNaviRepository = FakeNaviRepository(),
        gleo: FakeGleoActionRepository = FakeGleoActionRepository(),
    ) = HomeViewModel(
        recordingRepository = recording,
        recordPlace = FakeRecordPlaceUseCase(),
        drivingSignalRepository = FakeDrivingSignalRepository(DrivingState.PARKED),
        gleoActionRepository = gleo,
        naviRepository = navi,
    )

    @Test
    fun `V18 주행 화면에서 종료하면 홈도 완료 — 같은 출처 · 이 장소 기록 비활성`() = runTest(dispatcher) {
        val recording = recordingRepositoryOf(FakeTimeSource())
        val vm = viewModel(recording = recording)
        subscribe(vm)

        recording.endTrip()
        runCurrent()

        assertEquals(TripStatus.COMPLETED, vm.uiState.value.tripStatus)
        assertFalse(vm.uiState.value.recordPlaceEnabled)
        assertTrue(vm.uiState.value.startNewTripEnabled)
    }

    @Test
    fun `V34 완료 뒤 새 여행 시작 — 기록 중 · 0분 · 새 여행 시작 비활성 · 이 장소 기록 활성`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val recording = recordingRepositoryOf(time)
        val vm = viewModel(recording = recording)
        subscribe(vm)
        recording.endTrip()
        time.now += 30 * 60_000L

        vm.onStartNewTrip()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(TripStatus.RECORDING, state.tripStatus)
        assertEquals(0L, state.elapsedMinutes)
        assertFalse(state.startNewTripEnabled)
        assertTrue(state.recordPlaceEnabled)
    }

    @Test
    fun `V35 기록 중 — 새 여행 시작 비활성 · 눌러도 상태 · 경과 그대로`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val vm = viewModel(recording = recordingRepositoryOf(time))
        subscribe(vm)
        time.now += 5 * 60_000L

        assertFalse(vm.uiState.value.startNewTripEnabled)
        vm.onStartNewTrip()
        runCurrent()

        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V56 V35 주행 화면에서 일시정지하면 홈도 일시정지 — 경과 같은 값 · 이 장소 기록 활성 · 새 여행 시작 비활성`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val recording = recordingRepositoryOf(time)
        val vm = viewModel(recording = recording)
        subscribe(vm)
        time.now += 12 * 60_000L

        recording.togglePause()
        runCurrent()
        time.now += 30 * 60_000L
        vm.onStartNewTrip()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(TripStatus.PAUSED, state.tripStatus)
        assertEquals(12L, state.elapsedMinutes)
        assertTrue(state.recordPlaceEnabled)
        assertFalse(state.startNewTripEnabled)
    }

    @Test
    fun `T14 홈의 이 장소 기록도 같은 연결 지점 — 준비 중 안내`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onRecordPlace()
        runCurrent()

        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
    }

    @Test
    fun `T34 SDK 카드 — 기어 판정 · 내비 조회 · Gleo 값이 uiState 에 남음`() = runTest(dispatcher) {
        val vm = viewModel(
            navi = FakeNaviRepository(
                result = Result.success(
                    NaviRouteState(routing = true, mode = "EV", routeOption = "FAST", termsAgreed = true),
                ),
            ),
            gleo = FakeGleoActionRepository(opened = true),
        )
        subscribe(vm)

        vm.refreshNavi()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(DrivingState.PARKED, state.drivingState)
        assertTrue(state.navi is SdkStatus.Ready)
        assertTrue(state.openedByGleoAction)
    }

    private fun TestScope.subscribe(vm: HomeViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.runCurrent()
    }
}
