package com.example.travelcrumbs.ui.shell

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.unfinishedInputRepositoryOf
import com.example.travelcrumbs.ui.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val inputs: UnfinishedInputRepository = unfinishedInputRepositoryOf(FakeTimeSource(now = 7L))
    private val gleo = FakeGleoActionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AppShellViewModel(driving, inputs, handle, gleo)

    private val AppShellViewModel.state get() = uiState.value

    @Test
    fun `V1 V74 정차로 시작하면 정차 · 기록 선택 · 기록 강조 · 레일 4칸 순서 · 모두 레일 · 주행 중 허용은 기록 하나`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        assertFalse(vm.state.isDrivingMode)
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)
        assertEquals(
            listOf("records", "trips", "recommendations", "settings"),
            AppDestination.railItems.map { it.id },
        )
        assertEquals(AppDestination.entries, AppDestination.railItems)
        assertEquals(AppDestination.RECORDS, AppDestination.START)
        assertEquals(listOf(AppDestination.RECORDS), AppDestination.entries.filter { it.allowedWhileDriving })
    }

    @Test
    fun `V75 없어진 id home inbox · 모르는 문자열 · 없음은 기록`() {
        listOf("home", "inbox", "something", null).forEach { id ->
            assertEquals("id $id", AppDestination.RECORDS, AppDestination.fromId(id))
        }
    }

    @Test
    fun `V75 저장된 화면 상태의 목적지 id 가 home 이면 기록 칸`() = runTest(dispatcher) {
        // 둘째 판으로 저장된 화면 상태(정차 목적지 `home`)를 흉내 냄
        val vm = viewModel(SavedStateHandle(mapOf(SELECTED_KEY to "home")))
        subscribe(vm)

        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)
    }

    @Test
    fun `V2 V7 T2 레일 이동은 바꿔치기 · 뒤로는 기록`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.selectDestination(AppDestination.TRIPS)
        vm.selectDestination(AppDestination.RECOMMENDATIONS)
        advanceUntilIdle()
        assertEquals(AppDestination.RECOMMENDATIONS, vm.state.highlightedRailItem)

        vm.onBack()
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
    }

    @Test
    fun `V4 V6 레일 설정 칸 — 설정 · 강조 설정 · 뒤로는 기록`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.selectDestination(AppDestination.SETTINGS)
        advanceUntilIdle()
        assertEquals(AppDestination.SETTINGS, vm.state.selectedDestination)
        assertEquals(AppDestination.SETTINGS, vm.state.highlightedRailItem)

        vm.onBack()
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
    }

    @Test
    fun `V69 추천 보러 가기 — 추천 칸 · 강조 추천 · 뒤로는 기록`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.openRecommendations()
        advanceUntilIdle()
        assertEquals(AppDestination.RECOMMENDATIONS, vm.state.selectedDestination)
        assertEquals(AppDestination.RECOMMENDATIONS, vm.state.highlightedRailItem)

        vm.onBack()
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
    }

    @Test
    fun `V9 설정에서 주행 — 기록 칸 · 강조 기록 → 정차하면 설정으로 · 강조 설정`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.SETTINGS)

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        assertTrue(vm.state.isDrivingMode)
        assertEquals(AppDestination.RECORDS, vm.state.drivingDestination)
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)
        assertEquals(AppDestination.SETTINGS, vm.state.selectedDestination)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        assertFalse(vm.state.isDrivingMode)
        assertEquals(AppDestination.SETTINGS, vm.state.selectedDestination)
        assertEquals(AppDestination.SETTINGS, vm.state.highlightedRailItem)
    }

    @Test
    fun `V12 T9 판정 모름이면 주행과 같음`() = runTest(dispatcher) {
        driving.emit(DrivingState.UNKNOWN)
        val vm = viewModel()
        subscribe(vm)

        assertTrue(vm.state.isDrivingMode)
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)
    }

    @Test
    fun `D12 첫 값 전에는 주행처럼`() {
        assertTrue(viewModel().state.isDrivingMode)
    }

    @Test
    fun `V39 V42 주행 중 레일 이동 — 정차 목적지는 그대로 · 기록으로 돌아옴`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()

        vm.selectDestination(AppDestination.TRIPS)
        advanceUntilIdle()
        assertEquals(AppDestination.TRIPS, vm.state.drivingDestination)
        assertEquals(AppDestination.TRIPS, vm.state.highlightedRailItem)
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)

        vm.selectDestination(AppDestination.RECORDS)
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)
    }

    @Test
    fun `V43 주행 중 내 여행 칸에서 정차 — 내 여행 정차 화면 · 강조 내 여행`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        vm.selectDestination(AppDestination.TRIPS)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()

        assertEquals(AppDestination.TRIPS, vm.state.selectedDestination)
        assertEquals(AppDestination.TRIPS, vm.state.highlightedRailItem)
    }

    @Test
    fun `V10 추천에서 기어 R(주행) → P — 추천으로`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.RECOMMENDATIONS)

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.highlightedRailItem)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        assertEquals(AppDestination.RECOMMENDATIONS, vm.state.selectedDestination)
    }

    @Test
    fun `2-7 주행이 다시 시작되면 늘 기록 칸`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        vm.selectDestination(AppDestination.SETTINGS)
        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()

        assertEquals(AppDestination.RECORDS, vm.state.drivingDestination)
    }

    @Test
    fun `T21 T22 V27 주행으로 바뀌는 순간 입력 중 내용 저장 · 정차로 오면 띠`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.TRIPS)
        inputs.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "바다"))

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        assertTrue(vm.state.unfinishedInputAvailable)

        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        assertTrue(vm.state.unfinishedInputAvailable)
        assertEquals(AppDestination.TRIPS, vm.state.selectedDestination)
    }

    @Test
    fun `T23 V27 이어서 쓰기 — 저장 당시 목적지로 · 띠 사라짐 · 복원 상태`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        inputs.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "바다"))
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()
        vm.selectDestination(AppDestination.RECORDS)
        advanceUntilIdle()

        vm.resumeUnfinishedInput()
        advanceUntilIdle()

        assertEquals(AppDestination.TRIPS, vm.state.selectedDestination)
        assertFalse(vm.state.unfinishedInputAvailable)
        assertEquals("바다", inputs.pendingRestore.value?.fields?.get("memo"))
    }

    @Test
    fun `V76 작성 중 내용이 목적지 home 으로 저장됨 — 이어서 쓰기는 기록 칸 · 오류 없음`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        vm.selectDestination(AppDestination.TRIPS)
        inputs.updateActiveInput("home", mapOf("memo" to "옛 홈"))
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()

        vm.resumeUnfinishedInput()
        advanceUntilIdle()

        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
        assertFalse(vm.state.unfinishedInputAvailable)
    }

    @Test
    fun `T21 화면이 안 보이는 사이(구독 없음) 주행으로 바뀌어도 작성 중 내용 보존`() = runTest(dispatcher) {
        // uiState 를 구독하지 않음 — 화면이 안 보이는 상태
        viewModel()
        advanceUntilIdle()
        inputs.updateActiveInput(AppDestination.TRIPS.id, mapOf("memo" to "백그라운드"))

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()

        assertEquals("백그라운드", inputs.savedInput.first()?.fields?.get("memo"))
    }

    @Test
    fun `T24 입력 중 내용이 없으면 띠 없음`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        driving.emit(DrivingState.PARKED)
        advanceUntilIdle()

        assertFalse(vm.state.unfinishedInputAvailable)
        assertNull(inputs.savedInput.first())
    }

    @Test
    fun `V31 프로세스 재생성 뒤 설정 목적지 · 강조 설정 복원`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        subscribe(first)
        first.selectDestination(AppDestination.SETTINGS)
        advanceUntilIdle()

        val recreated = viewModel(copyOf(handle))
        subscribe(recreated)

        assertEquals(AppDestination.SETTINGS, recreated.state.selectedDestination)
        assertEquals(AppDestination.SETTINGS, recreated.state.highlightedRailItem)
    }

    @Test
    fun `3-5 주행 중 여행 칸에서 프로세스 재생성 — 주행 중 칸 그대로(기록으로 되돌리지 않음)`() = runTest(dispatcher) {
        driving.emit(DrivingState.DRIVING)
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        subscribe(first)
        first.selectDestination(AppDestination.TRIPS)
        advanceUntilIdle()

        val recreated = viewModel(copyOf(handle))
        subscribe(recreated)

        assertEquals(AppDestination.TRIPS, recreated.state.drivingDestination)
    }

    private companion object {
        /** AppShellViewModel 의 정차 목적지 저장 키 — 둘째 판으로 저장된 화면 상태를 흉내 낼 때만 씀. */
        const val SELECTED_KEY = "selected_destination"
    }

    private fun copyOf(handle: SavedStateHandle) =
        SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })

    @Test
    fun `69-T13 정차 중 Gleo 화면 요청 — 내 여행 · 지역 추천 · 종료 확인은 기록 칸 · 명령마다 한 번`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        subscribe(vm)

        vm.onGleoScreenRequest(1L, GleoScreenRequest.OpenTrips)
        advanceUntilIdle()
        assertEquals(AppDestination.TRIPS, vm.state.selectedDestination)

        val gyeongju = Region("경북", "경주시")
        vm.onGleoScreenRequest(2L, GleoScreenRequest.OpenRecommendations(gyeongju))
        advanceUntilIdle()
        assertEquals(AppDestination.RECOMMENDATIONS, vm.state.selectedDestination)
        assertEquals(gyeongju, vm.state.recommendationsRegion)
        val entry = vm.state.recommendationsEntry

        // 같은 명령을 다시 받아도(화면 재구성) 진입 값이 또 오르지 않음
        vm.onGleoScreenRequest(2L, GleoScreenRequest.OpenRecommendations(gyeongju))
        advanceUntilIdle()
        assertEquals(entry, vm.state.recommendationsEntry)

        vm.onGleoScreenRequest(3L, GleoScreenRequest.ConfirmEndTrip)
        advanceUntilIdle()
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)

        // 레일로 추천에 들어가면 Gleo 지역을 지우고 B-01 부터
        vm.selectDestination(AppDestination.RECOMMENDATIONS)
        advanceUntilIdle()
        assertNull(vm.state.recommendationsRegion)
    }

    @Test
    fun `69-T12 주행 · 모름에서는 Gleo 화면 요청으로 칸을 옮기지 않음`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)
        driving.emit(DrivingState.DRIVING)
        advanceUntilIdle()
        val before = vm.state.drivingDestination

        vm.onGleoScreenRequest(1L, GleoScreenRequest.OpenRecommendations(Region("경북", "경주시")))
        vm.onGleoScreenRequest(2L, GleoScreenRequest.OpenTrips)
        advanceUntilIdle()

        assertEquals(before, vm.state.drivingDestination)
        assertEquals(AppDestination.RECORDS, vm.state.selectedDestination)
        assertNull(vm.state.recommendationsRegion)
    }

    private fun TestScope.subscribe(vm: AppShellViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.advanceUntilIdle()
    }
}
