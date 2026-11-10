package com.example.travelcrumbs.ui.trips

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeTagRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.completedTrip
import com.example.travelcrumbs.testing.ratingRepositoryOf
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.tripdetail.TripDetailViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A-07 보관함 · A-08 상세(#51 U6). */
@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val trips = FakeTripRepository(
        listOf(
            completedTrip(tripNo = 1, tripNm = "강릉 커피 여행"),
            completedTrip(tripNo = 2, tripNm = "속초 가족여행", visibility = Visibility.ANONYMOUS_PUBLIC),
            completedTrip(tripNo = 3, tripNm = "진행 중").copy(status = TripStatus.RECORDING),
        ),
    )
    private val users = FakeCurrentUserRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.list(): TripsViewModel {
        val vm = TripsViewModel(trips, users, ratingRepositoryOf(), FakeTimeSource(), SavedStateHandle())
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    private fun TestScope.detail(tripNo: Long): TripDetailViewModel {
        val vm = TripDetailViewModel(trips, FakeTagRepository(), users, ratingRepositoryOf(), FakeTimeSource(), SavedStateHandle())
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load(tripNo)
        runCurrent()
        return vm
    }

    @Test
    fun `U6-1 보관함 — 끝난 여행만 최근 순 · 공개 상태 · 진입 값에 여행이 있으면 그 상세`() = runTest(dispatcher) {
        val vm = list()
        assertEquals(listOf("속초 가족여행", "강릉 커피 여행"), vm.uiState.value.trips.map { it.tripNm })
        assertEquals(listOf(true, false), vm.uiState.value.trips.map { it.public })

        vm.onEntry(entry = 1, openTripNo = 2)
        runCurrent()
        assertEquals(2L, vm.uiState.value.selectedTripNo)
        vm.onEntry(entry = 1, openTripNo = null)
        runCurrent()
        assertEquals("같은 진입 값이면 그대로", 2L, vm.uiState.value.selectedTripNo)
        vm.onCloseTrip()
        runCurrent()
        assertNull(vm.uiState.value.selectedTripNo)
    }

    @Test
    fun `U6-2 공개 토글 — 끄면 바로 비공개 · 켤 때 동의 없으면 팝업 · 동의하고 공개 · 동의 있으면 바로 공개`() = runTest(dispatcher) {
        val vm = list()
        vm.onTogglePublic(2, false)
        runCurrent()
        assertEquals(Visibility.PRIVATE, trips.trip(2).first()!!.visibility)

        vm.onTogglePublic(1, true)
        runCurrent()
        assertEquals(1L, vm.uiState.value.consentAskTripNo)
        assertEquals(Visibility.PRIVATE, trips.trip(1).first()!!.visibility)
        vm.onConsentAndPublish()
        runCurrent()
        assertEquals(Visibility.ANONYMOUS_PUBLIC, trips.trip(1).first()!!.visibility)
        assertEquals(listOf(ConsentType.RECO_PUBLIC to true), users.history)

        vm.onTogglePublic(2, true)
        runCurrent()
        assertNull(vm.uiState.value.consentAskTripNo)
        assertEquals(Visibility.ANONYMOUS_PUBLIC, trips.trip(2).first()!!.visibility)
    }

    @Test
    fun `U6-3 상세 — 이름 수정 즉시 저장 · 빈 이름은 안 됨 · 장소 고치기 A-03 시트 · 공개 토글`() = runTest(dispatcher) {
        val vm = detail(1)
        assertEquals("강릉 커피 여행", vm.uiState.value.summary?.tripNm)
        assertEquals(2, vm.uiState.value.places.size)

        vm.onEditName()
        runCurrent()
        vm.onRenameChange("  ")
        vm.onSaveRename()
        runCurrent()
        assertEquals("  ", vm.uiState.value.renameDraft)
        vm.onRenameChange("강릉 바다 여행")
        vm.onSaveRename()
        runCurrent()
        assertEquals("강릉 바다 여행", trips.trip(1).first()!!.tripNm)
        assertEquals(UiMessage.ROADMAP_SAVED, vm.uiState.value.message)

        vm.onEditPlace(101)
        runCurrent()
        assertTrue(vm.uiState.value.tagSheet!!.editing)
        vm.onSheetMemoChange("주차장 넓음")
        vm.onSheetSave()
        runCurrent()
        assertEquals("주차장 넓음", trips.trip(1).first()!!.places.first().memo)

        users.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
        vm.onTogglePublic(true)
        runCurrent()
        assertFalse(vm.uiState.value.consentAsk)
        assertTrue(vm.uiState.value.summary!!.public)
    }

    @Test
    fun `R2-2 상세를 연 여행이 다른 곳에서 지워지면 목록으로 · 다시 열면 삭제 확인 팝업은 닫힌 채`() = runTest(dispatcher) {
        val vm = detail(2)
        vm.onAskDelete()
        runCurrent()
        trips.removeQuietly(2)
        runCurrent()
        assertTrue(vm.uiState.value.deleted)
        vm.onDeletedHandled()
        vm.load(1)
        runCurrent()
        assertFalse(vm.uiState.value.deleteAsk)
        assertFalse(vm.uiState.value.deleted)
    }

    @Test
    fun `R1-3 여행 삭제 — 확인 팝업 · 지우면 보관함에서 빠지고 화면은 목록으로`() = runTest(dispatcher) {
        val list = list()
        val vm = detail(1)
        vm.onAskDelete()
        runCurrent()
        assertTrue(vm.uiState.value.deleteAsk)
        vm.onConfirmDelete()
        runCurrent()
        assertTrue(vm.uiState.value.deleted)
        assertTrue(list.uiState.value.trips.none { it.tripNo == 1L })
        vm.onDeletedHandled()
        runCurrent()
        assertFalse(vm.uiState.value.deleted)
    }
}
