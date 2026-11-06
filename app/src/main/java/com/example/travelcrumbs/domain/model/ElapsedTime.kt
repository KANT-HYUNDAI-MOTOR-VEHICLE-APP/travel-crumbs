package com.example.travelcrumbs.domain.model

/** 경과 표시용 나눔 — 1시간 미만이면 [hours] = 0(「경과 {분}분」) · 이상이면 「경과 {시}시간 {분}분」. */
data class ElapsedTime(
    val hours: Long,
    val minutes: Long,
) {
    val underAnHour: Boolean get() = hours == 0L

    companion object {
        fun fromMinutes(totalMinutes: Long): ElapsedTime {
            val safe = totalMinutes.coerceAtLeast(0L)
            return ElapsedTime(hours = safe / MINUTES_PER_HOUR, minutes = safe % MINUTES_PER_HOUR)
        }

        private const val MINUTES_PER_HOUR = 60L
    }
}
