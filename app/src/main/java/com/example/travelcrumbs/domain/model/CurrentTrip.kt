package com.example.travelcrumbs.domain.model

/**
 * 지금 여행 요약(스펙 app-shell-v5 4-1) — 칸 뜻은 trip_mst 의
 * trip_nm · trip_status_cd · start_at · end_at · place_cnt · duration(기록한 시간 · 일시정지 제외)과 맞춤. 한 번에 하나.
 *
 * 기록 시간(화면의 「경과」) = [accumulatedMillis] + (RECORDING 이면 지금 − [segmentStartMillis]).
 * PAUSED · 종료 상태는 [accumulatedMillis] 그대로(멈춤 · 고정). 벽시계라 앱이 꺼져 있던 동안 RECORDING 이었으면 포함 · 음수면 0.
 */
data class CurrentTrip(
    /** 여행명 — 없으면 화면이 기본 문구(「새 여행」)를 그림 · 기본 문구는 모델 값으로 저장하지 않음. */
    val tripName: String?,
    val status: TripStatus,
    val startAtMillis: Long,
    val endAtMillis: Long?,
    val placeCount: Int,
    /** 지난 기록 구간들의 합(밀리초) — 일시정지 · 종료 때 지금 구간을 여기에 더함. */
    val accumulatedMillis: Long,
    /** 지금 기록 구간이 시작된 벽시계 시각(밀리초) — RECORDING 일 때만 뜻이 있음. */
    val segmentStartMillis: Long,
) {
    fun elapsedMillis(nowMillis: Long): Long {
        val running = if (status == TripStatus.RECORDING) (nowMillis - segmentStartMillis).coerceAtLeast(0L) else 0L
        return (accumulatedMillis + running).coerceAtLeast(0L)
    }

    /**
     * 「기록 일시정지」 · 「기록 재개」 — RECORDING → PAUSED(기록 시간을 그 순간 값으로 멈춤) ·
     * PAUSED → RECORDING(멈춘 값에서 이어짐). 그 밖 상태는 바뀌지 않음(V64).
     */
    fun pauseToggled(nowMillis: Long): CurrentTrip = when (status) {
        TripStatus.RECORDING -> copy(
            status = TripStatus.PAUSED,
            accumulatedMillis = elapsedMillis(nowMillis),
            segmentStartMillis = nowMillis,
        )
        TripStatus.PAUSED -> copy(status = TripStatus.RECORDING, segmentStartMillis = nowMillis)
        TripStatus.COMPLETED, TripStatus.INTERRUPTED, TripStatus.CANCELED -> this
    }

    /**
     * 여행 종료(팝업 「종료」) — RECORDING · PAUSED(표준상 INTERRUPTED 도)이면 COMPLETED · 종료 시각 = [nowMillis] ·
     * 기록 시간 고정(PAUSED 면 일시정지한 순간 값). 이미 끝난 여행(COMPLETED · CANCELED)은 바뀌지 않음(V21).
     */
    fun ended(nowMillis: Long): CurrentTrip = when (status) {
        TripStatus.RECORDING, TripStatus.PAUSED, TripStatus.INTERRUPTED -> copy(
            status = TripStatus.COMPLETED,
            endAtMillis = nowMillis,
            accumulatedMillis = elapsedMillis(nowMillis),
            segmentStartMillis = nowMillis,
        )
        TripStatus.COMPLETED, TripStatus.CANCELED -> this
    }

    companion object {
        /** 새 여행 — 기록 중 · 시작 시각 = [nowMillis] · 종료 시각 없음 · 기록 시간 0 · 여행명 없음 · 장소 수 0(#6 D2 · D26). */
        fun start(nowMillis: Long): CurrentTrip = CurrentTrip(
            tripName = null,
            status = TripStatus.RECORDING,
            startAtMillis = nowMillis,
            endAtMillis = null,
            placeCount = 0,
            accumulatedMillis = 0L,
            segmentStartMillis = nowMillis,
        )
    }
}

/** 화면이 그리는 지금 여행 — 경과(기록 시간)는 분 단위로만 바뀜(#6 D6). */
data class RecordingProgress(
    val status: TripStatus,
    val elapsedMinutes: Long,
    val tripName: String? = null,
    val placeCount: Int = 0,
)
