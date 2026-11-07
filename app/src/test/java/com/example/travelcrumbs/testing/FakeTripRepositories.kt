package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.place.PlaceRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.domain.model.PlaceInput
import com.example.travelcrumbs.domain.model.PlaceType
import com.example.travelcrumbs.domain.model.RoutePoint
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException
import com.example.travelcrumbs.domain.model.TripDetail
import com.example.travelcrumbs.domain.model.TripSummary
import com.example.travelcrumbs.domain.model.TripPlace
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

    val renamed = mutableListOf<String>()

    override suspend fun endTrip(tripUuid: String): Result<TripDetail> {
        failWith?.let { return Result.failure(it) }
        val trip = active.value ?: return Result.failure(IllegalStateException("TRIP_NOT_RECORDING"))
        active.value = null
        return Result.success(detailOf(trip.tripUuid, trip.tripName))
    }

    val pauseCalls = mutableListOf<String>()

    override suspend fun pause(tripUuid: String): Result<Trip> = changeStatus("PAUSED")

    override suspend fun resume(tripUuid: String): Result<Trip> = changeStatus("RECORDING")

    private fun changeStatus(status: String): Result<Trip> {
        failWith?.let { return Result.failure(it) }
        val trip = active.value?.copy(statusCode = status) ?: return Result.failure(IllegalStateException("no trip"))
        pauseCalls += status
        active.value = trip
        return Result.success(trip)
    }

    var trips: List<TripSummary> = emptyList()

    override suspend fun myTrips(): Result<List<TripSummary>> =
        failWith?.let { Result.failure(it) } ?: Result.success(trips)

    override suspend fun detail(tripUuid: String): Result<TripDetail> =
        failWith?.let { Result.failure(it) } ?: Result.success(detailOf(tripUuid, renamed.lastOrNull() ?: "2026.11.07 여행"))

    override suspend fun rename(tripUuid: String, tripName: String): Result<TripDetail> {
        failWith?.let { return Result.failure(it) }
        renamed += tripName
        return Result.success(detailOf(tripUuid, tripName))
    }

    private fun detailOf(uuid: String, name: String) = TripDetail(
        tripUuid = uuid,
        tripName = name,
        statusCode = "COMPLETED",
        startAt = "2026-11-07T09:00:00Z",
        endAt = "2026-11-07T11:30:00Z",
        distanceMeters = 4362,
        durationSeconds = 9000,
        placeCount = places.size,
        routeData = "e{neFouvqW",
        places = emptyList(),
    )

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
    /** 그룹별 코드 — 없으면 [result](동행자 코드). */
    private val byGroup: Map<String, List<CodeItem>> = PLACE_CODES,
) : CodeRepository {
    override suspend fun codes(groupCode: String): Result<List<CodeItem>> =
        byGroup[groupCode]?.let { Result.success(it) } ?: result

    companion object {
        /** 마이그레이션 코드값 일부 — 후기성격. */
        val PLACE_CODES = mapOf(
            "REVIEW_POLARITY" to listOf(CodeItem("POSITIVE", "좋았어요"), CodeItem("NEGATIVE", "별로예요")),
        )
    }
}

/** 서버 trip_place_dtl(임시 기록) · tag_mst 대역. */
class FakePlaceRepository(
    drafts: List<TripPlace> = emptyList(),
    var failWith: Throwable? = null,
) : PlaceRepository {
    val completed = mutableListOf<Pair<Long, PlaceInput>>()
    private val draftList = MutableStateFlow(drafts)
    override val drafts: StateFlow<List<TripPlace>> = draftList

    override suspend fun refreshDrafts(tripUuid: String): Result<List<TripPlace>> =
        failWith?.let { Result.failure(it) } ?: Result.success(draftList.value)

    override suspend fun placeTypes(): Result<List<PlaceType>> =
        failWith?.let { Result.failure(it) } ?: Result.success(
            listOf(
                PlaceType("REST_AREA", "휴게소", "MOBILITY", "이동·차량"),
                PlaceType("PARKING", "주차장", "MOBILITY", "이동·차량"),
                PlaceType("BEACH", "바다·해변", "NATURE", "자연"),
                PlaceType("MOUNTAIN", "산", "NATURE", "자연"),
            ),
        )

    override suspend fun tags(): Result<List<Tag>> =
        failWith?.let { Result.failure(it) } ?: Result.success(listOf(Tag(1L, "주차 편함"), Tag(5L, "웨이팅 있음")))

    override suspend fun complete(tripUuid: String, tripPlaceNo: Long, input: PlaceInput): Result<TripPlace> {
        failWith?.let { return Result.failure(it) }
        completed += tripPlaceNo to input
        val done = draftList.value.first { it.tripPlaceNo == tripPlaceNo }.copy(isDraft = false, userPlaceName = input.userPlaceName)
        draftList.value = draftList.value.filterNot { it.tripPlaceNo == tripPlaceNo }
        return Result.success(done)
    }

    override fun clearDrafts() {
        draftList.value = emptyList()
    }
}
