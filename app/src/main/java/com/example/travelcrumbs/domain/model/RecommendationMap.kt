package com.example.travelcrumbs.domain.model

/*
 * 추천 지도(#59 · 와이어프레임 v6 B-01) 모델.
 * 좌표 규칙(스펙 3-1 · #55 와 같음): 좌표를 가진 모델([MapPlace] · [KoreaMapAsset])은 저장소 → 지도 그리는 쪽으로만 감 —
 * ViewModel 상태 · SavedStateHandle · 로그에 넣지 않음. ViewModel 로는 장소 번호 · 개수 · 결과 종류만([MapPinResult] · 열거형).
 */

/** 지도 핀 하나 = 장소 하나(place_no) · 좌표 · 순위용 리뷰 수(그 장소를 쓰는 공개 로드맵 별점 참여 수 합 · N12). */
data class MapPlace(
    val placeNo: Long,
    val coord: MapCoord,
    val reviewCnt: Int,
)

/** 앱 자산의 한국 전국 지도 — 스타일 주소(앱 자산) · 전국 범위 · 밀 수 있는 범위 · 시도 범위(시도 탭 이동). */
data class KoreaMapAsset(
    val styleUri: String,
    val countryBounds: MapBounds,
    val cameraBounds: MapBounds,
    val sidoBounds: Map<String, MapBounds>,
)

/** 화면 위 장소 하나 — 지도 그리는 쪽이 투영한 화면 픽셀 위치(좌표 아님 · 지도 그리는 쪽 안에서만). */
data class ScreenPlace(
    val placeNo: Long,
    val x: Float,
    val y: Float,
    val reviewCnt: Int,
)

/** 화면에 그릴 표시 하나 — 장소 번호 하나면 낱개 핀 · 둘 이상이면 숫자 묶음 핀. [x] · [y] 는 첫(리뷰 가장 많은) 장소 위치. */
data class MapMarker(
    val placeNos: List<Long>,
    val x: Float,
    val y: Float,
) {
    val isCluster: Boolean get() = placeNos.size > 1
}

/** 지도 그리는 쪽이 ViewModel 로 넘기는 화면 범위 결과 — 장소 번호 · 개수만(좌표 · 화면 위치 없음). */
data class MapPinResult(
    val singlePlaceNos: List<Long>,
    val clusters: List<List<Long>>,
    /** 화면 범위 안 공개 장소 수(표시 한도로 빠진 것 포함) — 0 이면 「범위 장소 0」. */
    val inRangeCnt: Int,
)

/** 첫 맞춤 기준 종류(스펙 3-1) — 종류만 · 좌표 없음. */
enum class MapFitBasis {
    /** 내 위치 반경 10km(#55 문구 반영 뒤 · 지금은 안 씀). */
    MY_LOCATION,

    /** 고른 시도가 다 들어오게. */
    SIDO,

    /** 대한민국 전체(N11 ① · 시도를 아직 안 고름). */
    COUNTRY,
}

/** 지도 상태(스펙 2-2 · 3-1). */
enum class MapStatus {
    PREPARING,
    LOCATING,
    READY,
    NO_LOCATION,
    OUTSIDE_KOREA,
    EMPTY_RANGE,
    FAILED,
}

/** 위치 결과 종류(스펙 3-1) — 종류만 · 좌표 · 거리 없음. */
enum class LocationResultKind {
    NONE,
    LOCATING,
    DOMESTIC,
    ABROAD,
    FAILED,
}

/** `LOCATION_RECO`(#55) 동의 — 최신 이력 기준. #55 가 이 브랜치에 들어오기 전엔 늘 [NOT_AGREED](위치 조회 0). */
enum class LocationConsent {
    AGREED,
    NOT_AGREED,
}
