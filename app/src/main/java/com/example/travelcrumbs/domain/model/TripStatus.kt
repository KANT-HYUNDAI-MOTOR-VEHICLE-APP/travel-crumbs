package com.example.travelcrumbs.domain.model

/**
 * 여행 상태 — 데이터 표준 v0.4 코드그룹 TRIP_STATUS 와 같은 값 이름 · 다섯(스펙 app-shell-v5 4-1 · D30 · N16).
 * 이 이슈가 일으키는 전이는 RECORDING ↔ PAUSED · RECORDING → COMPLETED · PAUSED → COMPLETED.
 * INTERRUPTED · CANCELED 는 값만 정의. 단일 출처는 data/recording 의 RecordingRepository(#6 D1).
 */
enum class TripStatus {
    RECORDING,
    PAUSED,
    COMPLETED,
    INTERRUPTED,
    CANCELED,
    ;

    /** 진행 중 여행 = RECORDING 또는 PAUSED(스펙 2절 공통) — 「이 장소 기록」 · 「여행 종료」가 눌리는 상태. */
    val inProgress: Boolean get() = this == RECORDING || this == PAUSED
}
