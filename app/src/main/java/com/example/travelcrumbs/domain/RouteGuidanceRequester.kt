package com.example.travelcrumbs.domain

/**
 * 길안내 요청 포트(#37 B-05) — 차량 내비로 목적지 하나를 넘기고 시작 · 실패를 기다림.
 * 바인딩: [TrackerRouteGuidanceRequester](성호님 길안내 입구 GuideRequestTracker 하나로 · #51 U7 · 모든 빌드 · di/GuidanceModule).
 */
interface RouteGuidanceRequester {
    suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult
}

/** 안내 목적지 — 좌표는 기기 안에서 내비로만 감(화면 상태 · 로그 · 서버에 넣지 않음). */
data class GuidanceDestination(
    val placeNo: Long,
    val placeNm: String,
    val addr: String,
    val lat: Double,
    val lng: Double,
)

/** 안내 요청 결과 — 표준 NAVI_REQ_STATUS 의 SUCCESS · FAILED 에 대응. */
sealed interface GuidanceRequestResult {
    data object Started : GuidanceRequestResult

    data object Failed : GuidanceRequestResult
}
