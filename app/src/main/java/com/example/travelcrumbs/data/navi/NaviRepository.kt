package com.example.travelcrumbs.data.navi

import android.util.Log
import com.example.travelcrumbs.domain.NaviGuidance
import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면 · ViewModel · 길안내 요청 입구가 내비에 닿는 유일한 길.
 * 길안내 요청 · 대조([NaviGuidance] — domain 의 입구가 씀)에 debug 진단 카드의 경로 상태 조회를 더함.
 */
interface NaviRepository : NaviGuidance {
    override val ready: StateFlow<Boolean>

    suspend fun routeState(): NaviRouteState
}

@Singleton
class DefaultNaviRepository @Inject constructor(
    private val dataSource: NaviInfoDataSource,
) : NaviRepository {

    override val ready: StateFlow<Boolean> = dataSource.ready

    override suspend fun routeState(): NaviRouteState = dataSource.routeState()

    override suspend fun requestRoute(place: Place): RouteRequestResult = dataSource.requestRoute(place)

    override suspend fun activeRoute(): NaviActiveRoute = try {
        dataSource.activeRoute()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        // 대조 조회 실패는 부르는 쪽이 「안내 중 아님」으로 봄 — 로그엔 예외 이름만(4.4)
        Log.w(TAG, "내비 상태 조회 실패: ${error.javaClass.simpleName}")
        throw error
    }

    private companion object {
        const val TAG = "NaviRepository"
    }
}
