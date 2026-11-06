package com.example.travelcrumbs.domain.model

/** 길안내 요청의 결과 — 내비 콜백을 data/navi 에서 이 모델로 바꿔 내보냄(스펙 navigation 4.1 · 4.2). */
sealed interface RouteRequestResult {
    /** 내비가 경로 탐색 시작을 알림(onRouteStarted · 결정 기록 U4). */
    data object Started : RouteRequestResult

    data class Failed(val reason: RouteFailReason) : RouteRequestResult
}

/** 실패 이유 코드(스펙 navigation 3.1 · 4.2) — 앱 안 값. */
enum class RouteFailReason {
    NOT_CONNECTED,
    TOO_CLOSE,
    TOO_FAR,
    LOW_BATTERY,
    ROUTE_NOT_FOUND,
    TIMEOUT,
    REQUEST_FAILED,
}
