package com.example.travelcrumbs.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 서버 `GET /codes/{cd_grp_cd}` 응답 한 줄 — cd_dtl 의 코드 · 코드명. */
@Serializable
data class CodeResponse(
    @SerialName("cd") val cd: String,
    @SerialName("cd_nm") val cdNm: String,
)

/** 서버 `POST /trips` 요청 — 여행 시작 설정(A-04) 「여행 시작」. */
@Serializable
data class TripCreateRequest(
    @SerialName("trip_nm") val tripNm: String,
    @SerialName("companion_type_cds") val companionTypeCds: List<String>,
)

/** 서버 여행 응답 — trip_mst 1행 + 동행자 코드 목록. */
@Serializable
data class TripResponse(
    @SerialName("trip_no") val tripNo: Long,
    @SerialName("trip_uuid") val tripUuid: String,
    @SerialName("trip_nm") val tripNm: String,
    @SerialName("trip_status_cd") val tripStatusCd: String,
    @SerialName("visibility_cd") val visibilityCd: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("companion_type_cds") val companionTypeCds: List<String>,
)

/** 서버 `GET /trips/current` 응답 — 기록 중인 여행이 없으면 trip = null. */
@Serializable
data class CurrentTripResponse(
    @SerialName("trip") val trip: TripResponse? = null,
)
