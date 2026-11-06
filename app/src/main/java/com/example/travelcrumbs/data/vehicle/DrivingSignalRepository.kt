package com.example.travelcrumbs.data.vehicle

import com.example.travelcrumbs.domain.combineDrivingState
import com.example.travelcrumbs.domain.model.DrivingState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn

/** 화면 · ViewModel 이 차량 신호에 닿는 유일한 길 — 기어 + 주행 규제를 합성(스펙 app-shell 3-1). */
interface DrivingSignalRepository {
    val drivingState: Flow<DrivingState>
}

@Singleton
class DefaultDrivingSignalRepository @Inject constructor(
    gearDataSource: VehicleGearDataSource,
    uxRestrictionsDataSource: UxRestrictionsDataSource,
) : DrivingSignalRepository {

    /** 구독하는 화면이 여럿이어도 SDK · Car 콜백은 하나 — 마지막 구독이 끝나면 바로 해제하고 옛 값도 버림. */
    private val sharingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val drivingState: Flow<DrivingState> =
        combine(
            gearDataSource.gearFlow(),
            uxRestrictionsDataSource.restrictionFlow(),
        ) { gear, restriction ->
            combineDrivingState(gear, restriction)
        }
            .distinctUntilChanged()
            .shareIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                replay = 1,
            )
}
