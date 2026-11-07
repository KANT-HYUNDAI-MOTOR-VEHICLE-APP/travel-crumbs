package com.example.travelcrumbs.domain.model

/** 여행 상세(A-08) — 요약 · 간소화 경로 · 장소 타임라인. */
data class TripDetail(
    val tripUuid: String,
    val tripName: String,
    val statusCode: String,
    /** 시작 · 종료 일시 — 서버가 준 ISO-8601 문자열. */
    val startAt: String,
    val endAt: String?,
    /** 거리(m) · 소요시간(초) — 종료 때 계산. */
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val placeCount: Int,
    /** 간소화 경로(인코딩 폴리라인) — 지도 그리기용. */
    val routeData: String?,
    val places: List<TimelinePlace>,
)

/** 타임라인 한 줄(trip_place_dtl) — 기록 순서. */
data class TimelinePlace(
    val tripPlaceNo: Long,
    val recordedAt: String,
    val isDraft: Boolean,
    val userPlaceName: String?,
    val placeTypeName: String?,
    val reviewPolarityCode: String?,
    val memo: String?,
    val tagNames: List<String>,
)
