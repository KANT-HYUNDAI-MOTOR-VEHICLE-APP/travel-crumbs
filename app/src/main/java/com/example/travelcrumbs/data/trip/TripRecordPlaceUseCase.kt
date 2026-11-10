package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.RoutePoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

/**
 * 「이 장소 기록」(A-05) — 지금 위치를 내비에서 받아 기록 중인 여행에 임시 저장(trip_place_dtl · draft_yn = Y).
 * Gleo가 발급한 키는 기존 장소 저장에 전달하며 수동 기록에는 사용하지 않음.
 */
class TripRecordPlaceUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val timeSource: TimeSource,
) : RecordPlaceUseCase {

    override suspend fun invoke(idempotencyKey: UUID?): ActionResult {
        val trip = tripRepository.activeTrip.value ?: return ActionResult.FAILED
        val location = runCatching {
            withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.currentLocation() }
        }.getOrNull()
        if (location == null || !location.isValid) return ActionResult.FAILED
        val point = RoutePoint(location.lat, location.lng, timeSource.nowMillis())
        return tripRepository.recordPlace(trip.tripUuid, point, idempotencyKey)
            .fold(
                onSuccess = { ActionResult.DONE },
                onFailure = { error ->
                    if (idempotencyKey == null || (error is HttpException && error.code() in REJECTED_PLACE_CODES)) {
                        ActionResult.FAILED
                    } else {
                        ActionResult.UNCONFIRMED
                    }
                },
            )
    }

    private companion object {
        val REJECTED_PLACE_CODES = setOf(401, 403, 404, 409, 422)
    }
}
