package com.example.travelcrumbs.ui.recommendations

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.testing.FakeOfflineMapRepository
import com.example.travelcrumbs.testing.FakeRouteGuidanceRequester
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.ratingRepositoryOf
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

/** 기록 칸 근처 로드맵 카드로 연 「추천」 칸(#55 · 스펙 home-nearby 3-4 · H13 · H29 · H30 · H33 · H34). */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsFromHomeTest {

    private val dispatcher = StandardTestDispatcher()
    private val ratings = ratingRepositoryOf(FakeTimeSource())
    private lateinit var vm: RecommendationsViewModel
    private val state get() = vm.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.start() {
        vm = RecommendationsViewModel(
            repository = SeedRecommendationRepository(),
            requestRouteGuidance = RequestRouteGuidanceUseCase(FakeRouteGuidanceRequester()),
            savedStateHandle = SavedStateHandle(),
            ratingRepository = ratings,
            timeSource = FakeTimeSource(),
            offlineMap = FakeOfflineMapRepository(),
        )
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
    }

    @Test
    fun `H29 카드 102 로 들어옴 — B-04 그 로드맵 · 뒤로는 칸을 떠남`() = runTest(dispatcher) {
        start()
        vm.onEntry(1, openTripNo = 102L, openSidoNm = "강원")
        runCurrent()

        assertEquals(RecommendationPage.DETAIL, state.page)
        assertEquals(102L, state.detail?.tripNo)
        assertEquals(9, state.detail?.ratingCnt)
        assertTrue(state.backLeavesTab)
    }

    @Test
    fun `H13 B-04 의 평균 · 참여 수가 카드와 같은 값(내 별점 포함)`() = runTest(dispatcher) {
        start()
        ratings.recordGuidance(102L, "바다 보고 시장 한 바퀴", "바다곰")
        ratings.saveRating(102L, 5, emptySet())
        vm.onEntry(1, openTripNo = 102L, openSidoNm = "강원")
        runCurrent()

        assertEquals(10, state.detail?.ratingCnt)
        assertEquals((4.6 * 9 + 5) / 10, state.detail!!.ratingAvg, 1e-9)
    }

    @Test
    fun `H31 B-05 팝업이 떠 있으면 뒤로는 팝업부터(칸을 떠나지 않음)`() = runTest(dispatcher) {
        start()
        vm.onEntry(1, openTripNo = 101L, openSidoNm = "강원")
        runCurrent()
        vm.askGuidanceFromFirst()
        runCurrent()
        assertFalse(state.backLeavesTab)
        vm.onBack()
        runCurrent()
        assertNull(state.confirm)
        assertTrue(state.backLeavesTab)
    }

    @Test
    fun `H33 레일 추천으로 다시 들어옴 — B-01 부터 · 칸을 떠나는 뒤로 아님`() = runTest(dispatcher) {
        start()
        vm.onEntry(1, openTripNo = 101L, openSidoNm = "강원")
        runCurrent()
        vm.onEntry(2)
        runCurrent()

        assertEquals(RecommendationPage.REGION, state.page)
        assertFalse(state.backLeavesTab)
        assertFalse(state.handlesBack)
    }

    @Test
    fun `H34 카드의 로드맵을 찾지 못함 — B-01`() = runTest(dispatcher) {
        start()
        vm.onEntry(1, openTripNo = 999_999L, openSidoNm = "강원")
        runCurrent()

        assertEquals(RecommendationPage.REGION, state.page)
        assertFalse(state.backLeavesTab)
    }

    @Test
    fun `H30 B-02 에서 연 B-04 는 기존대로 뒤로 = B-02(칸을 떠나지 않음)`() = runTest(dispatcher) {
        start()
        vm.selectSido("강원")
        runCurrent()
        vm.selectRegion("속초시")
        runCurrent()
        vm.openTrip(101L)
        runCurrent()
        assertFalse(state.backLeavesTab)
        vm.onBack()
        runCurrent()
        assertEquals(RecommendationPage.RANKING, state.page)
    }
}
