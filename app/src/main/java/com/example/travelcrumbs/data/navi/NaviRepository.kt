package com.example.travelcrumbs.data.navi

import com.example.travelcrumbs.domain.model.NaviLocation
import com.example.travelcrumbs.domain.model.NaviRouteState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/** 화면 · ViewModel 이 내비에 닿는 유일한 길. */
interface NaviRepository {
    val ready: StateFlow<Boolean>

    suspend fun routeState(): NaviRouteState

    /** 현재 위치(위도 · 경도 · 주소) — 내비가 없거나 실패하면 예외. */
    suspend fun currentLocation(): NaviLocation
}

@Singleton
class DefaultNaviRepository @Inject constructor(
    private val dataSource: NaviInfoDataSource,
) : NaviRepository {

    override val ready: StateFlow<Boolean> = dataSource.ready

    override suspend fun routeState(): NaviRouteState = dataSource.routeState()

    override suspend fun currentLocation(): NaviLocation = dataSource.currentLocation()
}
