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
    @SerialName("place_cnt") val placeCnt: Int = 0,
    @SerialName("companion_type_cds") val companionTypeCds: List<String>,
)

/** 서버 `GET /trips/current` 응답 — 기록 중인 여행이 없으면 trip = null. */
@Serializable
data class CurrentTripResponse(
    @SerialName("trip") val trip: TripResponse? = null,
)

/** 경로점 한 개 — 시각은 시간대 포함 ISO-8601(서버가 시간대 없는 값은 거절). */
@Serializable
data class RoutePointDto(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("recorded_at") val recordedAt: String,
)

/** 서버 `POST /trips/{trip_uuid}/route-points` 요청 — 모아 둔 경로점을 한 번에. */
@Serializable
data class RoutePointsCreateRequest(
    @SerialName("points") val points: List<RoutePointDto>,
)

@Serializable
data class RoutePointsCreateResponse(
    @SerialName("saved_cnt") val savedCnt: Int,
)

/** 서버 `POST /trips/{trip_uuid}/places` 요청 — 「이 장소 기록」(A-05) 위치 · 시각. */
@Serializable
data class TripPlaceCreateRequest(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("recorded_at") val recordedAt: String,
)

/** 서버 장소 기록 응답 — 임시 기록(draft_yn = Y) + 저장 후 장소 수. */
@Serializable
data class TripPlaceResponse(
    @SerialName("trip_place_no") val tripPlaceNo: Long,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("draft_yn") val draftYn: String,
    @SerialName("place_cnt") val placeCnt: Int,
)
