package com.example.travelcrumbs.ui.recommendations

import com.example.travelcrumbs.testing.FakeTimeSource
import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.testing.FakeRouteGuidanceRequester
import com.example.travelcrumbs.testing.ratingRepositoryOf
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

/** 「추천」 칸 ViewModel(#37) — 시드 저장소 그대로 · 길안내 포트는 fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val guidance = FakeRouteGuidanceRequester()
    private val ratings = ratingRepositoryOf()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) = RecommendationsViewModel(
        repository = SeedRecommendationRepository(),
        requestRouteGuidance = RequestRouteGuidanceUseCase(guidance),
        savedStateHandle = saved,
        ratingRepository = ratings,
        timeSource = FakeTimeSource(),
    )

    private val state get() = vm.uiState.value
    private lateinit var vm: RecommendationsViewModel

    private fun TestScope.start(saved: SavedStateHandle = SavedStateHandle()) {
        vm = viewModel(saved)
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.runCurrent()
    }

    private fun TestScope.toSokcho() {
        vm.selectSido("강원")
        runCurrent()
        vm.selectRegion("속초시")
        runCurrent()
    }

    @Test
    fun `R1 처음 — B-01 · 시도 10개 · 시군구 카드 없음 · 시연용 데이터 표시`() = runTest(dispatcher) {
        start()

        assertEquals(RecommendationPage.REGION, state.page)
        assertEquals(com.example.travelcrumbs.domain.model.SidoNames.ALL, state.sidoNms)
        assertNull(state.selectedSidoNm)
        assertTrue(state.sigunguCards.isEmpty())
        assertTrue(state.demoData)
        assertFalse(state.handlesBack)
    }

    @Test
    fun `R3 강원 — 시군구 카드 이름 · 로드맵 수(시드에서 셈) · 마지막 전체`() = runTest(dispatcher) {
        start()
        vm.selectSido("강원")
        runCurrent()

        assertEquals(
            listOf("속초시" to 3, "강릉시" to 2, "양양군" to 1, "춘천시" to 1, "평창군" to 1, null to 8),
            state.sigunguCards.map { it.sigunguNm to it.tripCnt },
        )
    }

    @Test
    fun `R2 R5 속초시 → B-02 별점순 랭킹 · 순위 · 닉네임 · 장소 수 · 대표 태그`() = runTest(dispatcher) {
        start()
        toSokcho()

        assertEquals(RecommendationPage.RANKING, state.page)
        assertEquals("속초시", state.regionSigunguNm)
        val first = state.rankedTrips.first()
        assertEquals(listOf("속초 가족여행", "바다 보고 시장 한 바퀴", "설악산 아래 하루"), state.rankedTrips.map { it.tripNm })
        assertEquals(listOf(1, 2, 3), state.rankedTrips.map { it.rank })
        assertEquals("민지네", first.nicknameNm)
        assertEquals(4.8, first.ratingAvg, 0.0)
        assertEquals(12, first.ratingCnt)
        assertEquals(4, first.placeCnt)
        assertEquals("아이와 가기 좋아요", first.representativeTagNm)
    }

    @Test
    fun `R6 R7 최신순 · 동행자 필터`() = runTest(dispatcher) {
        start()
        toSokcho()

        vm.selectSort(RankingSort.LATEST)
        runCurrent()
        assertEquals("바다 보고 시장 한 바퀴", state.rankedTrips.first().tripNm)

        vm.selectSort(RankingSort.RATING)
        vm.selectCompanion(CompanionType.CHILD)
        runCurrent()
        assertEquals(listOf("속초 가족여행", "설악산 아래 하루"), state.rankedTrips.map { it.tripNm })

        vm.selectCompanion(CompanionType.SOLO)
        runCurrent()
        assertTrue(state.rankedTrips.isEmpty())
    }

    @Test
    fun `R8 R9 태그별로 보기 — 많은 순 막대 · 처음엔 1위 태그 · 태그 고르면 그 로드맵만`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTags()
        runCurrent()

        assertEquals(RecommendationPage.TAGS, state.page)
        assertEquals(3, state.regionTripCnt)
        val counts = state.tagCounts.map { it.cnt }
        assertEquals(counts.sortedDescending(), counts)
        assertEquals(state.tagCounts.first().tagNo, state.selectedTagNo)

        val kids = state.tagCounts.first { it.tagNm == "아이와 가기 좋아요" }
        vm.selectTag(kids.tagNo)
        runCurrent()
        assertEquals("아이와 가기 좋아요", state.selectedTagNm)
        assertEquals(listOf("속초 가족여행", "설악산 아래 하루"), state.taggedTrips.map { it.tripNm })
    }

    @Test
    fun `R12 로드맵 카드 → B-04 상세 · 장소 순서 · 장소별 태그 · 경로 그림 점 수`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTrip(101)
        runCurrent()

        val detail = state.detail!!
        assertEquals(RecommendationPage.DETAIL, state.page)
        assertEquals("속초 가족여행", detail.tripNm)
        assertEquals(listOf(CompanionType.SPOUSE, CompanionType.CHILD), detail.companionTypes)
        assertEquals(listOf(1, 2, 3, 4), detail.places.map { it.placeOrd })
        assertEquals("속초 중앙시장 공영주차장", detail.places.first().placeNm)
        assertEquals(listOf("주차 편해요", "화장실 깨끗해요"), detail.places.first().tagNms)
        assertEquals(4, detail.sketch.size)
    }

    @Test
    fun `R13 첫 장소부터 안내 → B-05 팝업 → 안내 시작 → 길안내 포트 한 번 · 첫 장소 · 요청 안내 한 줄`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTrip(101)
        runCurrent()

        vm.askGuidanceFromFirst()
        runCurrent()
        val confirm = state.confirm!!
        assertEquals("속초 중앙시장 공영주차장", confirm.placeNm)
        assertEquals(1, confirm.placeOrd)
        assertEquals("민지네", confirm.nicknameNm)
        assertTrue(guidance.requested.isEmpty())

        vm.startGuidance()
        vm.startGuidance() // 두 번 눌러도 한 번
        runCurrent()

        assertEquals(1, guidance.requested.size)
        val sent = guidance.requested.single()
        assertEquals(1001L, sent.placeNo)
        assertEquals("속초 중앙시장 공영주차장", sent.placeNm)
        assertEquals(38.2049, sent.lat, 0.0)
        assertNull(state.confirm)
        assertEquals(UiMessage.GUIDANCE_REQUESTED, state.message)

        vm.onMessageShown()
        runCurrent()
        assertNull(state.message)
    }

    @Test
    fun `R13 여기로 안내 → 그 장소 · 취소면 요청 없음 · 실패면 시작 못 함 안내`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTrip(101)
        runCurrent()

        vm.askGuidance(1003)
        runCurrent()
        assertEquals("영랑호 산책로", state.confirm!!.placeNm)
        assertEquals(3, state.confirm!!.placeOrd)
        vm.dismissGuidance()
        runCurrent()
        assertNull(state.confirm)
        assertTrue(guidance.requested.isEmpty())

        guidance.result = GuidanceRequestResult.Failed
        vm.askGuidance(1003)
        vm.startGuidance()
        runCurrent()
        assertEquals(1003L, guidance.requested.single().placeNo)
        assertEquals(UiMessage.GUIDANCE_FAILED, state.message)
    }

    @Test
    fun `R14 시스템 뒤로 — 팝업 → 상세 → 온 화면(B-03) → B-02 → B-01 · B-01 에선 받지 않음`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTags()
        vm.openTrip(101)
        vm.askGuidanceFromFirst()
        runCurrent()
        assertTrue(state.handlesBack)

        vm.onBack(); runCurrent()
        assertNull(state.confirm)
        assertEquals(RecommendationPage.DETAIL, state.page)
        vm.onBack(); runCurrent()
        assertEquals(RecommendationPage.TAGS, state.page)
        vm.onBack(); runCurrent()
        assertEquals(RecommendationPage.RANKING, state.page)
        vm.onBack(); runCurrent()
        assertEquals(RecommendationPage.REGION, state.page)
        assertFalse(state.handlesBack)
    }

    @Test
    fun `R16 프로세스 재생성 — 고른 화면 · 지역 · 필터 · 로드맵 복원`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        start(saved)
        toSokcho()
        vm.selectCompanion(CompanionType.SPOUSE)
        vm.openTrip(102)
        runCurrent()

        start(saved)

        assertEquals(RecommendationPage.DETAIL, state.page)
        assertEquals("바다 보고 시장 한 바퀴", state.detail!!.tripNm)
        assertEquals(CompanionType.SPOUSE, state.companion)
        assertEquals("속초시", state.regionSigunguNm)
    }

    private fun TestScope.guideTrip101() {
        toSokcho()
        vm.openTrip(101)
        runCurrent()
        vm.askGuidanceFromFirst()
        runCurrent()
        vm.startGuidance()
        runCurrent()
    }

    @Test
    fun `R26 안내 요청 성공 → 별점 자격 · B-04 별점 주기 → 별 5 · 태그 → 평균 · 참여 수 · 정렬 · 태그 수 바로 반영`() = runTest(dispatcher) {
        start()
        toSokcho()
        vm.openTrip(101)
        runCurrent()
        assertFalse(state.detail!!.canRate) // 안내 기록 없음 → 버튼 없음
        vm.openRating()
        runCurrent()
        assertNull(state.rating)
        val kidsBefore = state.tagCounts.first { it.tagNo == 1L }.cnt

        vm.askGuidanceFromFirst()
        runCurrent()
        vm.startGuidance()
        runCurrent()
        assertTrue(state.detail!!.canRate)
        assertNull(state.detail!!.myRatingScore)

        vm.openRating()
        runCurrent()
        val dialog = state.rating!!
        assertEquals("민지네", dialog.nicknameNm)
        assertFalse(dialog.canSubmit)
        assertTrue(state.handlesBack)

        vm.selectRatingScore(5)
        vm.toggleRatingTag(1)
        runCurrent()
        vm.submitRating()
        runCurrent()

        assertNull(state.rating)
        assertEquals(UiMessage.RATING_SAVED, state.message)
        val detail = state.detail!!
        assertEquals(13, detail.ratingCnt)
        assertEquals((4.8 * 12 + 5) / 13, detail.ratingAvg, 1e-9)
        assertEquals(5, detail.myRatingScore)
        assertEquals(kidsBefore + 1, state.tagCounts.first { it.tagNo == 1L }.cnt)

        vm.onBack()
        runCurrent()
        val card = state.rankedTrips.first { it.tripNo == 101L }
        assertEquals(13, card.ratingCnt)
        assertEquals(1, card.rank)
    }

    @Test
    fun `R27 안내 요청 실패면 별점 자격 없음 — B-04 버튼 없음`() = runTest(dispatcher) {
        guidance.result = GuidanceRequestResult.Failed
        start()
        guideTrip101()

        assertEquals(UiMessage.GUIDANCE_FAILED, state.message)
        assertFalse(state.detail!!.canRate)
        vm.openRating()
        runCurrent()
        assertNull(state.rating)
    }

    @Test
    fun `R28 고치기 — 내 별점으로 채워 열림 · 다시 주면 바뀜 · 참여 수는 그대로 · 나중에는 닫기만`() = runTest(dispatcher) {
        start()
        guideTrip101()
        vm.openRating()
        runCurrent()
        vm.selectRatingScore(5)
        vm.toggleRatingTag(3)
        runCurrent()
        vm.submitRating()
        runCurrent()

        vm.openRating()
        runCurrent()
        val reopened = state.rating!!
        assertEquals(5, reopened.ratingScore)
        assertEquals(listOf(3L), reopened.tags.filter { it.selected }.map { it.tagNo })

        vm.dismissRating()
        runCurrent()
        assertNull(state.rating)
        assertEquals(5, state.detail!!.myRatingScore)

        vm.openRating()
        runCurrent()
        vm.selectRatingScore(1)
        runCurrent()
        vm.submitRating()
        runCurrent()

        val detail = state.detail!!
        assertEquals(1, detail.myRatingScore)
        assertEquals(13, detail.ratingCnt)
        assertEquals((4.8 * 12 + 1) / 13, detail.ratingAvg, 1e-9)
    }
}
