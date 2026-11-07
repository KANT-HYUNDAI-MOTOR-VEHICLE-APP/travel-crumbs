package com.example.travelcrumbs.domain.model

/** 여행 한 건(trip_mst) — 화면이 쓰는 값만. */
data class Trip(
    val tripNo: Long,
    val tripName: String,
    val statusCode: String,
    val companionCodes: List<String>,
)

/** 코드 한 줄(cd_dtl) — code 는 저장값, label 은 화면 글자. */
data class CodeItem(
    val code: String,
    val label: String,
)

/** 서버가 「이미 기록 중인 여행이 있음」(409 TRIP_ALREADY_RECORDING)으로 거절. */
class TripAlreadyRecordingException : RuntimeException("TRIP_ALREADY_RECORDING")
