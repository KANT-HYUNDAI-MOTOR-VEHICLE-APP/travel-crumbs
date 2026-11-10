package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.GuidedTrip
import com.example.travelcrumbs.domain.model.MyRating
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RatingRecords
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TagCount

/** 추천 화면의 순수 계산 — 지역 · 정렬 · 동행자 필터 · 태그별 개수 · 대표 태그 · 경로 그림(#37) · 내 별점 반영 · 별점 자격(#41). */
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

    /**
     * B-03 — 로드맵들의 장소에 붙은 태그 수(장소 하나에 같은 태그는 한 번) + 내 별점의 좋았던 점 태그(로드맵 하나에 태그마다 1 · #41)
     * · 많은 순 · 동수는 태그 번호순. 내 별점 태그는 [tags] 에서 이름을 찾음(모르는 번호는 셈에서 뺌).
     */
    fun tagCounts(
        trips: List<RecommendedTrip>,
        myRatings: Map<Long, MyRating> = emptyMap(),
        tags: List<Tag> = emptyList(),
    ): List<TagCount> {
        val tagByNo = tags.associateBy { it.tagNo }
        val placeTags = trips.flatMap { it.places }.flatMap { place -> place.tags.distinct() }
        val ratingTags = trips.mapNotNull { myRatings[it.tripNo] }.flatMap { rating -> rating.tagNos.mapNotNull { tagByNo[it] } }
        return (placeTags + ratingTags)
            .groupingBy { it }
            .eachCount()
            .map { (tag, cnt) -> TagCount(tag, cnt) }
            .sortedWith(compareByDescending<TagCount> { it.cnt }.thenBy { it.tag.tagNo })
    }

    /** 그 태그가 한 장소라도 붙었거나 내 별점에서 고른 로드맵 — 별점순. */
    fun withTag(
        trips: List<RecommendedTrip>,
        tagNo: Long,
        myRatings: Map<Long, MyRating> = emptyMap(),
    ): List<RecommendedTrip> = rank(
        trips.filter { trip ->
            trip.places.any { place -> place.tags.any { it.tagNo == tagNo } } ||
                myRatings[trip.tripNo]?.tagNos?.contains(tagNo) == true
        },
        RankingSort.RATING,
        null,
    )

    /** 내 별점 하나를 더한 평균 · 참여 수(#41) — 저장소가 준 값엔 내 별점이 없음(기기 안 저장 · 서버 전). */
    fun withMyRating(trip: RecommendedTrip, rating: MyRating?): RecommendedTrip {
        if (rating == null) return trip
        val cnt = trip.ratingCnt + 1
        return trip.copy(ratingAvg = (trip.ratingAvg * trip.ratingCnt + rating.ratingScore) / cnt, ratingCnt = cnt)
    }

    /** 로드맵 목록 전부에 내 별점 반영. */
    fun withMyRatings(trips: List<RecommendedTrip>, myRatings: Map<Long, MyRating>): List<RecommendedTrip> =
        if (myRatings.isEmpty()) trips else trips.map { withMyRating(it, myRatings[it.tripNo]) }

    /** 별점 자격 — 안내를 요청한 로드맵만(#41 · 이번 판). */
    fun canRate(records: RatingRecords, tripNo: Long): Boolean = records.guided(tripNo) != null

    /** 별 수가 범위 안인지. */
    fun isValidScore(score: Int): Boolean = score in AppConfig.RATING_SCORE_MIN..AppConfig.RATING_SCORE_MAX

    /**
     * 기록 칸 정차 화면에 자동으로 띄울 별점 팝업 — 안내받은 뒤 진행한 여행을 「여행 종료」로 끝냈고 · 아직 별점이 없고 ·
     * 자동 팝업을 띄운 적 없는 로드맵 중 가장 최근에 안내받은 것. 없으면 null(로드맵마다 한 번 · 「나중에」 기억).
     * 안내만 받고 여행을 끝내지 않았으면(중간 정차 · 기록 없음) 자동 팝업 없음.
     */
    fun pendingRatingPrompt(records: RatingRecords): GuidedTrip? = records.guidedTrips
        .filter { it.tripEndedAt != null && it.tripNo !in records.promptedTripNos && records.ratingOf(it.tripNo) == null }
        .maxByOrNull { it.guidedAt }

    /** 여행 종료 시각 [endedAt] 을 그 전에 안내받았고 아직 끝낸 여행이 없는 로드맵에 남김. */
    fun markGuidedTripsEnded(records: RatingRecords, endedAt: Long): RatingRecords = records.copy(
        guidedTrips = records.guidedTrips.map { guided ->
            if (guided.tripEndedAt == null && guided.guidedAt <= endedAt) guided.copy(tripEndedAt = endedAt) else guided
        },
    )

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
