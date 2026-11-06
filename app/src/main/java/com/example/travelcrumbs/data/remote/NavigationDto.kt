package com.example.travelcrumbs.data.remote

import kotlinx.serialization.Serializable

/** 요청 칸은 둘뿐(스펙 navigation 5절 · U44) — 좌표 · 이름 · 주소 · userId 칸 없음(android-architecture 규칙 8). */
@Serializable
data class NavigationStartIn(
    val placeId: String,
    /** ISO-8601 · 시간대 필수(예: 2026-11-05T14:00:00+09:00). */
    val startedAt: String,
)

@Serializable
data class NavigationStartOut(
    val id: String,
    val placeId: String,
    val startedAt: String,
    val receivedAt: String,
)

@Serializable
data class PlaceStatOut(
    val placeId: String,
    val startCount: Int,
    val userCount: Int,
)

@Serializable
data class NavigationStatsOut(
    val items: List<PlaceStatOut>,
)
