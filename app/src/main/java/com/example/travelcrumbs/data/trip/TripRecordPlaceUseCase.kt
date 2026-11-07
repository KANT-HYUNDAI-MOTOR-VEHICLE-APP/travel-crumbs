package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.RoutePoint
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

/**
 * 「이 장소 기록」(A-05) — 지금 위치를 내비에서 받아 기록 중인 여행에 임시 저장(trip_place_dtl · draft_yn = Y).
 * 재확인 없이 바로 저장. 기록 중인 여행이 없거나 위치 · 서버 실패면 FAILED.
 */
class TripRecordPlaceUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val timeSource: TimeSource,
) : RecordPlaceUseCase {

    override suspend fun invoke(): ActionResult {
        val trip = tripRepository.activeTrip.value ?: return ActionResult.FAILED
        val location = runCatching {
            withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.currentLocation() }
        }.getOrNull()
        if (location == null || !location.isValid) return ActionResult.FAILED
        val point = RoutePoint(location.lat, location.lng, timeSource.nowMillis())
        return tripRepository.recordPlace(trip.tripUuid, point)
            .fold(onSuccess = { ActionResult.DONE }, onFailure = { ActionResult.FAILED })
    }
}
