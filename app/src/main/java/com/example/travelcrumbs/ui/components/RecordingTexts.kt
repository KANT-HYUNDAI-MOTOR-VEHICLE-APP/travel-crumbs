package com.example.travelcrumbs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.ElapsedTime
import com.example.travelcrumbs.domain.model.RecordingStatus

/** 「기록 중」 · 「일시정지」 — 홈 · 주행 화면 공통. */
@Composable
fun recordingStatusText(status: RecordingStatus): String = stringResource(
    when (status) {
        RecordingStatus.RECORDING -> R.string.recording_status_recording
        RecordingStatus.PAUSED -> R.string.recording_status_paused
    },
)

/** 「경과 {분}분」 · 「경과 {시}시간 {분}분」(스펙 app-shell 2-5). */
@Composable
fun elapsedText(elapsedMinutes: Long): String {
    val time = ElapsedTime.fromMinutes(elapsedMinutes)
    return if (time.underAnHour) {
        stringResource(R.string.elapsed_minutes, time.minutes)
    } else {
        stringResource(R.string.elapsed_hours_minutes, time.hours, time.minutes)
    }
}
