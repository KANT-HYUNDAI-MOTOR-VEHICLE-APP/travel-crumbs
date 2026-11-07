package com.example.travelcrumbs.ui.tripsetup

import com.example.travelcrumbs.domain.TripNameSuggester
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeCodeRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.ui.components.UiMessage
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class TripSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val noon = LocalDate.of(2026, 11, 7).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        trips: FakeTripRepository = FakeTripRepository(),
        suggester: TripNameSuggester = TripNameSuggester { it },
    ) = TripSetupViewModel(
        tripRepository = trips,
        codeRepository = FakeCodeRepository(),
        nameSuggester = suggester,
        timeSource = FakeTimeSource(noon),
    )

    @Test
    fun `기본 여행명은 오늘 날짜`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()
        assertEquals("2026.11.07 여행", vm.uiState.value.tripName)
    }

    @Test
    fun `수정 — 추천 자리에서 받은 이름으로 입력칸이 열림`() = runTest(dispatcher) {
        val vm = viewModel(suggester = { "$it · Gleo 추천" })
        vm.prepare()
        runCurrent()

        vm.onEditName()
        runCurrent()

        assertTrue(vm.uiState.value.editingName)
        assertEquals("2026.11.07 여행 · Gleo 추천", vm.uiState.value.tripName)
    }

    @Test
    fun `동행자 — 여러 개 선택, 혼자는 다른 동행자와 함께 못 고름`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()

        vm.onToggleCompanion("CHILD")
        vm.onToggleCompanion("PET")
        assertEquals(setOf("CHILD", "PET"), vm.uiState.value.selectedCompanions)

        vm.onToggleCompanion("SOLO")
        assertEquals(setOf("SOLO"), vm.uiState.value.selectedCompanions)

        vm.onToggleCompanion("PARENT")
        assertEquals(setOf("PARENT"), vm.uiState.value.selectedCompanions)
    }

    @Test
    fun `동행자를 안 고르면 시작 못 함`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()
        assertFalse(vm.uiState.value.canStart)
    }

    @Test
    fun `여행 시작 — 화면 순서대로 저장하고 시작한 여행명을 남김`() = runTest(dispatcher) {
        val trips = FakeTripRepository()
        val vm = viewModel(trips = trips)
        vm.prepare()
        runCurrent()
        vm.onToggleCompanion("PET")
        vm.onToggleCompanion("CHILD")

        vm.onStart()
        runCurrent()

        assertEquals(listOf("2026.11.07 여행" to listOf("CHILD", "PET")), trips.started)
        assertEquals("2026.11.07 여행", vm.uiState.value.startedTripName)
    }

    @Test
    fun `여행 시작 — 이미 기록 중이면 안내만`() = runTest(dispatcher) {
        val trips = FakeTripRepository(current = Trip(1L, "uuid-1", "기존 여행", "RECORDING", listOf("SOLO")))
        val vm = viewModel(trips = trips)
        vm.prepare()
        runCurrent()
        vm.onToggleCompanion("SOLO")

        vm.onStart()
        runCurrent()

        assertEquals(UiMessage.TRIP_ALREADY_RECORDING, vm.uiState.value.message)
        assertEquals(null, vm.uiState.value.startedTripName)
    }
}
