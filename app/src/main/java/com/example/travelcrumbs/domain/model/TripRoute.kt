package com.example.travelcrumbs.domain.model

/** 경로 그림(홈 · 여행 상세) — 지도 없이 점 · 선으로 그릴 값. 서버 `GET /trips/{trip_uuid}/route`. */
data class TripRoute(
    /** 간소화한 경로점(기록 순서). */
    val points: List<GeoPoint> = emptyList(),
    /** 기록한 장소(기록 순서). */
    val places: List<RoutePin> = emptyList(),
) {
    val isEmpty: Boolean
        get() = points.isEmpty() && places.isEmpty()
}

data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

/** 경로 위 장소 핀 — 임시 기록이면 이름이 없음. */
data class RoutePin(
    val tripPlaceNo: Long,
    val lat: Double,
    val lng: Double,
    val isDraft: Boolean,
    val name: String?,
)
