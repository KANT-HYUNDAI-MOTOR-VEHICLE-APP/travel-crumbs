package com.example.travelcrumbs.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import com.example.travelcrumbs.domain.model.RouteFailReason

/**
 * 내비 오류 코드 → 실패 이유(스펙 navigation 4.2 · 결정 기록 U5).
 * Pleos 문서에 값 목록이 없어 SDK(navi-data 3.0.4) `NaviErrorCode` 의 값 이름을 근거로 함 ·
 * 표에 없는(새로 생긴) 값은 ROUTE_NOT_FOUND. 이름으로 대응하므로 SDK 에 값이 늘어도 컴파일이 깨지지 않음.
 */
internal fun routeFailReasonOf(naviErrorName: String): RouteFailReason = when (naviErrorName) {
    "CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE" -> RouteFailReason.TOO_CLOSE
    "CAUSE_SEARCH_ROUTE_OVER_DISTANCE" -> RouteFailReason.TOO_FAR
    "CAUSE_LOW_BATTERY" -> RouteFailReason.LOW_BATTERY
    else -> RouteFailReason.ROUTE_NOT_FOUND
}

internal fun NaviErrorCode.toFailReason(): RouteFailReason = routeFailReasonOf(name)
