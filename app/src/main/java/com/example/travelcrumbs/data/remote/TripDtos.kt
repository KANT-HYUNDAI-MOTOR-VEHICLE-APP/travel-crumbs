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

/** 서버 `GET /place-types` 응답 한 줄 — 장소유형(place_type_mst · 카테고리 포함). */
@Serializable
data class PlaceTypeResponse(
    @SerialName("place_type_cd") val placeTypeCd: String,
    @SerialName("place_type_nm") val placeTypeNm: String,
    @SerialName("place_category_cd") val placeCategoryCd: String,
    @SerialName("place_category_nm") val placeCategoryNm: String,
)

/** 서버 `GET /tags` 응답 한 줄 — 경험 태그(tag_mst). */
@Serializable
data class TagResponse(
    @SerialName("tag_no") val tagNo: Long,
    @SerialName("tag_nm") val tagNm: String,
)

/** 서버 `GET /trips/{trip_uuid}/places` 응답 한 줄 — 기록한 장소(trip_place_dtl). */
@Serializable
data class TripPlaceItem(
    @SerialName("trip_place_no") val tripPlaceNo: Long,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("draft_yn") val draftYn: String,
    @SerialName("place_no") val placeNo: Long? = null,
    @SerialName("user_place_nm") val userPlaceNm: String? = null,
    @SerialName("place_type_cd") val placeTypeCd: String? = null,
    @SerialName("review_polarity_cd") val reviewPolarityCd: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("tag_nos") val tagNos: List<Long> = emptyList(),
)

/** 내비 후보에서 고른 장소 — 서버가 같은 POI 의 place_mst 에 연결. */
@Serializable
data class NaviPlaceDto(
    @SerialName("place_nm") val placeNm: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("poi_id") val poiId: String,
    @SerialName("poi_provider") val poiProvider: String,
)

/** 서버 `PUT /trips/{trip_uuid}/places/{trip_place_no}` 요청 — 장소 경험 입력(A-06) 「저장」. */
@Serializable
data class TripPlaceUpdateRequest(
    @SerialName("user_place_nm") val userPlaceNm: String,
    @SerialName("place_type_cd") val placeTypeCd: String? = null,
    @SerialName("review_polarity_cd") val reviewPolarityCd: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("tag_nos") val tagNos: List<Long> = emptyList(),
    @SerialName("navi_place") val naviPlace: NaviPlaceDto? = null,
)

/** 서버 `PUT /trips/{trip_uuid}` 요청 — 여행 상세(A-08) 「제목 수정」. */
@Serializable
data class TripUpdateRequest(
    @SerialName("trip_nm") val tripNm: String,
)

/** 여행 상세 타임라인 한 줄. */
@Serializable
data class TimelinePlaceDto(
    @SerialName("trip_place_no") val tripPlaceNo: Long,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("draft_yn") val draftYn: String,
    @SerialName("user_place_nm") val userPlaceNm: String? = null,
    @SerialName("place_type_nm") val placeTypeNm: String? = null,
    @SerialName("review_polarity_cd") val reviewPolarityCd: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("tag_nms") val tagNms: List<String> = emptyList(),
)

/** 서버 여행 상세 응답 — `POST /trips/{trip_uuid}/end` · `GET /trips/{trip_uuid}` · `PUT /trips/{trip_uuid}`. */
@Serializable
data class TripDetailResponse(
    @SerialName("trip_no") val tripNo: Long,
    @SerialName("trip_uuid") val tripUuid: String,
    @SerialName("trip_nm") val tripNm: String,
    @SerialName("trip_status_cd") val tripStatusCd: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("dist") val dist: Int? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("place_cnt") val placeCnt: Int = 0,
    @SerialName("route_data") val routeData: String? = null,
    @SerialName("places") val places: List<TimelinePlaceDto> = emptyList(),
)

/** 서버 `GET /trips` 응답 한 줄 — 여행 목록(A-07 간단판). */
@Serializable
data class TripListItemDto(
    @SerialName("trip_uuid") val tripUuid: String,
    @SerialName("trip_nm") val tripNm: String,
    @SerialName("trip_status_cd") val tripStatusCd: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("dist") val dist: Int? = null,
    @SerialName("place_cnt") val placeCnt: Int = 0,
)

/** 서버 `GET /trips/{trip_uuid}/route` 응답 — 경로 그림(간소화 경로점 + 장소 좌표). */
@Serializable
data class TripRouteResponse(
    @SerialName("points") val points: List<RouteCoordDto> = emptyList(),
    @SerialName("places") val places: List<RoutePlaceDto> = emptyList(),
)

@Serializable
data class RouteCoordDto(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
)

@Serializable
data class RoutePlaceDto(
    @SerialName("trip_place_no") val tripPlaceNo: Long,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("draft_yn") val draftYn: String,
    @SerialName("user_place_nm") val userPlaceNm: String? = null,
)
