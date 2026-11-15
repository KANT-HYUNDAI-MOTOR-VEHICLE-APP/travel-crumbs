package com.example.travelcrumbs.ui.home

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recommendation.LocalRecommendationRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NearbySectionState
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeOfflineMapRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakePlaceCatalogRepository
import com.example.travelcrumbs.testing.FakePlaceStatsRepository
import com.example.travelcrumbs.testing.FakeRecordPlaceUseCase
import com.example.travelcrumbs.testing.FakeTagRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.completedTrip
import com.example.travelcrumbs.testing.guideTrackerOf
import com.example.travelcrumbs.testing.idleRecordingRepositoryOf
import com.example.travelcrumbs.testing.pinnedPlacesRepositoryOf
import com.example.travelcrumbs.testing.ratingRepositoryOf
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.testing.unfinishedInputRepositoryOf
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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

/**
 * 기록 칸 「내 근처 리뷰 많은 로드맵」 논리 상태 · 전이(#55 · 스펙 home-nearby 3-1 · 3-2 T1 ~ T19 · fake 저장소).
 * 따로 적지 않으면 근처 위치 동의(LOCATION_RECO) = 동의함(스펙 6절 전제).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeNearbyViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 현재 위치 조회 횟수 · 지연 · 오류를 재는 대역 — 나머지는 [FakeNaviRepository] 그대로. */
    private class CountingNavi(val inner: FakeNaviRepository) : NaviRepository by inner {
        var calls = 0
        var delayMs = 0L
        var error: Exception? = null

        override suspend fun currentLocation(): CurrentLocation? {
            calls++
            delay(delayMs)
            error?.let { throw it }
            return inner.currentLocation()
        }
    }

    private enum class Consent { NONE, AGREED, DECLINED }

    private inner class Env(
        driving: DrivingState = DrivingState.PARKED,
        tripInProgress: Boolean = false,
        consent: Consent = Consent.AGREED,
        naviReady: Boolean = true,
        /** 참이면 추천 = 시드 + 기기 안 내 공개 여행(묶음 변화 시험) · 거짓이면 시드만. */
        localTrips: Boolean = false,
        val savedState: SavedStateHandle = SavedStateHandle(),
    ) {
        val time = FakeTimeSource()
        val drivingRepo = FakeDrivingSignalRepository(driving)
        val navi = CountingNavi(FakeNaviRepository(ready = naviReady))
        val users = FakeCurrentUserRepository(time = time)
        val trips = FakeTripRepository(time = time)
        val tags = FakeTagRepository()
        val catalog = FakePlaceCatalogRepository()
        val ratings = ratingRepositoryOf(time)
        val recording = if (tripInProgress) recordingRepositoryOf(time) else idleRecordingRepositoryOf(time)
        val recommendations: RecommendationRepository =
            if (localTrips) LocalRecommendationRepository(trips, users, tags, catalog) else SeedRecommendationRepository()

        init {
            when (consent) {
                Consent.NONE -> Unit
                Consent.AGREED -> runBlocking { users.recordConsent(ConsentType.LOCATION_RECO, true) }
                Consent.DECLINED -> runBlocking { users.recordConsent(ConsentType.LOCATION_RECO, false) }
            }
        }

        val vm = HomeViewModel(
            recordingRepository = recording,
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = drivingRepo,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = navi,
            placeCatalogRepository = catalog,
            pinnedPlacesRepository = pinnedPlacesRepositoryOf(),
            guideRequests = guideTrackerOf(navi.inner, FakePinRepository()),
            placeStatsRepository = FakePlaceStatsRepository(),
            demoFeatures = DemoFeatures(dummyRoute = false),
            ratingRepository = ratings,
            recommendationRepository = recommendations,
            timeSource = time,
            savedStateHandle = savedState,
            userRepository = users,
            tripRepository = trips,
            tagRepository = tags,
            unfinishedInputRepository = unfinishedInputRepositoryOf(time),
            offlineMap = FakeOfflineMapRepository(),
            endTrip = com.example.travelcrumbs.testing.endTripOf(recording),
        )

        val section: NearbyRoadmapsSection get() = vm.uiState.value.nearby
        val state: NearbySectionState get() = section.state

        /** 내 별점 하나(안내 기록 → 별점 · 앱 흐름 그대로). */
        suspend fun rate(tripNo: Long, score: Int = 5) {
            ratings.recordGuidance(tripNo, "로드맵", "닉")
            ratings.saveRating(tripNo, score, emptySet())
        }

        /** 기기 안 공개 여행 하나(검토 끝 · 속초 부근 첫 장소) — 추천 번호 = 오프셋 + trip_no. */
        suspend fun publish(tripNo: Long, places: Int = 2) {
            // 검토를 먼저 끝냄 — 검토 대기 여행이 있으면 A-05 가 기록 칸을 차지해 자격 없음(앱 흐름도 검토 → 공개)
            trips.markReviewed(tripNo)
            trips.put(completedTrip(tripNo = tripNo, places = places, visibility = Visibility.ANONYMOUS_PUBLIC))
        }
    }

    private fun TestScope.shown(env: Env) {
        backgroundScope.launch { env.vm.uiState.collect { } }
        runCurrent()
        env.vm.onScreenResumed()
        runCurrent()
    }

    private fun TestScope.reshow(env: Env) {
        env.vm.onScreenPaused()
        runCurrent()
        env.vm.onScreenResumed()
        runCurrent()
    }

    @Test
    fun `H1 정차 · 여행 없음 · 속초 위치 — 위치 확인 중 뒤 근처 있음 · 101 102 103 · 순위 1 = 리뷰 가장 많은 것`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 1_000
        shown(env)
        assertEquals(NearbySectionState.LOCATING, env.state)
        assertTrue(env.section.cards.isEmpty())

        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(listOf(101L, 102L, 103L), env.section.cards.map { it.tripNo })
        assertEquals(listOf(1, 2, 3), env.section.cards.map { it.rank })
        assertEquals(41, env.section.cards.first().ratingCnt)
        assertEquals(AppConfig.NEARBY_ROADMAP_RADIUS_KM, env.section.radiusKm)
        assertEquals("강원", env.section.cards.first().sidoNm)
        assertEquals("속초시", env.section.cards.first().sigunguNm)
        assertEquals(1, env.navi.calls)
    }

    @Test
    fun `H10 반경 안 0개면 전국 대체 · 최대 10장 · 거리 보임`() = runTest(dispatcher) {
        val env = Env()
        env.navi.inner.location = CurrentLocation(33.0, 124.0, "바다")
        shown(env)
        assertEquals(NearbySectionState.NATIONWIDE, env.state)
        assertEquals(AppConfig.NEARBY_ROADMAP_MAX_CNT, env.section.cards.size)
        assertTrue(env.section.cards.all { it.distM > AppConfig.NEARBY_ROADMAP_RADIUS_KM * 1_000 })
    }

    @Test
    fun `H13 H14 T16 화면이 앞인 채 내 별점 저장 — 위치를 다시 묻지 않고 바뀐 리뷰 수 · 평균으로`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(listOf(41, 9, 6), env.section.cards.map { it.ratingCnt })

        env.rate(102L)
        env.rate(103L)
        runCurrent()
        val cards = env.section.cards
        assertEquals(listOf(101L, 102L, 103L), cards.map { it.tripNo })
        assertEquals(listOf(41, 10, 7), cards.map { it.ratingCnt })
        assertEquals((4.6 * 9 + 5) / 10, cards[1].ratingAvg, 1e-9)
        assertEquals(1, env.navi.calls)
    }

    @Test
    fun `H15 내비 연결 안 됨 — 조회 즉시 실패 · 숨김(위치 못 얻음)`() = runTest(dispatcher) {
        val env = Env(naviReady = false)
        shown(env)
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
        assertTrue(env.state.visible)
        assertEquals(0, env.navi.calls)
    }

    @Test
    fun `H16 현재 위치 조회 오류 — 위치 확인 중 뒤 위치 못 얻음`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 100
        env.navi.error = IllegalStateException("조회 오류")
        shown(env)
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(101)
        runCurrent()
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
    }

    @Test
    fun `H17 응답 한도 10초 안에 안 오면 한도 뒤 위치 못 얻음`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 60_000
        shown(env)
        advanceTimeBy(AppConfig.SDK_CALL_TIMEOUT_MS - 1)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
    }

    @Test
    fun `H18 T14 위치 못 얻음(연결 안 됨) 뒤 연결됨 — 위치 확인 중 · 조회 1회 · 결과`() = runTest(dispatcher) {
        val env = Env(naviReady = false)
        shown(env)
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
        env.navi.delayMs = 500
        env.navi.inner.setReady(true)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H19 H48 근처 위치 동의 이력 없음 — LOCATION_COLLECT 가 있어도 동의 카드 · 조회 0회`() = runTest(dispatcher) {
        val env = Env(consent = Consent.NONE)
        env.users.recordConsent(ConsentType.LOCATION_COLLECT, true)
        shown(env)
        assertEquals(NearbySectionState.CONSENT_NEEDED, env.state)
        assertTrue(env.state.visible)
        assertTrue(env.section.cards.isEmpty())
        assertEquals(0, env.navi.calls)
    }

    @Test
    fun `H20 H21 H54 주행 · 모름 — 자격 없음 · 동의 기록 0 · 위치 조회 0회`() = runTest(dispatcher) {
        for (driving in listOf(DrivingState.DRIVING, DrivingState.UNKNOWN)) {
            val env = Env(driving = driving, consent = Consent.NONE)
            shown(env)
            assertEquals(NearbySectionState.INELIGIBLE, env.state)
            env.vm.onAgreeNearby()
            runCurrent()
            assertTrue(env.users.history.isEmpty())
            assertEquals(0, env.navi.calls)
        }
    }

    @Test
    fun `H22 위치 확인 중 주행으로 바뀜 — 자격 없음 · 늦게 온 결과로 바뀌지 않음 · H23 정차면 다시 위치 확인 중`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 1_000
        shown(env)
        assertEquals(NearbySectionState.LOCATING, env.state)
        env.drivingRepo.emit(DrivingState.DRIVING)
        runCurrent()
        assertEquals(NearbySectionState.INELIGIBLE, env.state)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(NearbySectionState.INELIGIBLE, env.state)
        env.drivingRepo.emit(DrivingState.PARKED)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H23 주행 → 정차 — 위치 확인 중 · 조회 1회 · 결과`() = runTest(dispatcher) {
        val env = Env(driving = DrivingState.DRIVING)
        env.navi.delayMs = 500
        shown(env)
        assertEquals(0, env.navi.calls)
        env.drivingRepo.emit(DrivingState.PARKED)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H24 진행 중 여행 · 정차 — 자격 없음 · 위치 조회 0회`() = runTest(dispatcher) {
        val env = Env(tripInProgress = true)
        shown(env)
        assertEquals(NearbySectionState.INELIGIBLE, env.state)
        assertEquals(0, env.navi.calls)
        // 화면을 내리면 기록 경과 시계 구독도 놓음(시험 끝의 남은 일 처리가 끝나게)
        env.vm.onScreenPaused()
        runCurrent()
    }

    @Test
    fun `H25 T4 근처 있음 · 5분 지나 화면 다시 앞에 옴 — 카드 그대로 보인 채 조회 1회 · 위치 확인 중으로 안 돌아감`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        val before = env.section.cards
        env.vm.onScreenPaused()
        runCurrent()
        env.time.now += AppConfig.NEARBY_RELOOKUP_MIN_INTERVAL_MILLIS
        env.navi.delayMs = 1_000
        env.vm.onScreenResumed()
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(before, env.section.cards)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `N7 재진입 최소 간격 — 마지막 성공 5분 안 재진입은 조회 0회(카드 그대로) · 5분 지나면 1회`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(1, env.navi.calls)
        env.time.now += AppConfig.NEARBY_RELOOKUP_MIN_INTERVAL_MILLIS - 1
        reshow(env)
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(listOf(101L, 102L, 103L), env.section.cards.map { it.tripNo })

        env.time.now += 1
        reshow(env)
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `N7 동의 변화는 간격과 무관하게 바로 — 철회 뒤 다시 동의하면 5분 안이어도 위치 확인 중 · 조회 1회`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        env.users.recordConsent(ConsentType.LOCATION_RECO, false)
        runCurrent()
        env.navi.delayMs = 500
        env.users.recordConsent(ConsentType.LOCATION_RECO, true)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `T9 홈이 가려진 채 철회 — 메모리 거리 바로 지움(다시 동의하고 홈이 보이면 카드가 아닌 위치 확인 중부터)`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(NearbySectionState.NEARBY, env.state)
        env.vm.onScreenPaused()
        runCurrent()
        env.users.recordConsent(ConsentType.LOCATION_RECO, false)
        runCurrent()
        env.users.recordConsent(ConsentType.LOCATION_RECO, true)
        runCurrent()
        env.navi.delayMs = 500
        env.vm.onScreenResumed()
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        assertTrue(env.section.cards.isEmpty())
        advanceTimeBy(501)
        runCurrent()
        assertEquals(2, env.navi.calls)
    }

    @Test
    fun `H26 조회 진행 중 화면 다시 앞에 옴 — 앞 조회는 취소되고 한 번에 하나`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 1_000
        shown(env)
        reshow(env)
        advanceTimeBy(1_001)
        runCurrent()
        // T1(화면이 뒤로) 이 앞 조회를 취소 · T5 로 새 조회 1회 — 동시에 둘이 돌지 않음
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H27 백그라운드(화면 안 보임) — 시간이 지나도 위치 조회 0회`() = runTest(dispatcher) {
        val env = Env(driving = DrivingState.DRIVING)
        shown(env)
        env.vm.onScreenPaused()
        runCurrent()
        env.drivingRepo.emit(DrivingState.PARKED)
        advanceTimeBy(10 * 60_000)
        runCurrent()
        assertEquals(0, env.navi.calls)
        assertEquals(NearbySectionState.INELIGIBLE, env.state)
    }

    @Test
    fun `H35 H37 미리보기 · 시연용 데이터 — 지도 묶음이 없으면 경로 그림 점(0 ~ 1 상대 값) · 시연용 표시`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertTrue(env.section.demoData)
        env.section.cards.forEach { card ->
            assertEquals(null, card.routeMap)
            assertTrue(card.sketch.isNotEmpty())
            assertTrue(card.sketch.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test
    fun `H38 UiState · SavedStateHandle 에 위도 · 경도 · 주소 값 없음`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(NearbySectionState.NEARBY, env.state)
        val text = env.vm.uiState.value.toString()
        listOf("38.2049", "128.5906", "중앙로 147", "lat=", "lng=").forEach { assertFalse(it, text.contains(it)) }
        env.savedState.keys().forEach { key ->
            val value = env.savedState.get<Any?>(key).toString()
            assertFalse(key, value.contains("38.2049") || value.contains("128.5906"))
        }
        assertTrue(env.savedState.keys().none { it.contains("nearby") })
    }

    @Test
    fun `H38 H62 근처 로드맵 코드에 로그 · 저장 호출 없음(좌표 · 주소 · 거리를 남기지 않음)`() {
        val root = File("src/main/java/com/example/travelcrumbs")
        listOf(
            "ui/home/NearbyRoadmapsHolder.kt",
            "ui/home/components/NearbyRoadmapsArea.kt",
            "domain/NearbyRoadmapRules.kt",
            "domain/model/NearbySection.kt",
        ).forEach { path ->
            val source = File(root, path).readText()
            listOf("Log.", "println(", "savedStateHandle", "DataStore", "Dao", "File(").forEach { banned ->
                assertFalse("$path · $banned", source.contains(banned))
            }
        }
    }

    @Test
    fun `H41 프로세스 재생성 — 구역은 복원하지 않고 위치 확인 중부터 새로 계산 · 첫 카드부터`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val first = Env(savedState = saved)
        shown(first)
        assertEquals(NearbySectionState.NEARBY, first.state)

        val restored = Env(savedState = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))
        restored.navi.delayMs = 500
        shown(restored)
        assertEquals(NearbySectionState.LOCATING, restored.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(1, restored.section.cards.first().rank)
    }

    @Test
    fun `H44 T6 「동의하고 보기」 — LOCATION_RECO Y 행 1개 · 위치 확인 중 · 조회 정확히 1회 → 결과`() = runTest(dispatcher) {
        val env = Env(consent = Consent.NONE)
        env.navi.delayMs = 500
        shown(env)
        env.vm.onAgreeNearby()
        runCurrent()
        assertEquals(listOf(ConsentType.LOCATION_RECO to true), env.users.history)
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H45 T7 「괜찮아요」 — N 행 1개 · 숨김(동의 안 함) · 조회 0회 · 다시 보여도 · 재생성 뒤에도 동의 카드 안 뜸`() = runTest(dispatcher) {
        val env = Env(consent = Consent.NONE)
        shown(env)
        env.vm.onDeclineNearby()
        runCurrent()
        assertEquals(listOf(ConsentType.LOCATION_RECO to false), env.users.history)
        assertEquals(NearbySectionState.CONSENT_DECLINED, env.state)
        reshow(env)
        assertEquals(NearbySectionState.CONSENT_DECLINED, env.state)
        assertEquals(0, env.navi.calls)
    }

    @Test
    fun `H46 T9 T3 동의함 · 근처 있음 → 철회 — 동의 안 함 · 조회 0회 · 다시 동의하면 위치 확인 중부터`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(NearbySectionState.NEARBY, env.state)
        env.vm.onScreenPaused()
        runCurrent()
        env.users.recordConsent(ConsentType.LOCATION_RECO, false)
        env.vm.onScreenResumed()
        runCurrent()
        assertEquals(NearbySectionState.CONSENT_DECLINED, env.state)
        assertEquals(1, env.navi.calls)

        // 메모리 거리는 지워짐 — 다시 동의하면 카드가 아닌 위치 확인 중부터(T8 · T5)
        env.navi.delayMs = 500
        env.users.recordConsent(ConsentType.LOCATION_RECO, true)
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(2, env.navi.calls)
    }

    @Test
    fun `H47 T9 조회 진행 중 철회 — 동의 안 함 · 늦게 온 결과 버림`() = runTest(dispatcher) {
        val env = Env()
        env.navi.delayMs = 1_000
        shown(env)
        assertEquals(NearbySectionState.LOCATING, env.state)
        env.users.recordConsent(ConsentType.LOCATION_RECO, false)
        runCurrent()
        assertEquals(NearbySectionState.CONSENT_DECLINED, env.state)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(NearbySectionState.CONSENT_DECLINED, env.state)
        assertTrue(env.section.cards.isEmpty())
    }

    @Test
    fun `H49 LOCATION_COLLECT 철회 — 근처 위치 동의 · 구역 · 조회 그대로`() = runTest(dispatcher) {
        val env = Env()
        env.users.recordConsent(ConsentType.LOCATION_COLLECT, true)
        shown(env)
        env.users.recordConsent(ConsentType.LOCATION_COLLECT, false)
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(1, env.navi.calls)
    }

    @Test
    fun `H51 T8 설정에서 동의(동의 안 함 → Y) 뒤 기록 칸 — 위치 확인 중부터`() = runTest(dispatcher) {
        val env = Env(consent = Consent.DECLINED)
        env.navi.delayMs = 500
        shown(env)
        env.vm.onScreenPaused()
        runCurrent()
        env.users.recordConsent(ConsentType.LOCATION_RECO, true)
        env.vm.onScreenResumed()
        runCurrent()
        assertEquals(NearbySectionState.LOCATING, env.state)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H53 T1 T2 동의 카드 보는 중 주행 — 카드 사라짐 · 이력 행 0 · 정차로 돌아오면 다시 동의 카드`() = runTest(dispatcher) {
        val env = Env(consent = Consent.NONE)
        shown(env)
        assertEquals(NearbySectionState.CONSENT_NEEDED, env.state)
        env.drivingRepo.emit(DrivingState.DRIVING)
        runCurrent()
        assertEquals(NearbySectionState.INELIGIBLE, env.state)
        env.vm.onAgreeNearby()
        runCurrent()
        assertTrue(env.users.history.isEmpty())
        env.drivingRepo.emit(DrivingState.PARKED)
        runCurrent()
        assertEquals(NearbySectionState.CONSENT_NEEDED, env.state)
        assertEquals(0, env.navi.calls)
    }

    @Test
    fun `H56 T15 대상 없음 · 위치 못 얻음 아닌 상태에서 내비 연결 변화 — 조회 0회`() = runTest(dispatcher) {
        val env = Env()
        shown(env)
        assertEquals(NearbySectionState.NEARBY, env.state)
        env.navi.inner.setReady(false)
        runCurrent()
        env.navi.inner.setReady(true)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
    }

    @Test
    fun `H57 T18 근처 있음 · 내 공개 여행 추가(리뷰 1명 · 반경 안) — 카드 그대로 보인 채 조회 1회 → 새 로드맵 포함`() = runTest(dispatcher) {
        val env = Env(localTrips = true)
        shown(env)
        assertEquals(NearbySectionState.NEARBY, env.state)
        val mine = AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + 7
        env.rate(mine)
        runCurrent()
        assertEquals(1, env.navi.calls)

        env.navi.delayMs = 500
        env.publish(7)
        runCurrent()
        assertEquals(2, env.navi.calls)
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertFalse(env.section.cards.any { it.tripNo == mine })
        advanceTimeBy(501)
        runCurrent()
        assertTrue(env.section.cards.any { it.tripNo == mine })
    }

    @Test
    fun `H58 T18 공개 로드맵 장소 추가 · 비공개 전환 — 각각 조회 1회`() = runTest(dispatcher) {
        val env = Env(localTrips = true)
        env.publish(7)
        shown(env)
        assertEquals(1, env.navi.calls)

        env.publish(7, places = 3)
        runCurrent()
        assertEquals(2, env.navi.calls)

        env.trips.setVisibility(7, Visibility.PRIVATE)
        runCurrent()
        assertEquals(3, env.navi.calls)
    }

    @Test
    fun `H59 T18 T13 묶음 변화 뒤 조회가 위치 못 얻음 — 카드 빠짐`() = runTest(dispatcher) {
        val env = Env(localTrips = true)
        shown(env)
        env.navi.error = IllegalStateException("조회 오류")
        env.publish(7)
        runCurrent()
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
        assertTrue(env.section.cards.isEmpty())
    }

    @Test
    fun `H60 T19 위치 못 얻음 · 묶음 변화 · 별점 변화 — 조회 0회 · 그대로`() = runTest(dispatcher) {
        val env = Env(localTrips = true)
        env.navi.error = IllegalStateException("조회 오류")
        shown(env)
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
        env.publish(7)
        env.rate(101L)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertEquals(NearbySectionState.LOCATION_UNAVAILABLE, env.state)
    }

    @Test
    fun `H61 T17 조회 진행 중 묶음 변화 — 새 조회 0회 · 결과는 바뀐 자료로`() = runTest(dispatcher) {
        val env = Env(localTrips = true)
        val mine = AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + 7
        env.rate(mine)
        env.navi.delayMs = 1_000
        shown(env)
        env.publish(7)
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(1, env.navi.calls)
        assertTrue(env.section.cards.any { it.tripNo == mine })
    }

    @Test
    fun `H62 동의 → 근처 있음까지 — 새로 쌓인 것은 동의 이력 행뿐(위치 값 없음)`() = runTest(dispatcher) {
        val env = Env(consent = Consent.NONE)
        shown(env)
        env.vm.onAgreeNearby()
        runCurrent()
        assertEquals(NearbySectionState.NEARBY, env.state)
        assertEquals(listOf(ConsentType.LOCATION_RECO to true), env.users.history)
        assertTrue(env.trips.current.isEmpty())
    }
}
