package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.TripDetail

/** 여행 종료(A-08) — 구현은 data/trip/RouteFlushingEndTripUseCase(남은 경로점을 먼저 보내고 종료). */
fun interface EndTripUseCase {
    suspend operator fun invoke(tripUuid: String): Result<TripDetail>
}
