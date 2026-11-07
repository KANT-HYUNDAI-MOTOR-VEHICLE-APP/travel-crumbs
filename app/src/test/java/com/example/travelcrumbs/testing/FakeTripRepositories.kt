package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.domain.model.RoutePoint
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 서버 trip_mst · trip_route_point_dtl · trip_place_dtl 대역 — 기록 중인 여행은 최대 1건. */
class FakeTripRepository(
    current: Trip? = null,
    var failWith: Throwable? = null,
) : TripRepository {
    val started = mutableListOf<Pair<String, List<String>>>()
    val routePoints = mutableListOf<RoutePoint>()
    val places = mutableListOf<RoutePoint>()

    private val active = MutableStateFlow(current)
    override val activeTrip: StateFlow<Trip?> = active

    override suspend fun currentTrip(): Result<Trip?> =
        failWith?.let { Result.failure(it) } ?: Result.success(active.value)

    override suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip> {
        failWith?.let { return Result.failure(it) }
        if (active.value != null) return Result.failure(TripAlreadyRecordingException())
        started += tripName to companionCodes
        val trip = Trip(
            tripNo = started.size.toLong(),
            tripUuid = "uuid-${started.size}",
            tripName = tripName,
            statusCode = "RECORDING",
            companionCodes = companionCodes,
        )
        active.value = trip
        return Result.success(trip)
    }

    override suspend fun addRoutePoints(tripUuid: String, points: List<RoutePoint>): Result<Unit> {
        failWith?.let { return Result.failure(it) }
        routePoints += points
        return Result.success(Unit)
    }

    override suspend fun recordPlace(tripUuid: String, point: RoutePoint): Result<Int> {
        failWith?.let { return Result.failure(it) }
        places += point
        active.value = active.value?.copy(placeCount = places.size)
        return Result.success(places.size)
    }
}

/** cd_dtl COMPANION_TYPE 대역 — 마이그레이션의 코드값과 같은 순서. */
class FakeCodeRepository(
    var result: Result<List<CodeItem>> = Result.success(
        listOf(
            CodeItem("SOLO", "혼자"),
            CodeItem("SPOUSE", "배우자"),
            CodeItem("CHILD", "아이"),
            CodeItem("PARENT", "부모님"),
            CodeItem("PET", "반려동물"),
        ),
    ),
) : CodeRepository {
    override suspend fun codes(groupCode: String): Result<List<CodeItem>> = result
}
