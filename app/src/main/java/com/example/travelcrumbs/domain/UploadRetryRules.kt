package com.example.travelcrumbs.domain

/**
 * 핀 다시 보내기 간격(스펙 navigation 4.6 · 결정 기록 U30) — 30초부터 두 배 · 최대 1시간. 순수 계산.
 * [retryIndex] 0 = 첫 실패 뒤 · 1 = 두 번째 실패 뒤 …
 */
fun uploadRetryDelayMillis(retryIndex: Int): Long {
    val initial = AppConfig.PIN_UPLOAD_RETRY_INITIAL_MILLIS
    val max = AppConfig.PIN_UPLOAD_RETRY_MAX_MILLIS
    var delay = initial
    repeat(retryIndex.coerceAtLeast(0)) {
        if (delay >= max) return max
        delay *= 2
    }
    return delay.coerceAtMost(max)
}
