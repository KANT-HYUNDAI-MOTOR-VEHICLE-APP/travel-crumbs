package com.example.travelcrumbs.data.vehicle

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.toDrivingState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 화면 · ViewModel 이 차량 신호에 닿는 유일한 길. */
interface DrivingSignalRepository {
    val drivingState: Flow<DrivingState>
}

@Singleton
class DefaultDrivingSignalRepository @Inject constructor(
    private val dataSource: VehicleGearDataSource,
) : DrivingSignalRepository {

    override val drivingState: Flow<DrivingState> =
        dataSource.gearFlow().map { gear -> gear.toDrivingState() }
}
