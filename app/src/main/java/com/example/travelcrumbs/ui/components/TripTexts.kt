package com.example.travelcrumbs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.TripSummary
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 여행 카드 한 줄 요약 — 날짜 · 상태(기록 중 · 일시정지) 또는 거리 · 장소 수. 여행 목록 · 홈 최근 여행 공통. */
@Composable
fun tripSummaryText(trip: TripSummary): String {
    val date = runCatching {
        OffsetDateTime.parse(trip.startAt).atZoneSameInstant(ZoneId.systemDefault()).format(DATE)
    }.getOrDefault("")
    return when (trip.statusCode) {
        RECORDING -> stringResource(R.string.trips_recording, date, trip.placeCount)
        PAUSED -> stringResource(R.string.trips_paused, date, trip.placeCount)
        else -> stringResource(R.string.trips_summary, date, (trip.distanceMeters ?: 0) / 1000.0, trip.placeCount)
    }
}

private const val RECORDING = "RECORDING"
private const val PAUSED = "PAUSED"
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
