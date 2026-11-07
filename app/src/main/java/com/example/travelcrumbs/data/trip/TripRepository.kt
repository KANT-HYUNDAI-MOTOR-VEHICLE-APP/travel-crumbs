package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.RoutePointDto
import com.example.travelcrumbs.data.remote.RoutePointsCreateRequest
import com.example.travelcrumbs.data.remote.TripCreateRequest
import com.example.travelcrumbs.data.remote.TripPlaceCreateRequest
import com.example.travelcrumbs.data.remote.TripResponse
import com.example.travelcrumbs.domain.model.RoutePoint
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

/**
 * 여행(trip_mst · trip_companion_dtl · trip_status_hist · trip_route_point_dtl · trip_place_dtl)의 창구.
 * 기록 중인 여행은 [activeTrip] 하나로 홈 · 경로 기록이 같은 값을 봄.
 */
interface TripRepository {
    /** 기록 중인 여행 — 없으면 null. [currentTrip] · [startTrip] · [recordPlace] 가 갱신. */
    val activeTrip: StateFlow<Trip?>

    /** 서버에서 기록 중인 여행을 다시 읽음 — 없으면 null. */
    suspend fun currentTrip(): Result<Trip?>

    /** 여행 시작(A-04) — 기록 중인 여행이 이미 있으면 [TripAlreadyRecordingException]. */
    suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip>

    /** 경로점 묶음 저장(A-05). */
    suspend fun addRoutePoints(tripUuid: String, points: List<RoutePoint>): Result<Unit>

    /** 「이 장소 기록」 임시 저장(A-05) — 저장 후 장소 수를 돌려줌. */
    suspend fun recordPlace(tripUuid: String, point: RoutePoint): Result<Int>
}

@Singleton
class DefaultTripRepository @Inject constructor(
    private val api: AppApi,
) : TripRepository {

    private val active = MutableStateFlow<Trip?>(null)
    override val activeTrip: StateFlow<Trip?> = active.asStateFlow()

    override suspend fun currentTrip(): Result<Trip?> = runCatching {
        api.getCurrentTrip().trip?.toDomain()
    }.onSuccess { active.value = it }

    override suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip> =
        runCatching {
            api.createTrip(TripCreateRequest(tripNm = tripName, companionTypeCds = companionCodes)).toDomain()
        }.recoverCatching { error ->
            throw if (error is HttpException && error.code() == HTTP_CONFLICT) TripAlreadyRecordingException() else error
        }.onSuccess { active.value = it }

    override suspend fun addRoutePoints(tripUuid: String, points: List<RoutePoint>): Result<Unit> = runCatching {
        api.createRoutePoints(tripUuid, RoutePointsCreateRequest(points.map { it.toDto() }))
        Unit
    }

    override suspend fun recordPlace(tripUuid: String, point: RoutePoint): Result<Int> = runCatching {
        api.createPlace(
            tripUuid,
            TripPlaceCreateRequest(lat = point.lat, lng = point.lng, recordedAt = point.recordedAtMillis.toIso()),
        ).placeCnt
    }.onSuccess { count ->
        active.update { trip -> if (trip?.tripUuid == tripUuid) trip.copy(placeCount = count) else trip }
    }

    private fun TripResponse.toDomain(): Trip = Trip(
        tripNo = tripNo,
        tripUuid = tripUuid,
        tripName = tripNm,
        statusCode = tripStatusCd,
        companionCodes = companionTypeCds,
        placeCount = placeCnt,
    )

    private fun RoutePoint.toDto(): RoutePointDto =
        RoutePointDto(lat = lat, lng = lng, recordedAt = recordedAtMillis.toIso())

    /** 시간대 포함 ISO-8601 — 예: 2026-11-07T18:10:00+09:00. */
    private fun Long.toIso(): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault()).toString()

    private companion object {
        const val HTTP_CONFLICT = 409
    }
}
