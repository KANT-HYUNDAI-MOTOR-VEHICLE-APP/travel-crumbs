package com.example.travelcrumbs.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.NaviLocation
import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.NaviRouteState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Pleos NaviHelper 를 감싸는 유일한 파일 — `ai.pleos.*` 타입은 이 패키지 밖으로 나가지 않음.
 * 쓰는 범위는 조회만(경로 상태 · 현재 위치 · 목적지 · 경유지 · 최근 목적지 · 즐겨찾기) → 권한은 모두 `pleos.car.permission.NAVI_ROUTE` 하나.
 */
@Singleton
class NaviInfoDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val naviHelper = NaviHelper(context.applicationContext)
    private val initialized = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)

    /** 내비 서비스에 붙었는지. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        runCatching {
            naviHelper.initialize(
                { _ready.value = true },
                { error ->
                    _ready.value = false
                    Log.w(TAG, "NaviHelper initialize 실패: ${error.javaClass.simpleName}")
                },
            )
        }.onFailure {
            _ready.value = false
            Log.w(TAG, "NaviHelper initialize 호출 실패: ${it.javaClass.simpleName}")
        }
    }

    fun release() {
        if (initialized.compareAndSet(true, false)) {
            _ready.value = false
            runCatching { naviHelper.release() }
                .onFailure { Log.w(TAG, "NaviHelper release 실패: ${it.javaClass.simpleName}") }
        }
    }

    /** 경로 상태 한 번 조회 — 결과는 리스너로 돌아옴. */
    suspend fun routeState(): NaviRouteState = suspendCancellableCoroutine { continuation ->
        val listener = object : NaviHelperEventListener {
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(routeStateInfo.toDomain())
            }

            override fun onError(errorCode: NaviErrorCode) {
                naviHelper.removeListener(this)
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException(errorCode.name))
                }
            }
        }
        naviHelper.addListener(listener)
        continuation.invokeOnCancellation { naviHelper.removeListener(listener) }
        runCatching { naviHelper.getRouteStateInfo() }
            .onFailure { error ->
                naviHelper.removeListener(listener)
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    /** 현재 위치 한 번 조회(위도 · 경도 · 주소) — 경로 기록 · 「이 장소 기록」(A-05). 결과는 리스너로 돌아옴. */
    suspend fun currentLocation(): NaviLocation = suspendCancellableCoroutine { continuation ->
        val listener = object : NaviHelperEventListener {
            override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(currentLocationInfo.toDomain())
            }

            override fun onError(errorCode: NaviErrorCode) {
                naviHelper.removeListener(this)
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException(errorCode.name))
                }
            }
        }
        naviHelper.addListener(listener)
        continuation.invokeOnCancellation { naviHelper.removeListener(listener) }
        runCatching { naviHelper.getCurrentLocationInfo() }
            .onFailure { error ->
                naviHelper.removeListener(listener)
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    /** 지금 목적지 — 목적지가 없으면 내비가 오류로 답할 수 있음(호출하는 쪽이 실패를 빈 값으로 다룸). */
    suspend fun destination(): List<NaviPlace> = query({ naviHelper.getDestinationInfo() }) { resume ->
        object : NaviHelperEventListener {
            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                resume(listOfNotNull(destinationInfo.toDomain()))
            }
        }
    }

    /** 경로에 넣은 경유 장소들. */
    suspend fun waypoints(): List<NaviPlace> = query({ naviHelper.getWaypointInfo() }) { resume ->
        object : NaviHelperEventListener {
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                resume(waypointInfoList.mapNotNull { it.toDomain() })
            }
        }
    }

    /** 최근 목적지. */
    suspend fun recentDestinations(): List<NaviPlace> = query({ naviHelper.getRecentDestinationInfo() }) { resume ->
        object : NaviHelperEventListener {
            override fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>) {
                resume(recentDestinationInfoList.mapNotNull { it.toDomain() })
            }
        }
    }

    /** 즐겨찾기. */
    suspend fun bookmarks(): List<NaviPlace> = query({ naviHelper.getBookmarkInfo() }) { resume ->
        object : NaviHelperEventListener {
            override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) {
                resume(bookmarkInfoList.mapNotNull { it.toDomain() })
            }
        }
    }

    /** 조회 한 번 — [listenerFor] 가 만든 리스너의 결과 또는 onError 로 끝남. */
    private suspend fun <T> query(
        call: () -> Unit,
        listenerFor: (resume: (T) -> Unit) -> NaviHelperEventListener,
    ): T = suspendCancellableCoroutine { continuation ->
        lateinit var listener: NaviHelperEventListener
        val inner = listenerFor { value ->
            naviHelper.removeListener(listener)
            if (continuation.isActive) continuation.resume(value)
        }
        listener = object : NaviHelperEventListener by inner {
            override fun onError(errorCode: NaviErrorCode) {
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(errorCode.name))
            }
        }
        naviHelper.addListener(listener)
        continuation.invokeOnCancellation { naviHelper.removeListener(listener) }
        runCatching { call() }
            .onFailure { error ->
                naviHelper.removeListener(listener)
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    private fun naviPlace(
        name: String?,
        address: String?,
        lat: Double,
        lng: Double,
        poiId: String?,
        poiProvider: String?,
    ): NaviPlace? = name?.takeIf { it.isNotBlank() }?.let {
        NaviPlace(it, address?.takeIf { a -> a.isNotBlank() }, lat, lng, poiId?.takeIf { p -> p.isNotBlank() }, poiProvider)
    }

    private fun DestinationInfo.toDomain() = naviPlace(name, address, latitude, longitude, poiId, poiProvider)

    private fun WaypointInfo.toDomain() = naviPlace(name, address, latitude, longitude, poiId, poiProvider)

    private fun RecentDestinationInfo.toDomain() = naviPlace(name, address, latitude, longitude, poiId, poiProvider)

    private fun BookmarkInfo.toDomain() = naviPlace(name, address, latitude, longitude, poiId, poiProvider)

    private fun CurrentLocationInfo.toDomain(): NaviLocation = NaviLocation(
        lat = latitude,
        lng = longitude,
        address = address?.takeIf { it.isNotBlank() },
    )

    private fun RouteStateInfo.toDomain(): NaviRouteState = NaviRouteState(
        routing = isRouting,
        mode = naviMode.name,
        routeOption = routeOption.name,
        termsAgreed = termsAgreement,
    )

    private companion object {
        const val TAG = "NaviInfoDataSource"
    }
}
