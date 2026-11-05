package com.example.travelcrumbs.ui.driving

import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.RecordingStatus
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrivingViewModelTest {

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
        recordPlace: ActionResult = ActionResult.NOT_READY,
        voice: ActionResult = ActionResult.NOT_READY,
    ) = DrivingViewModel(
        recordingRepositoryOf(FakeTimeSource()),
        FakeRecordPlaceUseCase(recordPlace),
        FakeStartVoiceInputUseCase(voice),
    )

    @Test
    fun `T14 이 장소 기록 — 준비 중 안내 · 보여 준 뒤 지움`() = runTest(dispatcher) {
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
    fun `T15 음성 입력 — 준비 중 안내`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onVoiceInput()
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
    fun `T17 T18 일시정지 토글`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        assertEquals(RecordingStatus.RECORDING, vm.uiState.value.recording)

        vm.onTogglePause()
        runCurrent()
        assertEquals(RecordingStatus.PAUSED, vm.uiState.value.recording)

        vm.onTogglePause()
        runCurrent()
        assertEquals(RecordingStatus.RECORDING, vm.uiState.value.recording)
    }

    private fun TestScope.subscribe(vm: DrivingViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.runCurrent()
    }
}
