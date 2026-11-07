package com.example.travelcrumbs.domain.model

/** 여행 중 기록한 장소 1건(trip_place_dtl) — 임시(draft)면 이름 · 유형 · 후기가 비어 있음. */
data class TripPlace(
    val tripPlaceNo: Long,
    val lat: Double,
    val lng: Double,
    /** 기록일시 — 서버가 준 ISO-8601 문자열. */
    val recordedAt: String,
    val isDraft: Boolean,
    val userPlaceName: String? = null,
    val placeTypeCode: String? = null,
    val reviewPolarityCode: String? = null,
    val memo: String? = null,
    val tagNos: List<Long> = emptyList(),
)

/** 경험 태그 한 줄(tag_mst). */
data class Tag(
    val tagNo: Long,
    val name: String,
)

/** 장소 경험 입력(A-06) 「저장」에 보내는 값. */
data class PlaceInput(
    val userPlaceName: String,
    val placeTypeCode: String?,
    val reviewPolarityCode: String?,
    val memo: String?,
    val tagNos: List<Long>,
    /** 내비 후보에서 골랐으면 그 장소 — POI 가 있어야 공식 장소로 연결. */
    val naviPlace: NaviPlace?,
)

/** 장소유형 한 줄(place_type_mst) — 개별 코드 테이블(결정 로그 D25). */
data class PlaceType(
    val code: String,
    val name: String,
    val categoryCode: String,
    val categoryName: String,
)
