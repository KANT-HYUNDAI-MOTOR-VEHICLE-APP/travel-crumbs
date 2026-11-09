package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TagCount

/** 추천 화면의 순수 계산 — 지역 · 정렬 · 동행자 필터 · 태그별 개수 · 대표 태그 · 경로 그림(#37). */
object RecommendationRules {

    /** 그 지역(시군구 null = 시도 전체)의 로드맵. */
    fun inRegion(trips: List<RecommendedTrip>, region: Region): List<RecommendedTrip> = trips.filter { trip ->
        trip.region.sidoNm == region.sidoNm &&
            (region.sigunguNm == null || trip.region.sigunguNm == region.sigunguNm)
    }

    /** 시군구별 로드맵 수 — 시군구 목록 순서 그대로 · 없는 시군구는 0. */
    fun sigunguTripCnts(trips: List<RecommendedTrip>, sidoNm: String, sigunguNms: List<String>): Map<String, Int> {
        val inSido = trips.filter { it.region.sidoNm == sidoNm }
        return sigunguNms.associateWith { name -> inSido.count { it.region.sigunguNm == name } }
    }

    /** B-02 랭킹 — 동행자 필터(null = 전체) 뒤 정렬. 별점순 동점은 참여 수 · 최신순 동점은 별점. */
    fun rank(
        trips: List<RecommendedTrip>,
        sort: RankingSort,
        companion: CompanionType?,
    ): List<RecommendedTrip> {
        val filtered = if (companion == null) trips else trips.filter { companion in it.companionTypes }
        val comparator = when (sort) {
            RankingSort.RATING -> compareByDescending<RecommendedTrip> { it.ratingAvg }
                .thenByDescending { it.ratingCnt }
                .thenBy { it.tripNo }
            RankingSort.LATEST -> compareByDescending<RecommendedTrip> { it.publishedAt }
                .thenByDescending { it.ratingAvg }
                .thenBy { it.tripNo }
        }
        return filtered.sortedWith(comparator)
    }

    /** B-03 — 로드맵들의 장소에 붙은 태그 수(장소 하나에 같은 태그는 한 번) · 많은 순 · 동수는 태그 번호순. */
    fun tagCounts(trips: List<RecommendedTrip>): List<TagCount> = trips
        .flatMap { it.places }
        .flatMap { place -> place.tags.distinct() }
        .groupingBy { it }
        .eachCount()
        .map { (tag, cnt) -> TagCount(tag, cnt) }
        .sortedWith(compareByDescending<TagCount> { it.cnt }.thenBy { it.tag.tagNo })

    /** 그 태그가 한 장소라도 붙은 로드맵 — 별점순. */
    fun withTag(trips: List<RecommendedTrip>, tagNo: Long): List<RecommendedTrip> =
        rank(trips.filter { trip -> trip.places.any { place -> place.tags.any { it.tagNo == tagNo } } }, RankingSort.RATING, null)

    /** 대표 태그 — 그 로드맵 장소에 가장 많이 붙은 태그 · 동수는 먼저 나온 태그. 태그가 없으면 null. */
    fun representativeTag(trip: RecommendedTrip): Tag? {
        val ordered = trip.places.sortedBy { it.placeOrd }.flatMap { it.tags }
        if (ordered.isEmpty()) return null
        val counts = ordered.groupingBy { it }.eachCount()
        val max = counts.values.max()
        return ordered.first { counts.getValue(it) == max }
    }

    /** 장소 순서대로의 경로 그림 점 — 경도 → x · 위도 → y(북쪽이 위) · 여백 [margin] 안에 맞춤. */
    fun sketch(places: List<RecommendedPlace>, margin: Float = SKETCH_MARGIN): List<SketchPoint> {
        val ordered = places.sortedBy { it.placeOrd }
        if (ordered.isEmpty()) return emptyList()
        val minLng = ordered.minOf { it.lng }
        val maxLng = ordered.maxOf { it.lng }
        val minLat = ordered.minOf { it.lat }
        val maxLat = ordered.maxOf { it.lat }
        val span = 1f - 2 * margin
        fun scale(value: Double, min: Double, max: Double): Float =
            if (max - min == 0.0) 0.5f else ((value - min) / (max - min)).toFloat()
        return ordered.map { place ->
            SketchPoint(
                x = margin + span * scale(place.lng, minLng, maxLng),
                y = margin + span * (1f - scale(place.lat, minLat, maxLat)),
            )
        }
    }

    private const val SKETCH_MARGIN = 0.12f
}
