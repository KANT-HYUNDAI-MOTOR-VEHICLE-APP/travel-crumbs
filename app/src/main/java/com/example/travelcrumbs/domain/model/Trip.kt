package com.example.travelcrumbs.domain.model

/** 여행 한 건(trip_mst) — 화면이 쓰는 값만. */
data class Trip(
    val tripNo: Long,
    /** 서버 API 에서 여행을 가리키는 값(trip_mst.trip_uuid · 외부 노출용). */
    val tripUuid: String,
    val tripName: String,
    val statusCode: String,
    val companionCodes: List<String>,
    /** 기록한 장소 수(trip_mst.place_cnt) — A-05 「기록된 장소 N」. */
    val placeCount: Int = 0,
)

/** 경로점 한 개(trip_route_point_dtl) — 시각은 기기 벽시계(밀리초). */
data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val recordedAtMillis: Long,
)

/** 코드 한 줄(cd_dtl) — code 는 저장값, label 은 화면 글자, categoryCode 는 속한 카테고리(장소유형에서만 씀). */
data class CodeItem(
    val code: String,
    val label: String,
    val categoryCode: String? = null,
)

/** 서버가 「이미 기록 중인 여행이 있음」(409 TRIP_ALREADY_RECORDING)으로 거절. */
class TripAlreadyRecordingException : RuntimeException("TRIP_ALREADY_RECORDING")
