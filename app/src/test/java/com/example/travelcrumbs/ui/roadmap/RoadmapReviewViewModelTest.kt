package com.example.travelcrumbs.ui.roadmap

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeTagRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.completedTrip
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

/** A-05 로드맵 검토 · A-06 저장 · 공개(#51 U5). */
@OptIn(ExperimentalCoroutinesApi::class)
class RoadmapReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val trips = FakeTripRepository(listOf(completedTrip(tripNo = 7, places = 3, draftFirst = true)))
    private val users = FakeCurrentUserRepository(nicknameNm = "민지네")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): RoadmapReviewViewModel {
        val vm = RoadmapReviewViewModel(trips, FakeTagRepository(), users, FakeTimeSource(), SavedStateHandle(), com.example.travelcrumbs.testing.FakePlaceCatalogRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        vm.load(7)
        runCurrent()
        return vm
    }

    @Test
    fun `U5-1 검토 — 장소 순서 · 태그 이름 · 미완성 · 경로 핀 3개 · 기록 시간 · 지역 강원 속초시`() = runTest(dispatcher) {
        val state = viewModel().uiState.value

        assertEquals(RoadmapStep.REVIEW, state.step)
        assertEquals(listOf(1, 2, 3), state.places.map { it.placeOrd })
        assertTrue(state.places[0].draft)
        assertEquals(listOf("아이와 가기 좋아요"), state.places[1].tagNms)
        assertEquals(3, state.sketch.size)
        assertEquals(60L, state.durationMinutes)
        assertEquals("강원", state.regionSidoNm)
        assertEquals("속초시", state.regionSigunguNm)
    }

    @Test
    fun `U5-2 로드맵 저장 → 공개 처음 — 동의 카드 · 저장하면 RECO_PUBLIC 동의 · ANONYMOUS_PUBLIC · 검토 끝 · 보관함으로`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSaveRoadmap()
        vm.onChoosePublic(true)
        runCurrent()
        assertEquals(RoadmapStep.PUBLISH, vm.uiState.value.step)
        assertTrue(vm.uiState.value.recoConsentNeeded)

        vm.onPublish()
        runCurrent()

        assertEquals(listOf(ConsentType.RECO_PUBLIC to true), users.history)
        assertEquals(Visibility.ANONYMOUS_PUBLIC, trips.trip(7).first()!!.visibility)
        assertTrue(vm.uiState.value.done)
        vm.onDoneHandled()
        runCurrent()
        assertNull(trips.pendingReviewTripNo.first())
    }

    @Test
    fun `U5-3 비공개 저장 — 동의 행 없음 · PRIVATE · 검토 끝`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSaveRoadmap()
        vm.onPublish()
        runCurrent()
        vm.onDoneHandled()
        runCurrent()

        assertTrue(users.history.isEmpty())
        assertEquals(Visibility.PRIVATE, trips.trip(7).first()!!.visibility)
        assertNull(trips.pendingReviewTripNo.first())
    }

    @Test
    fun `U5-4 뒤로 — A-06 에선 A-05 · A-05 에선 나중에(검토 끝 · 비공개 그대로) · 태그 달기는 A-03 시트`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEditPlace(701)
        runCurrent()
        assertEquals(701L, vm.uiState.value.tagSheet?.tripPlaceNo)
        vm.onSheetToggleTag(3L)
        vm.onSheetSave()
        runCurrent()
        assertFalse(vm.uiState.value.places[0].draft)

        vm.onSaveRoadmap()
        vm.onBack()
        runCurrent()
        assertEquals(RoadmapStep.REVIEW, vm.uiState.value.step)
        assertEquals(7L, trips.pendingReviewTripNo.first())

        vm.onBack()
        runCurrent()
        assertNull(trips.pendingReviewTripNo.first())
        assertEquals(Visibility.PRIVATE, trips.trip(7).first()!!.visibility)
    }
}
