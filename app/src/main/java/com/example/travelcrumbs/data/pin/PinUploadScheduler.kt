package com.example.travelcrumbs.data.pin

/**
 * 대기 핀 다시 보내기 예약(스펙 navigation 4.6 · 결정 기록 U30) — 앱이 꺼져 있어도 · 연결되면.
 * 시험은 fake 로 바꿔 끼워 예약 간격만 셈.
 */
interface PinUploadScheduler {
    /** [retryIndex] 번째 실패 뒤 다시 보내기 — 간격은 domain uploadRetryDelayMillis. */
    fun scheduleRetry(retryIndex: Int)

    /** 앱이 뜰 때 한 번 — 이미 예약이 있으면 그대로(간격을 처음으로 되돌리지 않음). */
    fun scheduleOnAppStart()
}
