package com.example.travelcrumbs.domain

import java.time.LocalDate

/**
 * 여행명 기본값(A-04) — 「2026.11.05 여행」. 사용자가 「수정」으로 고침(Gleo 추천 자리 · TripNameSuggester).
 * 저장은 trip_mst.trip_nm 하나(결정 로그 D11).
 */
object TripName {
    const val MAX_LENGTH = 100

    fun default(date: LocalDate): String =
        "%04d.%02d.%02d 여행".format(date.year, date.monthValue, date.dayOfMonth)
}
