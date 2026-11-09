package com.example.travelcrumbs.domain.model

/**
 * 후보 장소 하나(스펙 navigation 4.1) — 출처는 assets 의 후보 JSON 한 곳.
 * 좌표는 기기 안에서 내비(NaviHelper)로만 감 — 화면 상태 · 로그 · 기기 저장 · 서버에 넣지 않음(4.4).
 */
data class Place(
    /** 앱 안 고유 id — 서버 계약의 placeId 꼴과 같음([PlaceIds.isValid]). */
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

/** 장소 id 꼴 — 서버 계약 `^[a-z0-9][a-z0-9-]{0,63}$`(스펙 5절)와 같음. */
object PlaceIds {
    private val PATTERN = Regex("^[a-z0-9][a-z0-9-]{0,63}$")

    fun isValid(id: String): Boolean = PATTERN.matches(id)
}
