package com.example.travelcrumbs.domain.model

/**
 * 기기 안에 남는 기록 상태(스펙 app-shell 4-1 · D3).
 *
 * 경과 = [accumulatedMillis] + (기록 중이면 지금 − [segmentStartMillis]).
 * 벽시계 기준이라 앱이 꺼져 있던 동안도 기록 중이면 경과에 들어감(미확정 N1 의 추천 (b) — builder 보고서에 적음).
 */
data class RecordingSession(
    val status: RecordingStatus,
    /** 지난 기록 구간들의 합(밀리초). */
    val accumulatedMillis: Long,
    /** 지금 기록 구간이 시작된 벽시계 시각(밀리초) · 일시정지 중엔 의미 없음. */
    val segmentStartMillis: Long,
) {
    fun elapsedMillis(nowMillis: Long): Long {
        val running = if (status == RecordingStatus.RECORDING) nowMillis - segmentStartMillis else 0L
        return (accumulatedMillis + running.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    fun toggled(nowMillis: Long): RecordingSession = when (status) {
        RecordingStatus.RECORDING -> RecordingSession(
            status = RecordingStatus.PAUSED,
            accumulatedMillis = elapsedMillis(nowMillis),
            segmentStartMillis = nowMillis,
        )
        RecordingStatus.PAUSED -> copy(
            status = RecordingStatus.RECORDING,
            segmentStartMillis = nowMillis,
        )
    }

    companion object {
        /** 설치 뒤 첫 실행 — 기록 중 · 0분부터(스펙 D2 · 임시). */
        fun start(nowMillis: Long): RecordingSession = RecordingSession(
            status = RecordingStatus.RECORDING,
            accumulatedMillis = 0L,
            segmentStartMillis = nowMillis,
        )
    }
}

/** 화면이 그리는 기록 상태 — 경과는 분 단위로만 바뀜(스펙 D6). */
data class RecordingProgress(
    val status: RecordingStatus,
    val elapsedMinutes: Long,
)
