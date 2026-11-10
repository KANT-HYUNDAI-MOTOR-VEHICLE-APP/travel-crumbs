package com.example.travelcrumbs.ui.recommendations.map

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.MapCoord
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/** 지도 렌더러 경계의 저장소 연결. 좌표는 호출한 지도 부품으로만 반환하며 보관하지 않음. */
class RecommendationMapRuntime @Inject constructor(
    users: CurrentUserRepository,
    driving: DrivingSignalRepository,
    private val navi: NaviRepository,
    trips: TripRepository,
) {
    val access = combine(users.consents, driving.drivingState) { consents, state ->
        MapAccess(consents[ConsentType.LOCATION_RECO]?.agreed == true, state == DrivingState.PARKED)
    }.distinctUntilChanged()

    val publicChanges = trips.trips.map { Unit }

    suspend fun locate(access: MapAccess): MapCoord? = withTimeoutOrNull(AppConfig.RECO_MAP_LOCATION_TIMEOUT_MILLIS) {
        if (!access.consent || !access.parked) return@withTimeoutOrNull null
        if (!navi.ready.value) null else navi.currentLocation()?.let { MapCoord(it.lng, it.lat) }
    }
}

data class MapAccess(val consent: Boolean = false, val parked: Boolean = false)
