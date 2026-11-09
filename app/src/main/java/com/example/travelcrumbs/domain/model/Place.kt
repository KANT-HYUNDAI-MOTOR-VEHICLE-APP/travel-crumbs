package com.example.travelcrumbs.domain.model

/**
 * 내비에 전달할 목적지 하나(스펙 navigation 4.1).
 * 좌표는 기기 안에서 내비(NaviHelper)로만 감 — 화면 상태 · 로그 · 기기 저장 · 서버에 넣지 않음(4.4).
 */
data class Place(
    /** 앱 안 고유 id. */
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
