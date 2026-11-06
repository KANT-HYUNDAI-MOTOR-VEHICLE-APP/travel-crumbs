package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.Place
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 내비가 지금 [place] 로 안내 중인가(R1 — onRouteStarted 를 놓쳤을 때의 대조). 순수 계산.
 * 안내 중이고 목적지 좌표가 [AppConfig.ROUTE_MATCH_RADIUS_METERS] 안이며 목적지 이름이 우리가 넘긴 이름과 같을 때만 참.
 */
fun NaviActiveRoute.isHeadingTo(place: Place): Boolean =
    routing &&
        destinationName.trim() == place.name.trim() &&
        distanceMeters(destinationLatitude, destinationLongitude, place.latitude, place.longitude) <=
        AppConfig.ROUTE_MATCH_RADIUS_METERS

/** 하버사인 거리(미터). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
