package com.example.travelcrumbs.ui

import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.driving.DrivingViewModel
import com.example.travelcrumbs.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** T51(#66 3.8) — 정차 홈 · 주행 화면 「여행 종료」가 같은 동작(대기 확정 → 종료)을 부름 · 확정 1번. */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoRecordEndTripScreensTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun graph(): ShellTestGraph {
        val g = ShellTestGraph(tripNo = TRIP)
        g.trips.put(TripRecord(TRIP, "여행", TripStatus.RECORDING, Visibility.PRIVATE, 0L, null, null, null, emptySet(), emptyList()))
        g.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        g.autoStore.pendingList = listOf(PendingArrival(TRIP, "경복궁", "서울 종로구 사직로 161", 37.5796, 126.977, g.time.now, parkedSeen = false))
        return g
    }

    @Test
    fun `T51 정차 홈 여행 종료 — 대기 확정 → COMPLETED · 확정 1번`() = runTest(dispatcher) {
        val g = graph()
        val vm = g.viewModel(HomeViewModel::class.java)
        vm.onEndTrip()
        vm.onConfirmEndTrip()
        runCurrent()
        assertEquals(listOf("경복궁"), g.trips.current.single().places.map { it.userPlaceNm })
        assertEquals(TripStatus.COMPLETED, g.recording.progress.first().status)
        assertTrue(g.autoStore.pendingList.isEmpty())
        vm.onConfirmEndTrip()
        runCurrent()
        assertEquals(1, g.trips.current.single().places.size)
    }

    @Test
    fun `T51 주행 화면 여행 종료 — 대기 확정 → COMPLETED · 확정 1번`() = runTest(dispatcher) {
        val g = graph()
        val vm = g.viewModel(DrivingViewModel::class.java)
        vm.onEndTrip()
        vm.onConfirmEndTrip()
        runCurrent()
        assertEquals(listOf("경복궁"), g.trips.current.single().places.map { it.userPlaceNm })
        assertEquals(TripStatus.COMPLETED, g.recording.progress.first().status)
        assertTrue(g.autoStore.pendingList.isEmpty())
    }

    private companion object {
        const val TRIP = 7L
    }
}
