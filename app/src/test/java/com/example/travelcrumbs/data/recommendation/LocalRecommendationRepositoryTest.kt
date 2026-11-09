package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeTagRepository
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.completedTrip
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 = 시드 + 내 공개 여행(#51 U7). */
class LocalRecommendationRepositoryTest {

    private val trips = FakeTripRepository(
        listOf(
            completedTrip(tripNo = 7, tripNm = "속초 가족여행", places = 2, visibility = Visibility.ANONYMOUS_PUBLIC),
            completedTrip(tripNo = 8, tripNm = "비공개 여행", visibility = Visibility.PRIVATE),
            completedTrip(tripNo = 9, tripNm = "해운대", places = 1, visibility = Visibility.ANONYMOUS_PUBLIC).let { trip ->
                trip.copy(places = trip.places.map { it.copy(addr = "부산 해운대구 해운대해변로 264") })
            },
            completedTrip(tripNo = 10, tripNm = "위치 없음", visibility = Visibility.ANONYMOUS_PUBLIC).let { trip ->
                trip.copy(places = trip.places.map { it.copy(lat = null, lng = null) })
            },
        ),
    )
    private val users = FakeCurrentUserRepository(nicknameNm = "민지네")
    private val repository = LocalRecommendationRepository(trips, users, FakeTagRepository())

    @Test
    fun `U7-3 내 공개 여행이 강원 속초시 랭킹 · 태그별 보기에 닉네임으로 · 비공개 · 위치 없음은 빠짐`() = runTest {
        val gangwon = repository.publicTrips("강원")
        val mine = gangwon.single { it.tripNo == AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + 7 }
        assertEquals("민지네", mine.nicknameNm)
        assertEquals(Region("강원", "속초시"), mine.region)
        assertEquals(2, mine.placeCnt)
        assertTrue(gangwon.none { it.tripNm == "비공개 여행" || it.tripNm == "위치 없음" })
        assertEquals(RecommendationSeed.trips.count { it.region.sidoNm == "강원" } + 1, gangwon.size)

        val sokcho = RecommendationRules.inRegion(gangwon, Region("강원", "속초시"))
        assertTrue(sokcho.any { it.tripNo == mine.tripNo })
        assertTrue(RecommendationRules.withTag(sokcho, 1L, emptyMap()).any { it.tripNo == mine.tripNo })

        users.updateNickname("바다곰네")
        assertEquals("바다곰네", repository.publicTrips("강원").single { it.tripNo == mine.tripNo }.nicknameNm)
    }

    @Test
    fun `U7-4 지역 목록 — 시드 시도에 내 여행 시군구가 더해짐 · 시드에 없는 시군구도`() = runTest {
        val regions = repository.sidoRegions()
        val busan = regions.single { it.sidoNm == "부산" }
        assertEquals(listOf("해운대구"), busan.sigunguNms)
        assertEquals(1, repository.publicTrips("부산").size)
        assertTrue(regions.single { it.sidoNm == "강원" }.sigunguNms.contains("속초시"))
    }
}
