package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GleoTravelRules
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * SHOW_TRAVEL_INFO(#69 스펙 3.3) — 읽기만 함(저장 · 서버 전송 없음).
 * TODAY_RECORDS = 기기 안 여행의 오늘 장소 수 + 지금 여행 상태.
 * RECOMMENDED_ROADMAPS = 말한 지역 우선(U3), 없으면 현재 위치에서 가장 가까운 지역 · 별점순 상위 [AppConfig.GLEO_RECOMMENDATION_COUNT]개.
 * 좌표는 거리 계산에만 쓰고 콜백 · 로그 · 화면 요청에 넣지 않음(지역 이름만).
 */
class GleoTravelInfo @Inject constructor(
    private val trips: TripRepository,
    private val recording: RecordingRepository,
    private val recommendations: RecommendationRepository,
    private val navi: NaviRepository,
    private val timeSource: TimeSource,
) {
    suspend fun show(infoType: String, region: String?): GleoOutcome = when (infoType) {
        AppConfig.GLEO_INFO_TODAY_RECORDS -> todayRecords()
        AppConfig.GLEO_INFO_RECOMMENDED_ROADMAPS -> recommended(region)
        else -> GleoOutcome(GleoCommandPhase.INVALID_REQUEST, GleoStatus.FAIL, AppConfig.GLEO_INPUT_FAIL_MESSAGE)
    }

    private suspend fun todayRecords(): GleoOutcome {
        val count = GleoTravelRules.todayPlaceCount(trips.trips.first(), timeSource.nowMillis(), timeSource.zoneId())
        val progress = recording.progress.first()
        val current = if (progress.tripNo != null && progress.status.inProgress) progress.status.name else NO_TRIP
        val currentCount = if (current == NO_TRIP) 0 else progress.placeCount
        val tripSentence = when (current) {
            NO_TRIP -> AppConfig.GLEO_NO_TRIP_MESSAGE
            else -> AppConfig.GLEO_CURRENT_TRIP_MESSAGE.format(current.lowercase(), currentCount, places(currentCount))
        }
        val data = buildJsonArray {
            add(
                buildJsonObject {
                    put("todayPlaceCount", count)
                    put("currentTripStatus", current)
                    put("currentTripPlaceCount", currentCount)
                },
            )
        }.toString()
        return GleoOutcome(
            phase = GleoCommandPhase.TODAY_RECORDS,
            status = GleoStatus.SUCCESS,
            message = AppConfig.GLEO_TODAY_MESSAGE.format(count, places(count), tripSentence),
            data = data,
            detail = count.toString(),
            screen = GleoScreenRequest.OpenTrips,
        )
    }

    private suspend fun recommended(spoken: String?): GleoOutcome {
        val all = recommendations.sidoRegions().flatMap { recommendations.publicTrips(it.sidoNm) }.distinctBy { it.tripNo }
        val regions: List<Region>? = if (spoken != null) {
            GleoTravelRules.matchRegions(spoken, all).ifEmpty {
                // 사용자 말을 응답에 다시 쓸 때는 글자 · 숫자 · 공백만 · 길이 제한(Pleos 리뷰 #69).
                val echo = spoken.filter { it.isLetterOrDigit() || it == ' ' }.trim().take(AppConfig.GLEO_REGION_ECHO_CHARS)
                return GleoOutcome(
                    phase = GleoCommandPhase.NO_RECOMMENDATION,
                    status = GleoStatus.FAIL,
                    message = if (echo.isEmpty()) AppConfig.GLEO_NO_ROADMAPS_MESSAGE else AppConfig.GLEO_NO_REGION_ROADMAPS_MESSAGE.format(echo),
                    detail = echo.ifEmpty { null },
                )
            }
        } else {
            val here = withTimeoutOrNull(AppConfig.GLEO_LOCATION_WAIT_MILLIS) { navi.currentLocation() }
            GleoTravelRules.nearestRegion(here, all)?.let(::listOf)
        }
        val pool = if (regions == null) all else GleoTravelRules.inAny(all, regions)
        val top = RecommendationRules.rank(pool, RankingSort.RATING, companion = null).take(AppConfig.GLEO_RECOMMENDATION_COUNT)
        if (top.isEmpty()) {
            return GleoOutcome(GleoCommandPhase.NO_RECOMMENDATION, GleoStatus.FAIL, AppConfig.GLEO_NO_ROADMAPS_MESSAGE)
        }
        val label = regions?.let { list -> list.joinToString(", ") { GleoTravelRules.label(it) } }
        return GleoOutcome(
            phase = GleoCommandPhase.RECOMMENDED,
            status = GleoStatus.SUCCESS,
            message = top.joinToString(", ") { it.tripNm }.let { names ->
                if (label == null) AppConfig.GLEO_TOP_ROADMAPS_MESSAGE.format(names) else AppConfig.GLEO_TOP_REGION_ROADMAPS_MESSAGE.format(label, names)
            },
            data = top.toData(),
            detail = label ?: top.first().let { GleoTravelRules.label(it.region) },
            screen = GleoScreenRequest.OpenRecommendations(regions?.singleOrNull()),
        )
    }

    private fun List<RecommendedTrip>.toData(): String = buildJsonArray {
        forEach { trip ->
            add(
                buildJsonObject {
                    put("tripNm", trip.tripNm)
                    put("region", GleoTravelRules.label(trip.region))
                    put("ratingAvg", trip.ratingAvg)
                    put("placeCount", trip.places.size)
                },
            )
        }
    }.toString()

    private fun places(count: Int) = if (count == 1) "place" else "places"

    private companion object {
        const val NO_TRIP = "NONE"
    }
}
