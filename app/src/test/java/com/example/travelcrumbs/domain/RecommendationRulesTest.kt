package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 계산(#37) — 지역 · 정렬 · 동행자 필터 · 태그별 개수 · 대표 태그 · 경로 그림. */
class RecommendationRulesTest {

    private val kids = Tag(1, "아이와 가기 좋아요")
    private val parking = Tag(2, "주차 편해요")
    private val again = Tag(3, "다시 가고 싶어요")

    private fun place(no: Long, ord: Int, lat: Double, lng: Double, vararg tags: Tag) =
        RecommendedPlace(no, "장소$no", ord, "주소", lat, lng, tags.toList())

    private fun trip(
        no: Long,
        sigungu: String,
        rating: Double,
        cnt: Int,
        published: Long,
        companions: Set<CompanionType>,
        places: List<RecommendedPlace>,
    ) = RecommendedTrip(no, "여행$no", "닉$no", Region("강원", sigungu), companions, rating, cnt, published, places)

    private val a = trip(1, "속초시", 4.8, 12, 100, setOf(CompanionType.CHILD, CompanionType.SPOUSE), listOf(
        place(11, 1, 38.20, 128.59, parking),
        place(12, 2, 38.22, 128.58, kids, again),
        place(13, 3, 38.19, 128.60, kids),
    ))
    private val b = trip(2, "속초시", 4.6, 9, 300, setOf(CompanionType.SPOUSE), listOf(
        place(21, 1, 38.17, 128.60, parking),
        place(22, 2, 38.18, 128.61, parking, kids),
    ))
    private val c = trip(3, "속초시", 4.8, 6, 200, setOf(CompanionType.SOLO), listOf(
        place(31, 1, 38.17, 128.49, again),
    ))
    private val d = trip(4, "강릉시", 4.9, 20, 400, setOf(CompanionType.SOLO), listOf(
        place(41, 1, 37.80, 128.90, again, again),
    ))
    private val all = listOf(a, b, c, d)

    @Test
    fun `R3 시군구별 로드맵 수 — 목록 순서 그대로 · 없는 시군구는 0`() {
        val cnts = RecommendationRules.sigunguTripCnts(all, "강원", listOf("속초시", "강릉시", "양양군"))
        assertEquals(listOf("속초시" to 3, "강릉시" to 1, "양양군" to 0), cnts.toList())
    }

    @Test
    fun `R4 지역 — 시군구 하나 · null 이면 시도 전체`() {
        assertEquals(listOf(a, b, c), RecommendationRules.inRegion(all, Region("강원", "속초시")))
        assertEquals(all, RecommendationRules.inRegion(all, Region("강원", null)))
        assertTrue(RecommendationRules.inRegion(all, Region("제주", null)).isEmpty())
    }

    @Test
    fun `R5 별점순 — 별점 높은 순 · 동점은 참여 수 많은 순`() {
        val ranked = RecommendationRules.rank(listOf(a, b, c), RankingSort.RATING, null)
        assertEquals(listOf(1L, 3L, 2L), ranked.map { it.tripNo })
    }

    @Test
    fun `R6 최신순 — 공개 일시 최근 순`() {
        val ranked = RecommendationRules.rank(listOf(a, b, c), RankingSort.LATEST, null)
        assertEquals(listOf(2L, 3L, 1L), ranked.map { it.tripNo })
    }

    @Test
    fun `R7 동행자 필터 — 그 동행자가 있는 로드맵만 · null 은 전체`() {
        assertEquals(listOf(1L), RecommendationRules.rank(listOf(a, b, c), RankingSort.RATING, CompanionType.CHILD).map { it.tripNo })
        assertEquals(listOf(1L, 2L), RecommendationRules.rank(listOf(a, b, c), RankingSort.RATING, CompanionType.SPOUSE).map { it.tripNo })
        assertTrue(RecommendationRules.rank(listOf(a, b, c), RankingSort.RATING, CompanionType.PET).isEmpty())
    }

    @Test
    fun `R8 태그별 개수 — 장소에 붙은 수 · 많은 순 · 동수는 태그 번호순 · 한 장소에 같은 태그는 한 번`() {
        // 넷 다: 주차 3 · 아이와 3 · 다시 3(강릉 장소의 겹친 「다시」는 한 번) → 동수라 번호순
        val counts = RecommendationRules.tagCounts(listOf(a, b, c, d))
        assertEquals(listOf(kids to 3, parking to 3, again to 3), counts.map { it.tag to it.cnt })

        // 속초 셋: 주차 3 · 아이와 3 · 다시 2
        val sokcho = RecommendationRules.tagCounts(listOf(a, b, c))
        assertEquals(listOf(kids to 3, parking to 3, again to 2), sokcho.map { it.tag to it.cnt })
    }

    @Test
    fun `R9 태그로 거르기 — 그 태그가 한 장소라도 있는 로드맵 · 별점순`() {
        assertEquals(listOf(1L, 2L), RecommendationRules.withTag(listOf(b, c, a), kids.tagNo).map { it.tripNo })
        assertEquals(listOf(1L, 3L), RecommendationRules.withTag(listOf(b, c, a), again.tagNo).map { it.tripNo })
    }

    @Test
    fun `R10 대표 태그 — 가장 많이 붙은 태그 · 동수는 먼저 나온 태그 · 없으면 null`() {
        assertEquals(kids, RecommendationRules.representativeTag(a))
        assertEquals(parking, RecommendationRules.representativeTag(b))
        assertNull(RecommendationRules.representativeTag(a.copy(places = emptyList())))
    }

    @Test
    fun `R11 경로 그림 — 장소 순서대로 · 0 ~ 1 안 · 북쪽이 위`() {
        val points = RecommendationRules.sketch(a.places.reversed(), margin = 0f)
        assertEquals(3, points.size)
        assertTrue(points.all { it.x in 0f..1f && it.y in 0f..1f })
        // 2번 장소(위도 가장 큼) → y 0 · 3번 장소(위도 가장 작음) → y 1
        assertEquals(0f, points[1].y, 0.0001f)
        assertEquals(1f, points[2].y, 0.0001f)
        // 장소 하나 → 가운데
        val single = RecommendationRules.sketch(c.places, margin = 0f).single()
        assertEquals(0.5f, single.x, 0.0001f)
        assertEquals(0.5f, single.y, 0.0001f)
    }
}
