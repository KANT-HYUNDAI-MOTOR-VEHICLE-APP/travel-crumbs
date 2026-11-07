package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.domain.TripNameSuggester
import javax.inject.Inject

/** Gleo AI 가 붙기 전 임시 — 추천 없이 지금 이름을 그대로 돌려줌. Gleo 구현이 생기면 di/TripModule 에서 교체. */
class PassThroughTripNameSuggester @Inject constructor() : TripNameSuggester {
    override suspend fun suggest(current: String): String = current
}
