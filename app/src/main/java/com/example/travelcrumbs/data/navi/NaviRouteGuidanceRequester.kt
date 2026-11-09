package com.example.travelcrumbs.data.navi

import android.util.Log
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import com.example.travelcrumbs.domain.awaitRouteResult
import com.example.travelcrumbs.domain.isHeadingTo
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * 추천 안내(#37 B-05 · #41 별점 자격)를 실제 차량 내비로 — #4 의 [NaviRepository.requestRoute] 를 감싼 어댑터(모든 빌드).
 * 기다림은 #4 와 같은 [awaitRouteResult](시작 · 오류 · 연결 끊김 · 한도) · 한도가 지나면(앱이 얼려져 시작 알림을 놓친 경우 포함)
 * 내비의 지금 목적지로 한 번 대조(R1) — 그 장소로 안내 중이면 시작됨.
 * 핀(navi_req_log)은 남기지 않음 — 핀 표는 「가 볼 장소」 후보 id(placeId) 기준이라 추천 장소 번호와 맞지 않음(보고서 「남은 것」).
 */
@Singleton
class NaviRouteGuidanceRequester @Inject constructor(
    private val navi: NaviRepository,
) : RouteGuidanceRequester {

    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult {
        if (!navi.ready.value) return GuidanceRequestResult.Failed
        val place = Place(
            id = PLACE_ID_PREFIX + destination.placeNo,
            name = destination.placeNm,
            address = destination.addr,
            latitude = destination.lat,
            longitude = destination.lng,
        )
        var result = awaitRouteResult(navi.ready, AppConfig.ROUTE_START_TIMEOUT_MILLIS) { navi.requestRoute(place) }
        if (result == TIMEOUT && naviIsHeadingTo(place)) result = RouteRequestResult.Started
        return if (result == RouteRequestResult.Started) GuidanceRequestResult.Started else GuidanceRequestResult.Failed
    }

    private suspend fun naviIsHeadingTo(place: Place): Boolean = try {
        // 얼림이 풀린 직후 쌓인 콜백이 먼저 지나가게(#4 와 같은 기다림)
        delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
        withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { navi.activeRoute() }?.isHeadingTo(place) == true
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Log.w(TAG, "내비 상태 조회 실패: ${error.javaClass.simpleName}")
        false
    }

    private companion object {
        const val TAG = "NaviRouteGuidance"

        /** 내비로만 가는 장소 id(화면 · 로그 · 저장에 안 남음) — 추천 장소 번호 앞에 붙여 「가 볼 장소」 id 와 겹치지 않게. */
        const val PLACE_ID_PREFIX = "reco-"
        val TIMEOUT = RouteRequestResult.Failed(RouteFailReason.TIMEOUT)
    }
}
