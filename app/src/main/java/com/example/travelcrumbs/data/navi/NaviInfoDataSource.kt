package com.example.travelcrumbs.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
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
 * 쓰는 범위: 조회 getRouteStateInfo · getDestinationInfo(권한 `NAVI_ROUTE`) · 목적지 설정 requestRoute(권한 `NAVI_ROUTE_SEARCH` · #4).
 * 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations
 */
@Singleton
class NaviInfoDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val naviHelper = NaviHelper(context.applicationContext)
    private val initialized = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    private val queryWindow = NaviQueryWindow()

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
    suspend fun routeState(): NaviRouteState = routeStateInfo().toDomain()

    /**
     * 지금 안내 상태 — 경로 상태(안내 중인가) + 목적지(이름 · 좌표). 시작 알림을 놓쳤을 때의 대조(R1)에만 씀.
     * 「안내 중」 = naviMode 가 ROUTE_GUIDING · SIMULATION_GUIDING. `isRouting` 은 보지 않음 — 에뮬레이터 실측(2026-11-06)에서
     * 해운대 안내 중에 isRouting=false · naviMode=ROUTE_GUIDING. 안내 중이 아니면 목적지를 묻지 않음.
     * 값은 로그에 남기지 않음(좌표 · 스펙 navigation 4.4).
     */
    suspend fun activeRoute(): NaviActiveRoute {
        val routing = routeStateInfo().naviMode.name in GUIDING_MODES
        if (!routing) return NaviActiveRoute(routing = false, destinationName = "", 0.0, 0.0)
        val destination = destinationInfo()
        return NaviActiveRoute(
            routing = true,
            destinationName = destination.name,
            destinationLatitude = destination.latitude,
            destinationLongitude = destination.longitude,
        )
    }

    private suspend fun destinationInfo(): DestinationInfo = queryWindow.around { destinationInfoOnce() }

    private suspend fun destinationInfoOnce(): DestinationInfo = suspendCancellableCoroutine { continuation ->
        val listener = object : NaviHelperEventListener {
            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(destinationInfo)
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
        runCatching { naviHelper.getDestinationInfo() }
            .onFailure { error ->
                naviHelper.removeListener(listener)
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    private suspend fun routeStateInfo(): RouteStateInfo = queryWindow.around { routeStateInfoOnce() }

    private suspend fun routeStateInfoOnce(): RouteStateInfo = suspendCancellableCoroutine { continuation ->
        val listener = object : NaviHelperEventListener {
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(routeStateInfo)
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

    /**
     * 목적지 설정(스펙 navigation 4.1) — 좌표 · 이름 · 주소만 넘김 · POI 식별자는 빈 문자열(SDK 칸이 null 불가 · U6) · 경로 옵션은 내비 기본값(U12).
     * 결과 = 경로 탐색 시작 알림(onRouteStarted · U4) 또는 오류 알림. 응답 대기 한도 · 연결 끊김은 부르는 쪽이 봄.
     * 로그에는 콜백 이름 · 오류 코드 이름 · 예외 종류만(좌표 · 이름 · 주소 없음 · 4.4).
     */
    suspend fun requestRoute(place: Place): RouteRequestResult = suspendCancellableCoroutine { continuation ->
        val listener = object : NaviHelperEventListener {
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
                Log.i(TAG, "onRouteStarted")
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(RouteRequestResult.Started)
            }

            override fun onError(errorCode: NaviErrorCode) {
                if (queryWindow.open) {
                    // 상태 조회 중 — 그 조회의 오류일 수 있어 요청 결과로 쓰지 않음(리뷰 P3 · 계속 기다림)
                    Log.w(TAG, "onError ${errorCode.name} — 조회 중이라 무시")
                    return
                }
                Log.w(TAG, "onError ${errorCode.name}")
                naviHelper.removeListener(this)
                if (continuation.isActive) continuation.resume(RouteRequestResult.Failed(errorCode.toFailReason()))
            }
        }
        naviHelper.addListener(listener)
        continuation.invokeOnCancellation { naviHelper.removeListener(listener) }
        runCatching { naviHelper.requestRoute(place.toRouteInfo()) }
            .onFailure { error ->
                Log.w(TAG, "requestRoute 호출 실패: ${error.javaClass.simpleName}")
                naviHelper.removeListener(listener)
                if (continuation.isActive) {
                    continuation.resume(RouteRequestResult.Failed(RouteFailReason.REQUEST_FAILED))
                }
            }
    }

    private fun Place.toRouteInfo(): RouteInfo = RouteInfo(
        latitude = latitude,
        longitude = longitude,
        displayLatitude = null,
        displayLongitude = null,
        poiName = name,
        poiId = "",
        poiSubId = "",
        address = address,
    )

    private fun RouteStateInfo.toDomain(): NaviRouteState = NaviRouteState(
        routing = isRouting,
        mode = naviMode.name,
        routeOption = routeOption.name,
        termsAgreed = termsAgreement,
    )

    private companion object {
        const val TAG = "NaviInfoDataSource"

        /** 목적지로 안내 중인 내비 모드(SDK NaviMode 이름 — NOT_WORKING · SAFE_GUIDING 은 안내 아님). */
        val GUIDING_MODES = setOf("ROUTE_GUIDING", "SIMULATION_GUIDING")
    }
}
