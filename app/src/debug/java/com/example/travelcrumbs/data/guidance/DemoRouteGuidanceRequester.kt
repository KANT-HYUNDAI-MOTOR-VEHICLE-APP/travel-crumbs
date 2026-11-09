package com.example.travelcrumbs.data.guidance

import android.util.Log
import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import javax.inject.Inject
import javax.inject.Singleton

/**
 * debug 빌드 전용 시연용 길안내 요청(#37) — 차량 내비를 부르지 않고 요청을 기록한 뒤 성공으로 답함.
 * 기록은 장소 번호 · 이름만(좌표는 로그에 남기지 않음). 확인: `adb logcat -s DemoRouteGuidance`.
 */
@Singleton
class DemoRouteGuidanceRequester @Inject constructor() : RouteGuidanceRequester {

    private val _requested = mutableListOf<Long>()

    /** 지금까지 요청한 장소 번호(순서대로). */
    val requestedPlaceNos: List<Long> get() = synchronized(_requested) { _requested.toList() }

    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult {
        synchronized(_requested) { _requested += destination.placeNo }
        Log.i(TAG, "길안내 요청(시연용) place_no=${destination.placeNo} place_nm=${destination.placeNm}")
        return GuidanceRequestResult.Started
    }

    private companion object {
        const val TAG = "DemoRouteGuidance"
    }
}
