package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.testing.unfinishedInputRepositoryOf
import com.example.travelcrumbs.testing.FakeTagRepository
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.RatingRecords
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakePlaceStatsRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.guideTrackerOf
import com.example.travelcrumbs.testing.pinnedPlacesRepositoryOf
import com.example.travelcrumbs.testing.ratingRepositoryOf
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        recording: RecordingRepository = recordingRepositoryOf(FakeTimeSource()),
        navi: FakeNaviRepository = FakeNaviRepository(),
        gleo: FakeGleoActionRepository = FakeGleoActionRepository(),
        ratings: RatingRepository = ratingRepositoryOf(),
        saved: SavedStateHandle = SavedStateHandle(),
    ) = HomeViewModel(
        recordingRepository = recording,
        recordPlace = FakeRecordPlaceUseCase(),
        drivingSignalRepository = FakeDrivingSignalRepository(DrivingState.PARKED),
        gleoActionRepository = gleo,
        naviRepository = navi,
        placeCatalogRepository = FakePlaceCatalogRepository(),
        pinnedPlacesRepository = pinnedPlacesRepositoryOf(),
        guideRequests = guideTrackerOf(navi, FakePinRepository()),
        placeStatsRepository = FakePlaceStatsRepository(),
        demoFeatures = DemoFeatures(dummyRoute = true),
        ratingRepository = ratings,
        recommendationRepository = SeedRecommendationRepository(),
        timeSource = FakeTimeSource(),
        savedStateHandle = saved,
        userRepository = FakeCurrentUserRepository(),
        tripRepository = FakeTripRepository(),
        tagRepository = FakeTagRepository(),
        unfinishedInputRepository = unfinishedInputRepositoryOf(FakeTimeSource()),
        offlineMap = com.example.travelcrumbs.testing.FakeOfflineMapRepository(),
    )

    @Test
    fun `R22 안내만 받음(중간 정차 · 기록 없음)이면 팝업 없음 · 여행 종료 뒤 팝업 · 작성자 · 여행명 · 태그 칩`() = runTest(dispatcher) {
        val ratings = ratingRepositoryOf()
        val vm = viewModel(ratings = ratings)
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        assertNull(vm.uiState.value.ratingPrompt)

        ratings.recordGuidance(101, "속초 가족여행", "민지네")
        runCurrent()
        assertNull(vm.uiState.value.ratingPrompt)

        ratings.markGuidedTripsEnded(endedAt = 10)
        runCurrent()

        val prompt = vm.uiState.value.ratingPrompt!!
        assertEquals(101L, prompt.tripNo)
        assertEquals("속초 가족여행", prompt.tripNm)
        assertEquals("민지네", prompt.nicknameNm)
        assertNull(prompt.ratingScore)
        assertFalse(prompt.canSubmit)
        assertEquals(5, prompt.tags.size)
    }

    @Test
    fun `R23 팝업은 로드맵마다 한 번 — 나중에 누르면 다시 안 뜸 · 다른 로드맵은 뜸`() = runTest(dispatcher) {
        val ratings = ratingRepositoryOf()
        ratings.recordGuidance(101, "속초 가족여행", "민지네")
        ratings.markGuidedTripsEnded(endedAt = 10)
        val vm = viewModel(ratings = ratings)
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        assertNotNull(vm.uiState.value.ratingPrompt)

        vm.dismissRatingPrompt()
        runCurrent()
        assertNull(vm.uiState.value.ratingPrompt)

        // 앱을 다시 열어도(새 ViewModel) 같은 로드맵은 안 뜸
        val again = viewModel(ratings = ratings)
        backgroundScope.launch { again.uiState.collect { } }
        runCurrent()
        assertNull(again.uiState.value.ratingPrompt)

        ratings.recordGuidance(102, "바다 보고 시장 한 바퀴", "바다곰")
        runCurrent()
        assertNull(again.uiState.value.ratingPrompt)
        ratings.markGuidedTripsEnded(endedAt = 10)
        runCurrent()
        assertEquals(102L, again.uiState.value.ratingPrompt?.tripNo)
    }

    @Test
    fun `R24 별 5 · 태그 고르고 별점 주기 → 저장 · 팝업 닫힘 · 남겼어요 안내 · 다시 안 뜸`() = runTest(dispatcher) {
        val ratings = ratingRepositoryOf()
        ratings.recordGuidance(101, "속초 가족여행", "민지네")
        ratings.markGuidedTripsEnded(endedAt = 10)
        val vm = viewModel(ratings = ratings)
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()

        vm.selectRatingScore(5)
        vm.toggleRatingTag(1)
        vm.toggleRatingTag(3)
        vm.toggleRatingTag(3)
        vm.toggleRatingTag(3)
        runCurrent()
        val prompt = vm.uiState.value.ratingPrompt!!
        assertEquals(5, prompt.ratingScore)
        assertEquals(setOf(1L, 3L), prompt.tags.filter { it.selected }.map { it.tagNo }.toSet())
        assertTrue(prompt.canSubmit)

        vm.submitRating()
        runCurrent()

        assertNull(vm.uiState.value.ratingPrompt)
        assertEquals(UiMessage.RATING_SAVED, vm.uiState.value.message)
        var saved: RatingRecords? = null
        backgroundScope.launch { ratings.records.collect { saved = it } }
        runCurrent()
        val mine = saved!!.ratingOf(101)!!
        assertEquals(5, mine.ratingScore)
        assertEquals(setOf(1L, 3L), mine.tagNos)
    }

    @Test
    fun `V18 주행 화면에서 종료하면 기록 칸도 완료 — 같은 출처 · 진행 중 아님 모양`() = runTest(dispatcher) {
        val recording = recordingRepositoryOf(FakeTimeSource())
        val vm = viewModel(recording = recording)
        subscribe(vm)

        recording.endTrip()
        runCurrent()

        assertEquals(TripStatus.COMPLETED, vm.uiState.value.tripStatus)
        assertFalse(vm.uiState.value.tripInProgress)
    }

    @Test
    fun `V34 V70 U3-1 완료 뒤 여행 시작 — A-01 이 열림 · 시작되면 닫히고 기록 중 · 0분 · 장소 0 · 진행 중 모양`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val recording = recordingRepositoryOf(time)
        val vm = viewModel(recording = recording)
        subscribe(vm)
        recording.endTrip()
        time.now += 30 * 60_000L

        vm.onStartTrip()
        runCurrent()
        assertTrue(vm.uiState.value.tripStartOpen)
        assertFalse(vm.uiState.value.tripInProgress)

        recording.startNewTrip()
        vm.onTripStartDone(null)
        runCurrent()
        assertFalse(vm.uiState.value.tripStartOpen)

        val state = vm.uiState.value
        assertEquals(TripStatus.RECORDING, state.tripStatus)
        assertEquals(0L, state.elapsedMinutes)
        assertEquals(0, state.placeCount)
        assertTrue(state.tripInProgress)
    }

    @Test
    fun `V35 기록 중 — 진행 중 모양 · 여행 시작이 불려도 상태 그대로`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val vm = viewModel(recording = recordingRepositoryOf(time))
        subscribe(vm)
        time.now += 5 * 60_000L

        assertTrue(vm.uiState.value.tripInProgress)
        vm.onStartTrip()
        runCurrent()

        assertEquals(TripStatus.RECORDING, vm.uiState.value.tripStatus)
    }

    @Test
    fun `V56 V35 주행 화면에서 일시정지하면 기록 칸도 일시정지 — 경과 같은 값 · 진행 중 모양`() = runTest(dispatcher) {
        val time = FakeTimeSource()
        val recording = recordingRepositoryOf(time)
        val vm = viewModel(recording = recording)
        subscribe(vm)
        time.now += 12 * 60_000L

        recording.togglePause()
        runCurrent()
        time.now += 30 * 60_000L
        vm.onStartTrip()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(TripStatus.PAUSED, state.tripStatus)
        assertEquals(12L, state.elapsedMinutes)
        assertTrue(state.tripInProgress)
    }

    @Test
    fun `T14 홈의 이 장소 기록도 같은 연결 지점 — 준비 중 안내`() = runTest(dispatcher) {
        val vm = viewModel()
        subscribe(vm)

        vm.onRecordPlace()
        runCurrent()

        assertEquals(UiMessage.NOT_READY, vm.uiState.value.message)
    }

    @Test
    fun `T34 SDK 카드 — 기어 판정 · 내비 조회 · Gleo 값이 uiState 에 남음`() = runTest(dispatcher) {
        val vm = viewModel(
            navi = FakeNaviRepository(
                result = Result.success(
                    NaviRouteState(routing = true, mode = "EV", routeOption = "FAST", termsAgreed = true),
                ),
            ),
            gleo = FakeGleoActionRepository(opened = true),
        )
        subscribe(vm)

        vm.refreshNavi()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(DrivingState.PARKED, state.drivingState)
        assertTrue(state.navi is SdkStatus.Ready)
        assertTrue(state.openedByGleoAction)
    }

    private fun TestScope.subscribe(vm: HomeViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        testScheduler.runCurrent()
    }

    @Test
    fun `R30 고르는 중 값(별 · 태그)은 프로세스 재생성 뒤에도 그대로`() = runTest(dispatcher) {
        val ratings = ratingRepositoryOf()
        ratings.recordGuidance(101, "속초 가족여행", "민지네")
        ratings.markGuidedTripsEnded(endedAt = 10)
        val saved = SavedStateHandle()
        val vm = viewModel(ratings = ratings, saved = saved)
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        vm.selectRatingScore(4)
        runCurrent()
        vm.toggleRatingTag(2)
        runCurrent()

        val again = viewModel(ratings = ratings, saved = saved)
        backgroundScope.launch { again.uiState.collect { } }
        runCurrent()
        val prompt = again.uiState.value.ratingPrompt!!
        assertEquals(4, prompt.ratingScore)
        assertEquals(listOf(2L), prompt.tags.filter { it.selected }.map { it.tagNo })
    }
}
