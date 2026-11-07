package com.example.travelcrumbs.domain.model

/**
 * 내비가 아는 장소(목적지 · 경유 장소 · 최근 목적지 · 즐겨찾기) — A-06 장소명 후보.
 * poiId + poiProvider 가 있으면 공식 장소(place_mst)로 연결(결정 로그 D20).
 */
data class NaviPlace(
    val name: String,
    val address: String?,
    val lat: Double,
    val lng: Double,
    val poiId: String?,
    val poiProvider: String?,
)
