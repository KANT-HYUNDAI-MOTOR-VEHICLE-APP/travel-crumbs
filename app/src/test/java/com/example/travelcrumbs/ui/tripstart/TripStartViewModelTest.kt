package com.example.travelcrumbs.ui.tripstart

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.fakeRecordingLocal
import com.example.travelcrumbs.testing.guideTrackerOf
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

/** A-01 여행 시작(#51 U3) — 입력 · 동의 · 여행 저장 · 실제 내비 길안내 요청 · 내비 앱 열기. */
@OptIn(ExperimentalCoroutinesApi::class)
class TripStartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val time = FakeTimeSource(now = 1_794_106_800_000L)
    private val local = fakeRecordingLocal(CurrentTrip.start(0).ended(1))
    private val recording = recordingRepositoryOf(time, local)
    private val users = FakeCurrentUserRepository(time = time)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(navi: FakeNaviRepository = FakeNaviRepository()): TripStartViewModel {
        val vm = TripStartViewModel(recording, users, FakePlaceCatalogRepository(), guideTrackerOf(navi, FakePinRepository()), navi, time, SavedStateHandle())
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun `U3-2 입력 — 동의 필요 · 혼자는 다른 동행자와 함께 못 고름 · 목적지 없음이 기본`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.uiState.value.consentNeeded)
        assertNull(vm.uiState.value.destinationId)
        assertEquals(5, vm.uiState.value.destinations.size)

        vm.onToggleCompanion(CompanionType.SPOUSE)
        vm.onToggleCompanion(CompanionType.CHILD)
        runCurrent()
        assertEquals(setOf(CompanionType.SPOUSE, CompanionType.CHILD), vm.uiState.value.companions)
        vm.onToggleCompanion(CompanionType.SOLO)
        runCurrent()
        assertEquals(setOf(CompanionType.SOLO), vm.uiState.value.companions)
        vm.onToggleCompanion(CompanionType.PET)
        runCurrent()
        assertEquals(setOf(CompanionType.PET), vm.uiState.value.companions)
    }

    @Test
    fun `U3-3 목적지 있음 — 동의 행 · 기록 중 여행(이름 · 동행자 · 목적지) · 그 장소로 실제 길안내 요청 · 끝`() = runTest(dispatcher) {
        val navi = FakeNaviRepository()
        val vm = viewModel(navi)
        vm.onNameChange("속초 가족여행")
        vm.onToggleCompanion(CompanionType.CHILD)
        vm.onSelectDestination("b")
        runCurrent()

        vm.onStart("11월 10일 여행")
        runCurrent()

        assertEquals(listOf(ConsentType.LOCATION_COLLECT to true), users.history)
        val trip = local.current!!
        assertEquals(TripStatus.RECORDING, trip.status)
        assertEquals("속초 가족여행", trip.tripName)
        assertEquals(setOf(CompanionType.CHILD), trip.companions)
        assertEquals("b", navi.routeRequests.single().id)
        assertEquals(trip.destPlaceNm, navi.routeRequests.single().name)
        assertTrue(vm.uiState.value.done)
        assertNull(vm.uiState.value.message)
        assertEquals(0, navi.naviAppOpens)
    }

    @Test
    fun `U3-4 목적지 없음 · 이름 비움 — 기본 이름 · 내비 앱을 엶`() = runTest(dispatcher) {
        val navi = FakeNaviRepository()
        val vm = viewModel(navi)

        vm.onStart("11월 10일 여행")
        runCurrent()

        assertEquals("11월 10일 여행", local.current!!.tripName)
        assertEquals(1, navi.naviAppOpens)
        assertTrue(navi.routeRequests.isEmpty())
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `U3-5 내비 미연결 — 기록은 시작 · 내비를 열지 못했어요 한 줄 · 이미 동의했으면 동의 행 없음`() = runTest(dispatcher) {
        users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        val navi = FakeNaviRepository(ready = false)
        val vm = viewModel(navi)
        assertFalse(vm.uiState.value.consentNeeded)
        vm.onSelectDestination("a")
        runCurrent()

        vm.onStart("11월 10일 여행")
        runCurrent()

        assertEquals(TripStatus.RECORDING, local.current!!.status)
        assertEquals(UiMessage.NAVI_START_FAILED, vm.uiState.value.message)
        assertEquals(1, users.history.size)
    }
}
