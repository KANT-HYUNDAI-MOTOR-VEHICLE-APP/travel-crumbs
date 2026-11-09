package com.example.travelcrumbs.domain.model

/*
 * 내 여행 · 장소 · 사용자 · 동의(#51 · 와이어프레임 v6 A-01 ~ A-08 · C-02 · C-03) 도메인 모델 — 저장 자리(기기 안 Room · 뒤에 서버)와 무관한 앱 안 값.
 * 이름은 데이터 표준(trip_mst · trip_place_dtl · user_mst · user_consent_hist · tag_mst)의 단어를 따름.
 * 좌표(lat · lng)는 경로 그림 · 지역 계산에만 씀 — 화면 상태 · 로그에 넣지 않음.
 */

/** 공개 범위 — 표준 VISIBILITY 「결정」 값 중 이 앱이 쓰는 둘. 「공개(닉네임 표시)」는 ANONYMOUS_PUBLIC 으로 저장(보고서 남은 결정). */
enum class Visibility {
    PRIVATE,
    ANONYMOUS_PUBLIC,
}

/** 동의 유형 — 표준 CONSENT_TYPE 코드값 그대로. */
enum class ConsentType {
    LOCATION_COLLECT,
    RECO_PUBLIC,
}

/** 동의 유형 하나의 최신 상태 — [agreed] = 동의(Y) · 철회(N) · [at] = 그 행의 시각. */
data class ConsentState(
    val type: ConsentType,
    val agreed: Boolean,
    val at: Long,
)

/** 지금 사용자(user_mst) — 로그인 화면 전엔 기기 안 기본 사용자 1명. */
data class UserProfile(
    val userNo: Long,
    val nicknameNm: String,
)

/** 여행 장소 하나(trip_place_dtl + 태그). [draft] = 임시저장(위치만 · 태그는 나중에). */
data class TripPlace(
    val tripPlaceNo: Long,
    val tripNo: Long,
    val placeOrd: Int,
    val userPlaceNm: String?,
    val memo: String?,
    val draft: Boolean,
    val lat: Double?,
    val lng: Double?,
    val addr: String?,
    val recordedAt: Long,
    val tagNos: List<Long>,
)

/** 내 여행 하나(trip_mst + 동행자 + 장소). */
data class TripRecord(
    val tripNo: Long,
    val tripNm: String?,
    val status: TripStatus,
    val visibility: Visibility,
    val startAt: Long,
    val endAt: Long?,
    /** 기록한 시간(밀리초 · 일시정지 제외) — 끝난 여행만. */
    val durationMillis: Long?,
    val destPlaceNm: String?,
    val companions: Set<CompanionType>,
    val places: List<TripPlace>,
) {
    val placeCnt: Int get() = places.size

    /** 태그 안 단 장소(임시저장) 수 — A-05 「미완성」. */
    val draftCnt: Int get() = places.count { it.draft }
}

/** A-01 「기록 시작」의 입력 — 여행명 · 동행자 · 목적지 이름(선택). */
data class NewTrip(
    val tripNm: String?,
    val companions: Set<CompanionType>,
    val destPlaceNm: String?,
)

/** 지금 위치 한 번(내비 getCurrentLocationInfo) — 기기 안에서 장소 기록에만 씀 · 로그 · 화면 상태에 넣지 않음. */
data class CurrentLocation(
    val lat: Double,
    val lng: Double,
    val addr: String?,
)
