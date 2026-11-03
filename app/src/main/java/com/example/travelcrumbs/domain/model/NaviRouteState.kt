package com.example.travelcrumbs.domain.model

/** 내비 조회 결과 — SDK 의 RouteStateInfo 를 data/navi 에서 이 모델로 바꿔 내보냄. */
data class NaviRouteState(
    val routing: Boolean,
    val mode: String,
    val routeOption: String,
    val termsAgreed: Boolean,
)
