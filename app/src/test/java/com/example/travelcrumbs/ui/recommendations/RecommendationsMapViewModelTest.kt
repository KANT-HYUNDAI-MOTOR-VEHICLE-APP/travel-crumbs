package com.example.travelcrumbs.ui.recommendations

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.model.LocationConsent
import com.example.travelcrumbs.domain.model.LocationResultKind
import com.example.travelcrumbs.domain.model.MapFitBasis
import com.example.travelcrumbs.domain.model.MapPinResult
import com.example.travelcrumbs.domain.model.MapStatus
import com.example.travelcrumbs.testing.FakeOfflineMapRepository
import com.example.travelcrumbs.testing.FakeRouteGuidanceRequester
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.MutableRecommendationRepository
import com.example.travelcrumbs.testing.ratingRepositoryOf
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
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

/** B-01 추천 지도(#59) ViewModel — 지도 부품이 넘기는 번호 · 개수만으로 카드 · 후보 · 뒤로 · 저장 상태. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val guidance = FakeRouteGuidanceRequester()
    private val ratings = ratingRepositoryOf()
    private val repository = MutableRecommendationRepository()
    private lateinit var vm: RecommendationsViewModel
    private val state get() = vm.uiState.value
    private val map get() = state.map

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.start(saved: SavedStateHandle = SavedStateHandle()) {
        vm = RecommendationsViewModel(
            repository = repository,
            requestRouteGuidance = RequestRouteGuidanceUseCase(guidance),
            savedStateHandle = saved,
            ratingRepository = ratings,
            timeSource = FakeTimeSource(),
            offlineMap = FakeOfflineMapRepository(),
        )
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        vm.onEntry(1)
        runCurrent()
    }

    @Test
    fun `M01 M02 M40 처음 — 지도 준비 중 · 전국 맞춤 · 위치 동의 안 함 · 위치 조회 없음`() = runTest(dispatcher) {
        start()

        assertEquals(RecommendationPage.REGION, state.page)
        assertEquals(MapStatus.PREPARING, map.status)
        assertEquals(MapFitBasis.COUNTRY, map.fitBasis)
        assertEquals(LocationConsent.NOT_AGREED, map.consent)
        assertFalse(map.locationAllowed)
        assertEquals(LocationResultKind.NONE, map.locationResult)
        assertNull(map.card)

        vm.onMapReady()
        vm.onMapFirstFitDone()
        runCurrent()
        assertEquals(MapStatus.READY, map.status)
        assertTrue(map.firstFitDone)
    }

    @Test
    fun `M06 M15 시도 칩 — 지도 이동 요청 · 첫 맞춤 끝남 · 카드 닫힘 · B-01 머묾`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        runCurrent()
        assertNotNull(map.card)

        vm.selectSido("강원")
        runCurrent()

        assertEquals(RecommendationPage.REGION, state.page)
        assertEquals(MapFitBasis.SIDO, map.fitBasis)
        assertEquals("강원", map.focusSidoNm)
        assertEquals(1, map.focusSeq)
        assertTrue(map.firstFitDone)
        assertNull(map.card)
    }

    @Test
    fun `M12 M17 낱개 핀 — 장소 카드 · 로드맵 후보 리뷰 많은 순 · 고르면 대상 바뀜 · 요청 없음`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        runCurrent()

        val card = map.card!!
        assertEquals("속초 중앙시장 공영주차장", card.placeNm)
        assertEquals(listOf(101L, 102L), card.roadmaps.map { it.tripNo })
        assertEquals(101L, card.selectedTripNo)
        assertEquals("민지네", card.selectedRoadmap.nicknameNm)
        assertTrue(card.tagNms.isNotEmpty())
        assertTrue(guidance.requested.isEmpty())

        vm.selectCardRoadmap(102)
        runCurrent()
        assertEquals(102L, map.card!!.selectedTripNo)
        assertEquals("바다곰", map.card!!.selectedRoadmap.nicknameNm)
    }

    @Test
    fun `M13 태그 없는 장소 — 태그 칸 비어 있음(화면이 숨김)`() = runTest(dispatcher) {
        val seed = RecommendationSeed.trips
        repository.trips = seed.map { trip -> trip.copy(places = trip.places.map { it.copy(tags = emptyList()) }) }
        start()
        vm.selectMapPlace(1001)
        runCurrent()
        assertTrue(map.card!!.tagNms.isEmpty())
    }

    @Test
    fun `M14 M16 다른 핀은 카드 교체 · 뒤로는 카드만 닫음`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        vm.selectCardRoadmap(102)
        vm.selectMapPlace(1011)
        runCurrent()
        assertEquals(1011L, map.card!!.placeNo)
        assertEquals(102L, map.card!!.selectedTripNo)
        assertTrue(state.handlesBack)

        vm.onBack()
        runCurrent()
        assertNull(map.card)
        assertEquals(RecommendationPage.REGION, state.page)
        assertFalse(state.handlesBack)
    }

    @Test
    fun `M35 묶음 핀 — 후보 목록(리뷰 많은 순) · 후보 카드 · 뒤로 후보 목록 · 뒤로 닫힘`() = runTest(dispatcher) {
        start()
        vm.selectMapCluster(listOf(1011, 1001, 1003))
        runCurrent()

        val candidates = map.candidates!!
        // 1001 은 로드맵 둘(12 + 9) · 1011 · 1003 은 하나씩
        assertEquals(1001L, candidates.first().placeNo)
        assertEquals(2, candidates.first().roadmapCnt)
        assertNull(map.card)

        vm.selectMapCandidate(1011)
        runCurrent()
        assertEquals(1011L, map.card!!.placeNo)
        assertTrue(map.card!!.fromCandidates)

        vm.onBack()
        runCurrent()
        assertNull(map.card)
        assertNotNull(map.candidates)

        vm.onBack()
        runCurrent()
        assertNull(map.candidates)
    }

    @Test
    fun `M18 로드맵 보기 — 고른 로드맵 B-04 · 뒤로면 지도 카드 그대로`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        vm.selectCardRoadmap(102)
        runCurrent()

        vm.openCardRoadmap()
        runCurrent()
        assertEquals(RecommendationPage.DETAIL, state.page)
        assertEquals(102L, state.detail!!.tripNo)

        vm.onBack()
        runCurrent()
        assertEquals(RecommendationPage.REGION, state.page)
        assertEquals(1001L, map.card!!.placeNo)
    }

    @Test
    fun `M19 M20 여기로 안내 — 확인 팝업 대상 일치 · 취소면 요청 0 · 성공이면 고른 로드맵 별점 자격`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        vm.selectCardRoadmap(102)
        runCurrent()

        vm.askCardGuidance()
        runCurrent()
        assertEquals(1001L, state.confirm!!.placeNo)
        assertEquals("바다 보고 시장 한 바퀴", state.confirm!!.tripNm)

        vm.onBack()
        runCurrent()
        assertNull(state.confirm)
        assertNotNull(map.card)
        assertTrue(guidance.requested.isEmpty())

        vm.askCardGuidance()
        runCurrent()
        vm.startGuidance()
        vm.startGuidance()
        runCurrent()
        assertEquals(1, guidance.requested.size)
        assertTrue(ratings.records.first().guidedTrips.any { it.tripNo == 102L })
    }

    @Test
    fun `M21 안내 실패 — 별점 자격 없음`() = runTest(dispatcher) {
        guidance.result = GuidanceRequestResult.Failed
        start()
        vm.selectMapPlace(1001)
        runCurrent()
        vm.askCardGuidance()
        runCurrent()
        vm.startGuidance()
        runCurrent()
        assertTrue(ratings.records.first().guidedTrips.none { it.tripNo == 101L })
    }

    @Test
    fun `M09 화면 범위 공개 장소 0 · M30 지도 못 그림 — 지역 선택 · 랭킹은 그대로`() = runTest(dispatcher) {
        start()
        vm.onMapReady()
        vm.onMapPins(MapPinResult(emptyList(), emptyList(), inRangeCnt = 0))
        runCurrent()
        assertEquals(MapStatus.EMPTY_RANGE, map.status)

        vm.onMapPins(MapPinResult(listOf(1001), listOf(listOf(1011, 1012)), inRangeCnt = 3))
        runCurrent()
        assertEquals(MapStatus.READY, map.status)
        assertEquals(listOf(1001L), map.singlePlaceNos)
        assertEquals(3, map.inRangeCnt)

        vm.onMapFailed()
        vm.selectSido("강원")
        vm.selectRegion("속초시")
        runCurrent()
        assertEquals(MapStatus.FAILED, map.status)
        assertEquals(RecommendationPage.RANKING, state.page)
        assertTrue(state.rankedTrips.isNotEmpty())
    }

    @Test
    fun `M26 선택 장소가 비공개 · 삭제되면 다시 읽을 때 카드 지움`() = runTest(dispatcher) {
        start()
        vm.selectMapPlace(1001)
        runCurrent()
        assertNotNull(map.card)

        repository.trips = RecommendationSeed.trips.filter { trip -> trip.places.none { it.placeNo == 1001L } }
        vm.onEntry(1)
        runCurrent()
        assertNull(map.card)
    }

    @Test
    fun `M27 M41 저장 상태엔 시도 · 시군구 · 장소 번호 · 로드맵 번호뿐 · 복원 때 다시 확인`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        start(saved)
        vm.selectSido("강원")
        vm.onMapPins(MapPinResult(listOf(1001), emptyList(), inRangeCnt = 1))
        vm.onMapMovedByUser()
        vm.selectMapPlace(1001)
        vm.selectCardRoadmap(102)
        runCurrent()

        val mapKeys = saved.keys().filter { it.startsWith("reco_map") }
        assertEquals(setOf("reco_map_place_no", "reco_map_trip_no"), mapKeys.toSet())
        val forbidden = Regex("lat|lng|lon|center|bound|zoom|camera|coord", RegexOption.IGNORE_CASE)
        assertTrue(saved.keys().none { forbidden.containsMatchIn(it) })
        saved.keys().forEach { key ->
            val value = saved.get<Any?>(key)
            assertFalse("$key 에 실수 값", value is Double || value is Float)
        }

        // 프로세스 재생성 — 같은 저장 상태로 새 ViewModel · 지도 위치는 없고 첫 맞춤 다시
        start(saved)
        assertEquals(1001L, map.card!!.placeNo)
        assertEquals(102L, map.card!!.selectedTripNo)
        assertFalse(map.firstFitDone)
    }

    @Test
    fun `M41 추천 UiState 의 지도 칸에 좌표 · 지도 가운데 · 화면 범위 칸 없음`() {
        val forbidden = Regex("lat|lng|lon|center|bound|zoom|camera|coord|^x$|^y$", RegexOption.IGNORE_CASE)
        listOf<KClass<*>>(
            RecommendationsUiState::class,
            RecommendationMapUi::class,
            MapPlaceCardItem::class,
            MapRoadmapItem::class,
            MapCandidateItem::class,
            MapPinResult::class,
        ).forEach { cls ->
            cls.memberProperties.forEach { property ->
                assertFalse("${cls.simpleName}.${property.name}", forbidden.containsMatchIn(property.name))
            }
        }
    }

    @Test
    fun `M41 추천 화면 · 지도 코드에 로그 호출 없음(좌표 · 범위가 로그로 새지 않게)`() {
        val folder = File("src/main/java/com/example/travelcrumbs/ui/recommendations")
        val offenders = folder.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().let { it.contains("android.util.Log") || it.contains("Log.") } }
            .map { it.name }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }
}
