package com.example.travelcrumbs.ui.shell

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.draft.DraftRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.draftRepositoryOf
import com.example.travelcrumbs.ui.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
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
class AppShellViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val driving = FakeDrivingSignalRepository(DrivingState.PARKED)
    private val drafts: DraftRepository = draftRepositoryOf(FakeTimeSource(now = 7L))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AppShellViewModel(driving, drafts, handle)

    @Test
    fun `T1 정차로 시작하면 레일 화면 · 홈 선택`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        assertFalse(vm.uiState.value.showDrivingScreen)
        assertEquals(AppDestination.HOME, vm.uiState.value.selectedDestination)
    }

    @Test
    fun `T2 T3 레일 이동은 바꿔치기 · 뒤로는 홈`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.selectDestination(AppDestination.TRIPS)
        vm.selectDestination(AppDestination.SETTINGS)
        advanceUntilIdle()
        assertEquals(AppDestination.SETTINGS, vm.uiState.value.selectedDestination)

        vm.onBack()
        advanceUntilIdle()
        assertEquals(AppDestination.HOME, vm.uiState.value.selectedDestination)
    }

    @Test
    fun `T5 T8 주행이면 주행 화면 · 정차로 오면 직전 목적지`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.SETTINGS)

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showDrivingScreen)
        assertEquals(AppDestination.SETTINGS, vm.uiState.value.selectedDestination)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDrivingScreen)
        assertEquals(AppDestination.SETTINGS, vm.uiState.value.selectedDestination)
    }

    @Test
    fun `T9 T12 판정 모름이면 주행 화면`() = runTest(dispatcher) {
        driving.emit(DrivingState.UNKNOWN)
        val vm = viewModel()
        subscribe(vm)

        assertTrue(vm.uiState.value.showDrivingScreen)
    }

    @Test
    fun `D12 첫 값 전에는 주행 화면`() {
        assertTrue(viewModel().uiState.value.showDrivingScreen)
    }

    @Test
    fun `T21 T22 주행으로 바뀌는 순간 입력 중 내용 저장 · 정차로 오면 띠`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.TRIPS)
        drafts.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "바다"))

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.draftAvailable)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.draftAvailable)
        assertEquals(AppDestination.TRIPS, vm.uiState.value.selectedDestination)
    }

    @Test
    fun `T23 이어서 쓰기 — 저장 당시 목적지로 · 띠 사라짐 · 복원 상태`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        drafts.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "바다"))
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        driving.emit(DrivingState.PARKED)
        vm.selectDestination(AppDestination.HOME)
        advanceUntilIdle()

        vm.resumeDraft()
        advanceUntilIdle()

        assertEquals(AppDestination.TRIPS, vm.uiState.value.selectedDestination)
        assertFalse(vm.uiState.value.draftAvailable)
        assertEquals("바다", drafts.pendingRestore.value?.fields?.get("memo"))
    }

    @Test
    fun `T21 화면이 안 보이는 사이(구독 없음) 주행으로 바뀌어도 임시 저장`() = runTest(dispatcher) {
        // uiState 를 구독하지 않음 — 화면이 안 보이는 상태
        viewModel()
        advanceUntilIdle()
        drafts.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "백그라운드"))

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()

        assertEquals("백그라운드", drafts.savedDraft.first()?.fields?.get("memo"))
    }

    @Test
    fun `T24 입력 중 내용이 없으면 띠 없음`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.draftAvailable)
        assertNull(drafts.savedDraft.first())
    }

    @Test
    fun `T30 프로세스 재생성 뒤 선택된 목적지 복원`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        viewModel(handle).selectDestination(AppDestination.TRIPS)

        val recreated = viewModel(SavedStateHandle(mapOf("selected_destination" to handle.get<String>("selected_destination"))))
        subscribe(recreated)

        assertEquals(AppDestination.TRIPS, recreated.uiState.value.selectedDestination)
    }

    private fun TestScope.subscribe(vm: AppShellViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.advanceUntilIdle()
    }
}
