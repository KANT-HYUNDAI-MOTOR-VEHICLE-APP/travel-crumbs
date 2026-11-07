package com.example.travelcrumbs.domain.model

/** 내비가 알려 준 현재 위치 — data/navi 가 SDK 의 CurrentLocationInfo 를 이 모델로 바꿈. */
data class NaviLocation(
    val lat: Double,
    val lng: Double,
    /** 내비가 준 주소 — 없으면 null. 여행명 추천 재료(Gleo · docs/data/gleo-handoff.md). */
    val address: String?,
) {
    /** 위치를 아직 못 잡으면 내비가 0,0 을 줄 수 있음 — 저장하지 않음. */
    val isValid: Boolean
        get() = !(lat == 0.0 && lng == 0.0) && lat in -90.0..90.0 && lng in -180.0..180.0
}
