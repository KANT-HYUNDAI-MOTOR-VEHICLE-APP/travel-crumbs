package com.example.travelcrumbs.ui.driving

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeStartVoiceInputUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrivingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val driving = FakeDrivingSignalRepository(DrivingState.DRIVING)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        recordPlace: ActionResult = ActionResult.NOT_READY,
        voice: ActionResult = ActionResult.NOT_READY,
        recording: RecordingRepository = recordingRepositoryOf(FakeTimeSource()),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = DrivingViewModel(
        recordingRepository = recording,
        recordPlace = FakeRecordPlaceUseCase(recordPlace),
        startVoiceInput = FakeStartVoiceInputUseCase(voice),
        drivingSignalRepository = driving,
        savedStateHandle = handle,
    )

    @Test
    fun `V11 첫 실행 — 여행명 없음 · 기록 중 · 0분 · 장소 0 · 팝업 없음 · 버튼 활성`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        val state = vm.uiState.value
        assertNull(state.tripName)
        assertEquals(TripStatus.RECORDING, state.tripStatus)
        assertEquals(0L, state.elapsedMinutes)
        assertEquals(0, state.placeCount)
        assertFalse(state.endConfirmVisible)
        assertTrue(state.recordingActionsEnabled)
    }

    @Test
    fun `V24 이 장소 기록 — 준비 중 안내 · 보여 준 뒤 지움`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onRecordPlace()
        runCurrent()
        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)

        vm.onMessageShown()
        runCurrent()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `V24 음성 — 준비 중 안내`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onVoice()
        runCurrent()

        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
    }

    @Test
    fun `T16 연결 지점이 DONE 이면 안내 없음`() = runTest(dispatcher) {
        val vm = viewModel(recordPlace = ActionResult.DONE)
        subscribe(vm)

        vm.onRecordPlace()
        runCurrent()

        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `V13 여행 종료 — 팝업만 열림 · 상태는 아직 기록 중`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onEndTrip()
        runCurrent()

        assertTrue(vm.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V14 V16 계속 기록 · 뒤로 · 바깥 누름 — 팝업 닫힘 · 기록 중 그대로`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onEndTrip()

        vm.onContinueRecording()
        runCurrent()

        assertFalse(vm.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V15 V25 종료 — COMPLETED · 팝업 닫힘 · 이 장소 기록 · 여행 종료 비활성 · 음성은 여전히 안내`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onEndTrip()

        vm.onConfirmEndTrip()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(TripStatus.COMPLETED, state.tripStatus)
        assertFalse(state.endConfirmVisible)
        assertFalse(state.recordingActionsEnabled)

        vm.onVoice()
        runCurrent()
        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
    }

    @Test
    fun `V17 N11 팝업 열린 채 정차 — 팝업 닫힘 · 기록 중 · 다시 주행이어도 저절로 안 열림`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onEndTrip()
        runCurrent()

        driving.emit(DrivingState.PARKED)
        runCurrent()
        assertFalse(vm.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)

        driving.emit(DrivingState.DRIVING)
        runCurrent()
        assertFalse(vm.uiState.value.endConfirmVisible)
    }

    @Test
    fun `V32 프로세스 재생성 — 팝업 다시 보임 · 기록 중`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val recording = recordingRepositoryOf(FakeTimeSource())
        viewModel(recording = recording, handle = handle).onEndTrip()

        val recreated = viewModel(
            recording = recording,
            handle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }),
        )
        subscribe(recreated)

        assertTrue(recreated.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.RECORDING, recreated.uiState.value.tripStatus)
    }

    @Test
    fun `V47 기록 중 — 버튼 넷 다 활성 · 버튼 3 은 기록 일시정지`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        val state = vm.uiState.value
        assertTrue(state.recordingActionsEnabled)
        assertTrue(state.pauseButtonEnabled)
        assertFalse(state.pauseButtonShowsResume)
    }

    @Test
    fun `V48 V49 기록 일시정지 → 일시정지 · 기록 재개 글자 · 팝업 없음 → 재개하면 기록 중`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onTogglePause()
        runCurrent()
        val paused = vm.uiState.value
        assertEquals(TripStatus.PAUSED, paused.tripStatus)
        assertTrue(paused.pauseButtonShowsResume)
        assertTrue(paused.pauseButtonEnabled)
        assertFalse(paused.endConfirmVisible)

        vm.onTogglePause()
        runCurrent()
        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)
        assertFalse(vm.uiState.value.pauseButtonShowsResume)
    }

    @Test
    fun `V51 V52 일시정지 중 이 장소 기록 · 음성 — 준비 중 안내 · 일시정지 그대로 · 버튼 활성`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onTogglePause()
        runCurrent()

        assertTrue(vm.uiState.value.recordingActionsEnabled)
        vm.onRecordPlace()
        runCurrent()
        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
        assertEquals(TripStatus.PAUSED, vm.uiState.value.tripStatus)

        vm.onMessageShown()
        vm.onVoice()
        runCurrent()
        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
        assertEquals(TripStatus.PAUSED, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V53 일시정지에서 여행 종료 → 계속 기록 — 팝업 열림 → 닫힘 · 일시정지 그대로`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onTogglePause()
        runCurrent()

        vm.onEndTrip()
        runCurrent()
        assertTrue(vm.uiState.value.endConfirmVisible)

        vm.onContinueRecording()
        runCurrent()
        assertFalse(vm.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.PAUSED, vm.uiState.value.tripStatus)
        assertTrue(vm.uiState.value.pauseButtonShowsResume)
    }

    @Test
    fun `V54 일시정지에서 종료 — 완료 · 이 장소 기록 · 기록 일시정지 · 여행 종료 비활성`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onTogglePause()
        runCurrent()
        vm.onEndTrip()
        runCurrent()

        vm.onConfirmEndTrip()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(TripStatus.COMPLETED, state.tripStatus)
        assertFalse(state.recordingActionsEnabled)
        assertFalse(state.pauseButtonEnabled)
        assertFalse(state.pauseButtonShowsResume)
    }

    @Test
    fun `V55 V65 일시정지 · 팝업 열린 채 정차 → 팝업 닫힘 · 다시 주행이어도 일시정지 그대로`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onTogglePause()
        runCurrent()
        vm.onEndTrip()
        runCurrent()

        driving.emit(DrivingState.PARKED)
        runCurrent()
        assertFalse(vm.uiState.value.endConfirmVisible)
        assertEquals(TripStatus.PAUSED, vm.uiState.value.tripStatus)

        driving.emit(DrivingState.DRIVING)
        runCurrent()
        assertEquals(TripStatus.PAUSED, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V62 일시정지 · 프로세스 재생성 — 일시정지 · 기록 재개 글자 복원`() = runTest(dispatcher) {
        val recording = recordingRepositoryOf(FakeTimeSource())
        val vm = viewModel(recording = recording)
        subscribe(vm)
        vm.onTogglePause()
        runCurrent()

        val recreated = viewModel(recording = recording)
        subscribe(recreated)

        assertEquals(TripStatus.PAUSED, recreated.uiState.value.tripStatus)
        assertTrue(recreated.uiState.value.pauseButtonShowsResume)
    }

    @Test
    fun `V15 종료 뒤 기록 일시정지 비활성`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.onConfirmEndTrip()
        runCurrent()

        assertFalse(vm.uiState.value.pauseButtonEnabled)
    }

    private fun TestScope.subscribe(vm: DrivingViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.runCurrent()
    }
}
