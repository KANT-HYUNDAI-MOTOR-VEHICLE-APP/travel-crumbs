package com.example.travelcrumbs.data.navi

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.NaviLocation
import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.NaviRouteState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull

/** 화면 · ViewModel 이 내비에 닿는 유일한 길. */
interface NaviRepository {
    val ready: StateFlow<Boolean>

    suspend fun routeState(): NaviRouteState

    /** 현재 위치(위도 · 경도 · 주소) — 내비가 없거나 실패하면 예외. */
    suspend fun currentLocation(): NaviLocation

    /**
     * 장소명 후보(A-06) — 내비 목적지 · 경유 장소 · 최근 목적지 · 즐겨찾기를 모음.
     * 하나가 실패하거나 늦어도 나머지는 돌려줌. 같은 POI 는 한 번만.
     */
    suspend fun placeCandidates(): List<NaviPlace>
}

@Singleton
class DefaultNaviRepository @Inject constructor(
    private val dataSource: NaviInfoDataSource,
) : NaviRepository {

    override val ready: StateFlow<Boolean> = dataSource.ready

    override suspend fun routeState(): NaviRouteState = dataSource.routeState()

    override suspend fun currentLocation(): NaviLocation = dataSource.currentLocation()

    override suspend fun placeCandidates(): List<NaviPlace> {
        val sources: List<suspend () -> List<NaviPlace>> = listOf(
            { dataSource.destination() },
            { dataSource.waypoints() },
            { dataSource.recentDestinations() },
            { dataSource.bookmarks() },
        )
        return sources
            .flatMap { source ->
                runCatching { withTimeoutOrNull(CANDIDATE_TIMEOUT_MS) { source() } }.getOrNull().orEmpty()
            }
            .distinctBy { it.poiId ?: "${it.name}@${it.lat},${it.lng}" }
    }

    private companion object {
        /** 후보 조회 하나를 기다리는 한도 — 목적지가 없으면 내비가 답하지 않을 수 있음. */
        const val CANDIDATE_TIMEOUT_MS = AppConfig.SDK_CALL_TIMEOUT_MS / 4
    }
}
