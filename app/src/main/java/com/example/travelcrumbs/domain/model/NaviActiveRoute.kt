package com.example.travelcrumbs.domain.model

/**
 * 내비의 지금 안내 상태(조회 결과) — 안내 중인가(내비 모드) + 목적지(이름 · 좌표).
 * 길안내 시작 알림을 놓쳤을 때 대조(R1)에만 씀 · 화면 상태 · 로그 · 저장에 넣지 않음(좌표 · 스펙 navigation 4.4).
 */
data class NaviActiveRoute(
    val routing: Boolean,
    val destinationName: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
)
