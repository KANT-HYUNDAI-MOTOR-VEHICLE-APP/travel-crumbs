package com.example.travelcrumbs.data.navi

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviRouteEvent
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 화면 · ViewModel 이 내비에 닿는 유일한 길. */
interface NaviRepository {
    val ready: StateFlow<Boolean>

    suspend fun routeState(): NaviRouteState
}

@Singleton
class DefaultNaviRepository @Inject constructor(
    private val dataSource: NaviInfoDataSource,
) : NaviRepository {

    override val ready: StateFlow<Boolean> = dataSource.ready

    override suspend fun routeState(): NaviRouteState = dataSource.routeState()

    val routeEvents: Flow<NaviRouteEvent> = dataSource.routeEvents

    suspend fun requestRoute(place: Place): RouteRequestResult = dataSource.requestRoute(place)

    suspend fun activeRoute(): NaviActiveRoute = dataSource.activeRoute()
}
