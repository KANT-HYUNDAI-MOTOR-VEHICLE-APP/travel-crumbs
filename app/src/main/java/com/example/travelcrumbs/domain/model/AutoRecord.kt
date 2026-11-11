package com.example.travelcrumbs.domain.model

/**
 * 내비 상시 알림 — SDK 타입이 아닌 domain 이벤트(#66 스펙 3.8 「내비 알림」).
 * 시작 알림엔 목적지가 없음(RouteStartInfo 에 좌표 · 주소 없음 · F5) · 도착 알림엔 이름만 씀(F17).
 */
sealed interface NaviEvent {
    data object RouteStarted : NaviEvent

    data class DestinationArrived(val name: String) : NaviEvent

    data object RouteCancelled : NaviEvent
}

/** 내비가 지금 안내 중인 목적지(시작 알림 때 조회 · F6) — 입구 좌표(display*)는 안 씀(4.1). 값은 로그에 남기지 않음. */
data class NaviDestination(
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
)

/**
 * 기록 대기 1건(#66 스펙 4.2) — 직접 안내의 도착을 재출발(PARKED 를 거친 첫 DRIVING) 때 장소로 확정하려고 기억.
 * [arrivedAt] = 도착 알림을 앱이 받은 때(N7) · [parkedSeen] = 도착 뒤 PARKED 를 봤나.
 */
data class PendingArrival(
    val tripNo: Long,
    val name: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val arrivedAt: Long,
    val parkedSeen: Boolean,
)

/** 자동 기록으로 확정한 장소의 이름 원본(3.9) — 철회 때 「사람이 안 고친 자동 이름」을 가리는 데만 씀. */
data class AutoPlaceName(val tripPlaceNo: Long, val name: String)

/** 확정 1건이 넣을 장소 값(4.1) — 이름은 [autoPlaceName] 규칙으로 정한 값. */
data class AutoPlace(
    val userPlaceNm: String?,
    val addr: String?,
    val lat: Double,
    val lng: Double,
    val recordedAt: Long,
)
