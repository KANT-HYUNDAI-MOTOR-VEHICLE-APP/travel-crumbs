package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.RoutePoint

/** 장소명 후보(A-06) — 기록한 위치 가까이 있는 내비 장소만, 가까운 순. */
object PlaceCandidates {
    /** 이 거리(미터) 안의 내비 장소만 후보로. */
    const val RADIUS_METERS: Double = 300.0

    fun near(lat: Double, lng: Double, places: List<NaviPlace>): List<NaviPlace> {
        val here = RoutePoint(lat, lng, 0L)
        return places
            .map { it to RouteRules.distanceMeters(here, RoutePoint(it.lat, it.lng, 0L)) }
            .filter { (_, meters) -> meters <= RADIUS_METERS }
            .sortedBy { (_, meters) -> meters }
            .map { (place, _) -> place }
    }
}
