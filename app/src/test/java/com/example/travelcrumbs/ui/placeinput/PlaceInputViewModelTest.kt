package com.example.travelcrumbs.ui.placeinput

import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.testing.FakeCodeRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePlaceRepository
import com.example.travelcrumbs.testing.FakeTripRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceInputViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val trip = Trip(1L, "uuid-1", "2026.11.07 여행", "RECORDING", listOf("SOLO"), placeCount = 1)
    private val draft = TripPlace(10L, 37.7543, 128.8990, "2026-11-07T09:20:00Z", isDraft = true)
    private val market = NaviPlace("강릉중앙시장", "강릉시 금성로", 37.7545, 128.8990, "12345", "TMAP")
    private val farAway = NaviPlace("안목해변", null, 37.7720, 128.9470, "999", "TMAP")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(places: FakePlaceRepository = FakePlaceRepository(drafts = listOf(draft))) =
        PlaceInputViewModel(
            tripRepository = FakeTripRepository(current = trip),
            placeRepository = places,
            codeRepository = FakeCodeRepository(),
            naviRepository = FakeNaviRepository(candidates = listOf(farAway, market)),
        )

    @Test
    fun `열면 첫 임시 기록과 근처 내비 장소 후보만`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()

        assertEquals(draft, vm.uiState.value.draft)
        assertEquals(listOf(market), vm.uiState.value.candidates)
        assertFalse(vm.uiState.value.canSave) // 이름이 아직 없음
    }

    @Test
    fun `후보를 누르면 이름이 채워지고 이름은 고칠 수 있음`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()

        vm.onSelectCandidate(market)
        assertEquals("강릉중앙시장", vm.uiState.value.placeName)

        vm.onNameChange("중앙시장")
        assertEquals("중앙시장", vm.uiState.value.placeName)
        assertEquals(market, vm.uiState.value.selectedCandidate)
    }

    @Test
    fun `카테고리를 고르면 그 유형만 보이고 바꾸면 유형 선택이 지워짐`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.prepare()
        runCurrent()

        vm.onSelectCategory("NATURE")
        assertEquals(listOf("BEACH", "MOUNTAIN"), vm.uiState.value.visibleTypes.map { it.code })
        vm.onSelectType("BEACH")

        vm.onSelectCategory("MOBILITY")
        assertNull(vm.uiState.value.selectedType)
    }

    @Test
    fun `저장 — 사용자 이름 · 유형 · 후기 · 태그 · 내비 장소를 보내고 닫힘`() = runTest(dispatcher) {
        val places = FakePlaceRepository(drafts = listOf(draft))
        val vm = viewModel(places)
        vm.prepare()
        runCurrent()
        vm.onSelectCandidate(market)
        vm.onNameChange("중앙시장")
        vm.onSelectCategory("MOBILITY")
        vm.onSelectType("PARKING")
        vm.onSelectPolarity("POSITIVE")
        vm.onToggleTag(5L)
        vm.onMemoChange("  지하 2층 여유  ")

        vm.onSave()
        runCurrent()

        val (no, input) = places.completed.single()
        assertEquals(10L, no)
        assertEquals("중앙시장", input.userPlaceName)
        assertEquals("PARKING", input.placeTypeCode)
        assertEquals("POSITIVE", input.reviewPolarityCode)
        assertEquals(listOf(5L), input.tagNos)
        assertEquals("지하 2층 여유", input.memo)
        assertEquals(market, input.naviPlace)
        assertTrue(vm.uiState.value.closed)
    }

    @Test
    fun `나중에 입력 — 확인 뒤 저장 없이 닫힘`() = runTest(dispatcher) {
        val places = FakePlaceRepository(drafts = listOf(draft))
        val vm = viewModel(places)
        vm.prepare()
        runCurrent()

        vm.onLater()
        assertTrue(vm.uiState.value.showLeaveConfirm)
        vm.onKeepDraft()

        assertTrue(vm.uiState.value.closed)
        assertTrue(places.completed.isEmpty())
    }
}
