package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.DraftPlaceInput
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 장소 기록 · A-03 태그 시트(#51 U4) — 실제 「이 장소 기록」 구현 + 기록 칸 ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTagSheetTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun graph(driving: DrivingState = DrivingState.PARKED) =
        ShellTestGraph(initialDriving = driving, tripNo = TRIP_NO, realRecordPlace = true).apply {
            trips.put(
                TripRecord(TRIP_NO, "속초", TripStatus.RECORDING, Visibility.PRIVATE, 0L, null, null, null, setOf(CompanionType.CHILD), emptyList()),
            )
        }

    private fun TestScope.home(graph: ShellTestGraph): HomeViewModel {
        val vm = graph.viewModel(HomeViewModel::class.java)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun `U4-2 정차 이 장소 기록 — 위치 · 주소로 장소 저장(임시) · A-03 시트 · 태그 고르고 저장하면 임시 해제 · 장소를 저장했어요`() = runTest(dispatcher) {
        val graph = graph()
        graph.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        val vm = home(graph)

        vm.onRecordPlace()
        runCurrent()

        val place = graph.trips.current.single().places.single()
        assertTrue(place.draft)
        assertEquals(38.2049, place.lat!!, 0.0)
        val sheet = vm.uiState.value.tagSheet
        assertNotNull(sheet)
        assertFalse(sheet!!.editing)
        assertEquals("강원 속초시 중앙로 147", sheet.userPlaceNm)
        assertEquals(8, sheet.tags.size)

        vm.onToggleSheetTag(2L)
        vm.onTagMemoChange("지하 2층")
        vm.onSaveTagSheet()
        runCurrent()

        val saved = graph.trips.current.single().places.single()
        assertFalse(saved.draft)
        assertEquals(listOf(2L), saved.tagNos)
        assertEquals("지하 2층", saved.memo)
        assertNull(vm.uiState.value.tagSheet)
        assertEquals(UiMessage.PLACE_SAVED, vm.uiState.value.message)
    }

    @Test
    fun `U4-3 위치 동의 없음 — 위치 없이 장소 저장 · 건너뛰기면 임시저장 그대로`() = runTest(dispatcher) {
        val graph = graph()
        val vm = home(graph)

        vm.onRecordPlace()
        runCurrent()
        assertNull(graph.trips.current.single().places.single().lat)

        vm.onSkipTagSheet()
        runCurrent()
        assertNull(vm.uiState.value.tagSheet)
        assertTrue(graph.trips.current.single().places.single().draft)
    }

    @Test
    fun `U4-4 주행 중 기록 — 시트 없이 이어서 쓰기 저장 · 정차 뒤 띠 이어서 쓰기 → 그 장소 시트`() = runTest(dispatcher) {
        val graph = graph(DrivingState.DRIVING)
        val vm = home(graph)

        graph.recordPlace()
        runCurrent()
        val placeNo = graph.trips.current.single().places.single().tripPlaceNo
        val saved = graph.inputs.savedInput.first()
        assertEquals(AppDestination.RECORDS.id, saved?.destinationId)
        assertEquals(DraftPlaceInput.DESTINATION_ID, AppDestination.RECORDS.id)

        graph.driving.emit(DrivingState.PARKED)
        graph.inputs.takeForRestore()
        runCurrent()

        assertEquals(placeNo, vm.uiState.value.tagSheet?.tripPlaceNo)
        assertNull(graph.inputs.pendingRestore.value)
    }

    @Test
    fun `U4-5 진행 중 여행 없음 — 기록하지 않고 준비 중`() = runTest(dispatcher) {
        val graph = ShellTestGraph(withTrip = false, realRecordPlace = true)
        val vm = home(graph)

        vm.onRecordPlace()
        runCurrent()

        assertTrue(graph.trips.current.isEmpty())
        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
    }

    private companion object {
        const val TRIP_NO = 1L
    }
}
