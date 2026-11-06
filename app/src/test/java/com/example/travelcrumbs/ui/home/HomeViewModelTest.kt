package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakePlaceStatsRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.pinnedPlacesRepositoryOf
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
        placeCatalogRepository = FakePlaceCatalogRepository(),
        pinnedPlacesRepository = pinnedPlacesRepositoryOf(),
        pinRepository = FakePinRepository(),
        placeStatsRepository = FakePlaceStatsRepository(),
    )

    @Test
    fun `T19 주행 화면에서 일시정지하면 홈도 일시정지 — 같은 출처`() = runTest(dispatcher) {
        val recording = recordingRepositoryOf(FakeTimeSource())
        val vm = viewModel(recording = recording)
        subscribe(vm)

        recording.togglePause()
        runCurrent()

        assertEquals(RecordingStatus.PAUSED, vm.uiState.value.recording)
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
