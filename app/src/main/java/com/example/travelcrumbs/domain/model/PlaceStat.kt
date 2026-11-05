package com.example.travelcrumbs.domain.model

/** 장소별 안내 건수(서버 `GET /navigation-starts/stats` 의 PlaceStat · 스펙 navigation 5절). */
data class PlaceStat(
    val placeId: String,
    val startCount: Int,
    val userCount: Int,
)
