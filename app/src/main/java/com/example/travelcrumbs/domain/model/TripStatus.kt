package com.example.travelcrumbs.domain.model

/**
 * 여행 상태 — 데이터 표준 v0.3 코드그룹 TRIP_STATUS 와 같은 값 이름(스펙 app-shell-v5 4-1 · N16).
 * 이 이슈가 일으키는 전이는 RECORDING → COMPLETED 하나 · INTERRUPTED · CANCELED 는 값만 정의.
 * 단일 출처는 data/recording 의 RecordingRepository(#6 D1).
 */
enum class TripStatus {
    RECORDING,
    COMPLETED,
    INTERRUPTED,
    CANCELED,
}
