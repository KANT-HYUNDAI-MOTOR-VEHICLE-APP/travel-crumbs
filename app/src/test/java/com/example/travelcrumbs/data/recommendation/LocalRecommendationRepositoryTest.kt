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
            completedTrip(tripNo = 9, tripNm = "동성로", places = 1, visibility = Visibility.ANONYMOUS_PUBLIC).let { trip ->
                trip.copy(places = trip.places.map { it.copy(addr = "대구 중구 동성로2길 80") })
            },
            completedTrip(tripNo = 10, tripNm = "위치 없음", visibility = Visibility.ANONYMOUS_PUBLIC).let { trip ->
                trip.copy(places = trip.places.map { it.copy(lat = null, lng = null) })
            },
            // 에뮬레이터 실측 — 내비 현재 위치 주소가 시도 없이 옴 → 목적지(경복궁) 주소로 서울 종로구
            completedTrip(tripNo = 11, tripNm = "경복궁 나들이", places = 1, visibility = Visibility.ANONYMOUS_PUBLIC).let { trip ->
                trip.copy(destPlaceNm = "경복궁", places = trip.places.map { it.copy(addr = "수정구 시흥동") })
            },
        ),
    )
    private val users = FakeCurrentUserRepository(nicknameNm = "민지네")
    private val repository = LocalRecommendationRepository(
        trips,
        users,
        FakeTagRepository(),
        com.example.travelcrumbs.testing.FakePlaceCatalogRepository(
            listOf(com.example.travelcrumbs.domain.model.Place("gyeongbokgung", "경복궁", "서울 종로구 사직로 161", 37.5796, 126.977)),
        ),
    )

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
    fun `U7-5 장소 주소에 시도가 없으면 목적지 주소로 — 서울 종로구`() = runTest {
        val seoul = repository.publicTrips("서울")
        assertEquals(Region("서울", "종로구"), seoul.single { it.tripNm == "경복궁 나들이" }.region)
    }

    @Test
    fun `U7-4 지역 목록 — 시드 시도에 내 여행 시군구가 더해짐 · 시드에 없는 시군구도`() = runTest {
        val regions = repository.sidoRegions()
        // 시드에 없는 시도(대구) — 내 여행 시군구로 채워짐
        val daegu = regions.single { it.sidoNm == "대구" }
        assertEquals(listOf("중구"), daegu.sigunguNms)
        assertEquals(1, repository.publicTrips("대구").size)
        // 시드에 있는 시군구(서울 종로구)는 두 번 들어가지 않음
        assertEquals(1, regions.single { it.sidoNm == "서울" }.sigunguNms.count { it == "종로구" })
        assertTrue(regions.single { it.sidoNm == "강원" }.sigunguNms.contains("속초시"))
    }
}
