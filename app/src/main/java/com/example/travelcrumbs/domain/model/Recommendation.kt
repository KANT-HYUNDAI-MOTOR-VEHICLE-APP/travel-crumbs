package com.example.travelcrumbs.domain.model

/*
 * 추천(#37 · 와이어프레임 v6 B-01 ~ B-05) 도메인 모델 — 서버 API 로 바꿔도 이 모양 그대로 받게 앱 안 값만 둠.
 * 이름은 데이터 표준 v0.4 단어(trip · place · tag · nickname_nm · place_ord · companion_type_cd · lat · lng)를 따름.
 * 표준에 아직 없는 것(지역 시도 · 시군구 · 별점 평균 · 별점 참여 수 · 공개 일시)은 제안 상태 — 서버 표준 등록 때 이름을 맞춤.
 */

/** 동행자 유형 — 표준 COMPANION_TYPE 코드값 그대로. */
enum class CompanionType {
    SOLO,
    SPOUSE,
    CHILD,
    PARENT,
    PET,
}

/** 경험 태그(tag_mst) — 태그명은 서버가 주는 값(해요체). */
data class Tag(
    val tagNo: Long,
    val tagNm: String,
)

/** 지역 — 시군구가 null 이면 그 시도 전체. */
data class Region(
    val sidoNm: String,
    val sigunguNm: String?,
)

/** 시도 하나와 그 아래 시군구 목록(지역 마스터 · 제안). */
data class SidoRegion(
    val sidoNm: String,
    val sigunguNms: List<String>,
)

/**
 * 공개 로드맵의 장소 하나(trip_place_dtl + place_mst).
 * 좌표는 안내 요청(내비)과 경로 그림의 상대 위치 계산에만 씀 — 화면 상태 · 로그에 넣지 않음.
 */
data class RecommendedPlace(
    val placeNo: Long,
    val placeNm: String,
    val placeOrd: Int,
    val addr: String,
    val lat: Double,
    val lng: Double,
    val tags: List<Tag>,
)

/** 공개 로드맵 하나(trip_mst · 닉네임 공개). */
data class RecommendedTrip(
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    val region: Region,
    val companionTypes: Set<CompanionType>,
    val ratingAvg: Double,
    val ratingCnt: Int,
    /** 공개 일시(epoch 밀리초) — 최신순 정렬 기준. */
    val publishedAt: Long,
    val places: List<RecommendedPlace>,
) {
    val placeCnt: Int get() = places.size
}

/** B-02 정렬. */
enum class RankingSort {
    RATING,
    LATEST,
}

/** 태그 하나와 지역 로드맵의 장소에 붙은 수(B-03). */
data class TagCount(
    val tag: Tag,
    val cnt: Int,
)

/** 경로 그림의 점 하나 — 0 ~ 1 상대 위치(x 오른쪽 · y 아래쪽). 실제 좌표는 화면으로 가지 않음. */
data class SketchPoint(
    val x: Float,
    val y: Float,
)
