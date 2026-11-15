package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.NearbyRoadmapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시연 시드에 홈 근처 로드맵 계산을 대 봄(#55 · 스펙 home-nearby H1 · H2 · H10 · N1 · N9). */
class NearbySeedTest {

    private val radiusM = 10_000

    @Test
    fun `H1 H2 속초 중앙시장에서 시드 — 반경 안 속초 3개 · 리뷰 12 9 6 순 · 순위 1 2 3 · 양양은 빠짐`() {
        val distances = NearbyRoadmapRules.firstPlaceDistances(RecommendationSeed.trips, 38.2049, 128.5906)
        val pick = NearbyRoadmapRules.pick(RecommendationSeed.trips, distances, radiusM, 10)!!

        assertTrue(pick.nearby)
        assertEquals(listOf(101L, 102L, 103L), pick.roadmaps.map { it.trip.tripNo })
        assertEquals(listOf(41, 9, 6), pick.roadmaps.map { it.trip.ratingCnt })
        assertEquals(listOf(1, 2, 3), pick.roadmaps.map { it.rank })
        assertTrue(pick.roadmaps.first().distM < 1_000)
    }

    @Test
    fun `H10 시드에서 성남 시흥동 부근이면 근처 없음 · 전국 리뷰 많은 순 10장`() {
        val distances = NearbyRoadmapRules.firstPlaceDistances(RecommendationSeed.trips, 37.4370, 127.1390)
        val pick = NearbyRoadmapRules.pick(RecommendationSeed.trips, distances, 5_000, 10)!!
        assertFalse(pick.nearby)
        assertEquals(10, pick.roadmaps.size)
        assertEquals(RecommendationSeed.trips.maxOf { it.ratingCnt }, pick.roadmaps.first().trip.ratingCnt)
    }
}
