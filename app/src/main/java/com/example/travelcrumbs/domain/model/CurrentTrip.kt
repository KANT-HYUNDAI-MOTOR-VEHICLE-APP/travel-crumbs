package com.example.travelcrumbs.domain.model

/**
 * 지금 여행 요약(스펙 app-shell-v5 4-1) — 칸 뜻은 ERD v0.3 trip_mst 의
 * trip_nm · trip_status_cd · start_at · end_at · place_cnt 와 1:1. 한 번에 하나.
 *
 * 경과 = 기록 중이면 지금 − 시작 시각 · 그 밖은 종료 시각 − 시작 시각(벽시계 · 앱이 꺼져 있던 동안 포함 · 음수면 0).
 */
data class CurrentTrip(
    /** 여행명 — 없으면 화면이 기본 문구(「새 여행」)를 그림 · 기본 문구는 모델 값으로 저장하지 않음. */
    val tripName: String?,
    val status: TripStatus,
    val startAtMillis: Long,
    val endAtMillis: Long?,
    val placeCount: Int,
) {
    fun elapsedMillis(nowMillis: Long): Long {
        val until = if (status == TripStatus.RECORDING) nowMillis else endAtMillis ?: startAtMillis
        return (until - startAtMillis).coerceAtLeast(0L)
    }

    /**
     * 여행 종료(팝업 「종료」) — RECORDING(표준상 INTERRUPTED 도)이면 COMPLETED · 종료 시각 = [nowMillis].
     * 이미 끝난 여행(COMPLETED · CANCELED)은 바뀌지 않음(표준 비고 「종료 상태 · 변경 불가」 · V21).
     */
    fun ended(nowMillis: Long): CurrentTrip = when (status) {
        TripStatus.RECORDING, TripStatus.INTERRUPTED -> copy(status = TripStatus.COMPLETED, endAtMillis = nowMillis)
        TripStatus.COMPLETED, TripStatus.CANCELED -> this
    }

    companion object {
        /** 새 여행 — 기록 중 · 시작 시각 = [nowMillis] · 종료 시각 없음 · 여행명 없음 · 장소 수 0(#6 D2 · D26). */
        fun start(nowMillis: Long): CurrentTrip = CurrentTrip(
            tripName = null,
            status = TripStatus.RECORDING,
            startAtMillis = nowMillis,
            endAtMillis = null,
            placeCount = 0,
        )
    }
}

/** 화면이 그리는 지금 여행 — 경과는 분 단위로만 바뀜(#6 D6). */
data class RecordingProgress(
    val status: TripStatus,
    val elapsedMinutes: Long,
    val tripName: String? = null,
    val placeCount: Int = 0,
)
