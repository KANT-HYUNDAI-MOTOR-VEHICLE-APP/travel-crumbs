package com.example.travelcrumbs.data.navi

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/** 화면 · ViewModel 이 내비에 닿는 유일한 길. */
interface NaviRepository {
    val ready: StateFlow<Boolean>

    suspend fun routeState(): NaviRouteState

    /** 그 장소로 길안내 요청(#4) — 시작 · 오류 알림이 올 때까지 기다림. 한도 · 연결 끊김은 부르는 쪽(awaitRouteResult). */
    suspend fun requestRoute(place: Place): RouteRequestResult

    /** 지금 안내 상태(안내 중 · 목적지) — 시작 알림을 놓쳤을 때 대조(R1). 실패는 예외. */
    suspend fun activeRoute(): NaviActiveRoute
}

@Singleton
class DefaultNaviRepository @Inject constructor(
    private val dataSource: NaviInfoDataSource,
) : NaviRepository {

    override val ready: StateFlow<Boolean> = dataSource.ready

    override suspend fun routeState(): NaviRouteState = dataSource.routeState()

    override suspend fun requestRoute(place: Place): RouteRequestResult = dataSource.requestRoute(place)

    override suspend fun activeRoute(): NaviActiveRoute = dataSource.activeRoute()
}
