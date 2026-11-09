package com.example.travelcrumbs.data.rating

import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.GuidedTrip
import com.example.travelcrumbs.domain.model.MyRating
import com.example.travelcrumbs.domain.model.RatingRecords
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 별점(#41 · B-06)의 유일한 저장소 — 안내 요청한 로드맵 · 내 별점 · 자동 팝업을 띄운 로드맵. 기기 안에만(서버 · 표준 등록 전).
 * 서버 별점 API 가 생기면 이 인터페이스의 구현만 바꿈(di/RatingModule).
 */
interface RatingRepository {
    val records: Flow<RatingRecords>

    /** 안내 요청이 성공한 로드맵을 남김(별점 자격). 이미 있으면 처음 시각 그대로. */
    suspend fun recordGuidance(tripNo: Long, tripNm: String, nicknameNm: String)

    /** 내 별점 저장(있으면 바꿈) · 자동 팝업도 끝난 것으로. 안내 기록이 없거나 별 수가 범위 밖이면 저장하지 않고 false. */
    suspend fun saveRating(tripNo: Long, ratingScore: Int, tagNos: Set<Long>): Boolean

    /** 자동 팝업을 띄웠음(「나중에」) — 그 로드맵은 다시 자동으로 안 뜸. */
    suspend fun markPrompted(tripNo: Long)

    /** 「여행 종료」 → 「종료」로 여행이 끝남 — 그 전에 안내받은 로드맵을 자동 팝업 대상으로. */
    suspend fun markGuidedTripsEnded()
}

@Singleton
class DefaultRatingRepository @Inject constructor(
    private val local: RatingLocalDataSource,
    private val timeSource: TimeSource,
) : RatingRepository {

    override val records: Flow<RatingRecords> = local.records

    override suspend fun recordGuidance(tripNo: Long, tripNm: String, nicknameNm: String) {
        val now = timeSource.nowMillis()
        local.update { records ->
            if (records.guided(tripNo) != null) {
                records
            } else {
                records.copy(guidedTrips = records.guidedTrips + GuidedTrip(tripNo, tripNm, nicknameNm, now))
            }
        }
    }

    override suspend fun saveRating(tripNo: Long, ratingScore: Int, tagNos: Set<Long>): Boolean {
        if (!RecommendationRules.isValidScore(ratingScore)) return false
        val now = timeSource.nowMillis()
        var saved = false
        local.update { records ->
            if (!RecommendationRules.canRate(records, tripNo)) return@update records
            saved = true
            records.copy(
                ratings = records.ratings.filterNot { it.tripNo == tripNo } + MyRating(tripNo, ratingScore, tagNos, now),
                promptedTripNos = records.promptedTripNos + tripNo,
            )
        }
        return saved
    }

    override suspend fun markPrompted(tripNo: Long) {
        local.update { it.copy(promptedTripNos = it.promptedTripNos + tripNo) }
    }

    override suspend fun markGuidedTripsEnded() {
        val now = timeSource.nowMillis()
        local.update { RecommendationRules.markGuidedTripsEnded(it, now) }
    }
}
