package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.domain.EndTripUseCase
import com.example.travelcrumbs.domain.model.TripDetail
import javax.inject.Inject

/**
 * 여행 종료(A-08) — 남은 경로점을 먼저 보내고 서버에 종료 요청(종료 뒤에는 서버가 경로점을 받지 않음).
 * 서버가 완료 처리 · 종료 시각 · 거리 · 소요시간 · 간소화 경로 · 상태 이력을 한 번에 저장.
 */
class RouteFlushingEndTripUseCase @Inject constructor(
    private val routeRecorder: RouteRecorder,
    private val tripRepository: TripRepository,
) : EndTripUseCase {
    override suspend fun invoke(tripUuid: String): Result<TripDetail> {
        routeRecorder.flushNow()
        return tripRepository.endTrip(tripUuid)
    }
}
