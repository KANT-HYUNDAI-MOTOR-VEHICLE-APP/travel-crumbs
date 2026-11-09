package com.example.travelcrumbs.domain

/**
 * 길안내 요청 포트(#37 B-05) — 차량 내비로 목적지 하나를 넘김.
 * 지금 바인딩: debug = 요청을 기록하고 성공으로 답하는 시연용 구현 · release = 「안내할 수 없음」(di/GuidanceModule · 빌드 타입별).
 * #4(`navigation/main`)의 NaviRepository.requestRoute 가 develop 에 들어오면 그것을 감싼 구현으로 바인딩만 바꿈.
 * 모양은 #4 의 Place(name · address · latitude · longitude) → RouteRequestResult(Started · Failed) 와 1:1.
 */
interface RouteGuidanceRequester {
    /** 실제 차량 내비로 안내가 가는지 — 아니면 화면이 「시연용 · 실제 안내는 준비 중」을 함께 보임(release 연결 전). */
    val available: Boolean

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
