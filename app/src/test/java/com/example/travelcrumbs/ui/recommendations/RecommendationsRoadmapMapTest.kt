package com.example.travelcrumbs.ui.recommendations

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.model.MapArea
import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapLayer
import com.example.travelcrumbs.domain.model.OfflineMapPack
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/** B-04 상세의 도로 지도(M10 · M11) — 묶음이 덮으면 지도 · 아니면 경로 그림 점만. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsRoadmapMapTest {

    private val dispatcher = StandardTestDispatcher()

    /** 속초 시내만 덮는 지역(설악산 소공원은 밖). */
    private val sokchoOnly = OfflineMapPack(
        listOf(
            MapArea(
                areaId = "sokcho",
                label = "강원 속초시",
                bounds = MapBounds(128.57, 38.16, 128.62, 38.235),
                layers = mapOf(MapLayer.MAJOR to listOf(listOf(MapCoord(128.58, 38.17), MapCoord(128.61, 38.22)))),
                routes = emptyList(),
            ),
        ),
    )
    private val maps = FakeOfflineMapRepository(sokchoOnly)
    private lateinit var vm: RecommendationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.openSokchoTrip(tripNo: Long) {
        vm = RecommendationsViewModel(
            repository = SeedRecommendationRepository(),
            requestRouteGuidance = RequestRouteGuidanceUseCase(FakeRouteGuidanceRequester()),
            savedStateHandle = SavedStateHandle(),
            ratingRepository = ratingRepositoryOf(),
            timeSource = FakeTimeSource(),
            offlineMap = maps,
        )
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        vm.selectSido("강원")
        runCurrent()
        vm.selectRegion("속초시")
        runCurrent()
        vm.openTrip(tripNo)
        runCurrent()
    }

    @Test
    fun `M10 묶음이 덮는 로드맵 — 상세에 도로 지도 · 장소 수만큼 핀 · 지역 이름`() = runTest(dispatcher) {
        openSokchoTrip(101)

        val detail = vm.uiState.value.detail!!
        assertNotNull(detail.routeMap)
        val map = detail.routeMap!!
        assertEquals("강원 속초시", map.areaLabel)
        assertEquals(4, map.pins.size)
        assertEquals(3, map.legs.size)
        assertEquals(4, detail.sketch.size)
        assertEquals(1, maps.loads)

        vm.askGuidance(detail.places.first().placeNo)
        runCurrent()

        assertNotNull(vm.uiState.value.confirm)
        assertSame(map, vm.uiState.value.detail?.routeMap)
    }

    @Test
    fun `M11 묶음이 덮지 못하는 로드맵 — 지도 없음 · 기존 경로 그림 점은 그대로`() = runTest(dispatcher) {
        openSokchoTrip(103)

        val detail = vm.uiState.value.detail!!
        assertNull(detail.routeMap)
        assertEquals(3, detail.sketch.size)
    }
}
