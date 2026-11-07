package com.example.travelcrumbs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.ElapsedTime
import com.example.travelcrumbs.domain.model.TripStatus

/** 여행 상태 글자 = 표준 TRIP_STATUS 코드명(스펙 app-shell-v5 2절 공통 · N8) — 홈 · 주행 화면 공통. */
@Composable
fun tripStatusText(status: TripStatus): String = stringResource(
    when (status) {
        TripStatus.RECORDING -> R.string.trip_status_recording
        TripStatus.COMPLETED -> R.string.trip_status_completed
        TripStatus.INTERRUPTED -> R.string.trip_status_interrupted
        TripStatus.CANCELED -> R.string.trip_status_canceled
    },
)

/** 여행명 — 없으면 「새 여행」(D23 · 기본 문구는 화면 문구라 모델 값이 아님). */
@Composable
fun tripNameText(tripName: String?): String = tripName ?: stringResource(R.string.trip_name_default)

/** 「경과 {분}분」 · 「경과 {시}시간 {분}분」(#6 2-5 · N7). */
@Composable
fun elapsedText(elapsedMinutes: Long): String {
    val time = ElapsedTime.fromMinutes(elapsedMinutes)
    return if (time.underAnHour) {
        stringResource(R.string.elapsed_minutes, time.minutes)
    } else {
        stringResource(R.string.elapsed_hours_minutes, time.hours, time.minutes)
    }
}
