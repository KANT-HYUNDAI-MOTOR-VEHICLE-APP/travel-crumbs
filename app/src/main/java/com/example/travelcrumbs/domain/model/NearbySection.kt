package com.example.travelcrumbs.domain.model

/**
 * 홈 「내 근처 리뷰 많은 로드맵」 구역의 논리 상태(#55 · 스펙 home-nearby 3-1) — 값 8개 · 화면 표현은 2-6 표와 1:1.
 * 숨김이 넷([INELIGIBLE] · [CONSENT_DECLINED] · [NO_ROADMAP] · [LOCATION_UNAVAILABLE])이지만 회복 전이가 달라 값을 나눔(3-2).
 */
enum class NearbySectionState {
    /** 주행 · 모름 · 진행 중 여행 · 화면이 뒤 · 다른 화면이 기록 칸 차지. */
    INELIGIBLE,

    /** 근처 위치 동의(LOCATION_RECO) 이력 없음 — 동의 카드(2-8). */
    CONSENT_NEEDED,

    /** 최신 이력 = 거부 · 철회. */
    CONSENT_DECLINED,

    /** 위치 확인 중 — 제목 · 「시연용 데이터」 · 카드 자리에 한 줄. */
    LOCATING,

    /** 반경 안 로드맵이 1장 이상. */
    NEARBY,

    /** 반경 안 0장 → 전국 리뷰 많은 순(N2). */
    NATIONWIDE,

    /** 위치는 얻음 · 대상 로드맵 0개. */
    NO_ROADMAP,

    /** 내비 연결 안 됨 · 조회 오류 · 응답 한도 지남. */
    LOCATION_UNAVAILABLE,
    ;

    /** 구역을 그리나(2-6). */
    val visible: Boolean get() = this == CONSENT_NEEDED || this == LOCATING || this == NEARBY || this == NATIONWIDE

    /** 카드 목록을 그리나. */
    val showsCards: Boolean get() = this == NEARBY || this == NATIONWIDE
}

/** 근처 위치 동의(LOCATION_RECO)의 유형별 최신 이력 — 이력 없음 · 동의함(Y) · 동의 안 함(N · 거부 · 철회). */
enum class NearbyConsent {
    NONE,
    AGREED,
    DECLINED,
    ;

    companion object {
        /** 최신 행의 동의 여부(null = 행 없음) → 값. */
        fun of(agreed: Boolean?): NearbyConsent = when (agreed) {
            null -> NONE
            true -> AGREED
            false -> DECLINED
        }
    }
}

/** 마지막 위치 조회 결과 — 좌표 없음. [Located] 는 로드맵별 첫 장소 거리(미터)만. */
sealed interface NearbyLookupResult {
    data object Unavailable : NearbyLookupResult

    data class Located(val trips: List<RecommendedTrip>, val distances: Map<Long, Int>) : NearbyLookupResult
}
