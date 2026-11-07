package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException

/** 서버 trip_mst 대역 — 기록 중인 여행은 최대 1건. */
class FakeTripRepository(
    var current: Trip? = null,
    var failWith: Throwable? = null,
) : TripRepository {
    val started = mutableListOf<Pair<String, List<String>>>()

    override suspend fun currentTrip(): Result<Trip?> =
        failWith?.let { Result.failure(it) } ?: Result.success(current)

    override suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip> {
        failWith?.let { return Result.failure(it) }
        if (current != null) return Result.failure(TripAlreadyRecordingException())
        started += tripName to companionCodes
        val trip = Trip(tripNo = started.size.toLong(), tripName = tripName, statusCode = "RECORDING", companionCodes = companionCodes)
        current = trip
        return Result.success(trip)
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
