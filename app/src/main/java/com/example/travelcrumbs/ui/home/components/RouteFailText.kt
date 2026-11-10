package com.example.travelcrumbs.ui.home.components

import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RouteFailReason

/** 길안내 요청 실패 이유 → 문구(스펙 navigation 2.1 E3) — 길안내 연결 화면이 씀. */
internal fun RouteFailReason.textRes(): Int = when (this) {
    RouteFailReason.NOT_CONNECTED -> R.string.places_navi_not_connected
    RouteFailReason.TOO_CLOSE -> R.string.places_reason_too_close
    RouteFailReason.TOO_FAR -> R.string.places_reason_too_far
    RouteFailReason.LOW_BATTERY -> R.string.places_reason_low_battery
    RouteFailReason.ROUTE_NOT_FOUND -> R.string.places_reason_route_not_found
    RouteFailReason.TIMEOUT -> R.string.places_reason_timeout
    RouteFailReason.REQUEST_FAILED -> R.string.places_reason_request_failed
}
