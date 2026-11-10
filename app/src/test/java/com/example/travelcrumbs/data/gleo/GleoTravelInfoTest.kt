package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.recordingRepositoryOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #69 스펙 3.3 · T8 T10 ~ T11 — 기기 안 여행 · 추천 시드를 읽기만 함. */
class GleoTravelInfoTest {
    private val time = FakeTimeSource(now = 1_700_000_000_000L)

    /** 시드만 돌려주는 추천 대역 — 지역 · 순위 규칙은 실제 것을 씀. */
    private val seedOnly = object : RecommendationRepository {
        override val isDemoData: Boolean = true
        override suspend fun sidoRegions(): List<SidoRegion> = RecommendationSeed.sidoRegions
        override suspend fun publicTrips(sidoNm: String): List<RecommendedTrip> =
            RecommendationSeed.trips.filter { it.region.sidoNm == sidoNm }
        override suspend fun tags(): List<Tag> = RecommendationSeed.tags
    }

    private fun info(
        trips: List<TripRecord> = emptyList(),
        current: CurrentTrip? = null,
        navi: FakeNaviRepository = FakeNaviRepository().apply { location = null },
    ) = GleoTravelInfo(
        trips = FakeTripRepository(trips),
        recording = recordingRepositoryOf(time, FakeRecordingLocalDataSource(current)),
        recommendations = seedOnly,
        navi = navi,
        timeSource = time,
    )

    private fun regionsOf(data: String?) =
        Json.parseToJsonElement(data!!).jsonArray.map { it.jsonObject.getValue("region").jsonPrimitive.content }

    @Test
    fun `T8 오늘 기록 수 · 지금 여행 상태를 말하고 내 여행 칸 요청`() = runTest {
        fun place(no: Long) = TripPlace(no, 7L, no.toInt(), null, null, true, null, null, null, time.now - 1_000L, emptyList())
        val today = TripRecord(7L, null, TripStatus.RECORDING, Visibility.PRIVATE, 0L, null, null, null, emptySet(), listOf(place(1), place(2)))
        val outcome = info(listOf(today), CurrentTrip.start(time.now).copy(tripNo = 7L)).show("TODAY_RECORDS", null)

        assertEquals(GleoCommandPhase.TODAY_RECORDS, outcome.phase)
        assertEquals("2", outcome.detail)
        assertEquals(GleoScreenRequest.OpenTrips, outcome.screen)
        assertTrue(outcome.message.startsWith("You recorded 2 places today."))
        val data = Json.parseToJsonElement(outcome.data!!).jsonArray.single().jsonObject
        assertEquals(2, data.getValue("todayPlaceCount").jsonPrimitive.content.toInt())
        assertEquals("RECORDING", data.getValue("currentTripStatus").jsonPrimitive.content)
    }

    @Test
    fun `T10 말한 지역 경주 — 경주시 로드맵만 별점순 최대 3개 · 현재 위치를 묻지 않음`() = runTest {
        val navi = FakeNaviRepository().apply { location = CurrentLocation(37.4979, 127.0276, null) }
        val outcome = info(navi = navi).show("RECOMMENDED_ROADMAPS", "경주")

        assertEquals(GleoCommandPhase.RECOMMENDED, outcome.phase)
        assertEquals(GleoStatus.SUCCESS, outcome.status)
        val regions = regionsOf(outcome.data)
        assertTrue(regions.isNotEmpty() && regions.size <= 3)
        assertTrue(regions.all { it == "경북 경주시" })
        assertEquals(GleoScreenRequest.OpenRecommendations(Region("경북", "경주시")), outcome.screen)
        assertEquals("경북 경주시", outcome.detail)
        assertFalse(outcome.message.contains("37.4"))
    }

    @Test
    fun `T10b 시드에 없는 지역은 실패 · 화면 요청 없음`() = runTest {
        val outcome = info().show("RECOMMENDED_ROADMAPS", "대구")

        assertEquals(GleoCommandPhase.NO_RECOMMENDATION, outcome.phase)
        assertEquals(GleoStatus.FAIL, outcome.status)
        assertEquals(null, outcome.screen)
        assertTrue(outcome.message.contains("대구"))
    }

    @Test
    fun `T10c 지역을 안 말하면 현재 위치에서 가장 가까운 지역`() = runTest {
        val navi = FakeNaviRepository().apply { location = CurrentLocation(37.4979, 127.0276, null) }
        val outcome = info(navi = navi).show("RECOMMENDED_ROADMAPS", null)

        assertTrue(regionsOf(outcome.data).all { it == "서울 강남구" })
        assertEquals(GleoScreenRequest.OpenRecommendations(Region("서울", "강남구")), outcome.screen)
    }

    @Test
    fun `T11 위치도 지역도 없으면 전체 상위 3개 · 추천 칸은 처음부터`() = runTest {
        val outcome = info().show("RECOMMENDED_ROADMAPS", null)

        assertEquals(GleoStatus.SUCCESS, outcome.status)
        assertEquals(3, regionsOf(outcome.data).size)
        assertEquals(GleoScreenRequest.OpenRecommendations(null), outcome.screen)
    }
}
