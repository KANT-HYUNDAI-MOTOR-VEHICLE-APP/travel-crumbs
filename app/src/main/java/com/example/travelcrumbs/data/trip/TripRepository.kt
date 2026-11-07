package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.TripCreateRequest
import com.example.travelcrumbs.data.remote.TripResponse
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/** 여행(trip_mst · trip_companion_dtl · trip_status_hist)의 창구. */
interface TripRepository {
    /** 기록 중인 여행 — 없으면 null. */
    suspend fun currentTrip(): Result<Trip?>

    /** 여행 시작(A-04) — 기록 중인 여행이 이미 있으면 [TripAlreadyRecordingException]. */
    suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip>
}

@Singleton
class DefaultTripRepository @Inject constructor(
    private val api: AppApi,
) : TripRepository {

    override suspend fun currentTrip(): Result<Trip?> = runCatching {
        api.getCurrentTrip().trip?.toDomain()
    }

    override suspend fun startTrip(tripName: String, companionCodes: List<String>): Result<Trip> =
        runCatching {
            api.createTrip(TripCreateRequest(tripNm = tripName, companionTypeCds = companionCodes)).toDomain()
        }.recoverCatching { error ->
            throw if (error is HttpException && error.code() == HTTP_CONFLICT) TripAlreadyRecordingException() else error
        }

    private fun TripResponse.toDomain(): Trip = Trip(
        tripNo = tripNo,
        tripName = tripNm,
        statusCode = tripStatusCd,
        companionCodes = companionTypeCds,
    )

    private companion object {
        const val HTTP_CONFLICT = 409
    }
}
