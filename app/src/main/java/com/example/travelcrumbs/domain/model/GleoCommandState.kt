package com.example.travelcrumbs.domain.model

/**
 * 앱이 직접 관측한 명령 상태. 음성 원문·좌표·여행 식별자는 보관하지 않음.
 * [detail] = 화면 한 줄에 끼울 짧은 값(장소 수 · 지역 이름) · [screen] = 정차 중 한 번 따를 화면 요청(#69).
 */
data class GleoCommandState(
    val noticeId: Long = 0L,
    val phase: GleoCommandPhase = GleoCommandPhase.IDLE,
    val detail: String? = null,
    val screen: GleoScreenRequest? = null,
)

enum class GleoCommandPhase(val inProgress: Boolean = false) {
    IDLE,
    PREPARING(inProgress = true),
    LOADING_TRIP(inProgress = true),
    PROCESSING(inProgress = true),
    SAVED,
    INVALID_REQUEST,
    NOT_READY,
    CONSENT_REQUIRED,
    FAILED,
    UNCONFIRMED,

    // CONTROL_CURRENT_TRIP(#69)
    TRIP_PAUSED,
    TRIP_RESUMED,
    ALREADY_PAUSED,
    ALREADY_RUNNING,
    END_CONFIRM,
    NO_TRIP,

    // SHOW_TRAVEL_INFO(#69)
    TODAY_RECORDS,
    RECOMMENDED,
    NO_RECOMMENDATION,

    /** 여행 제어 · 정보 조회가 실패하거나 기한 안에 끝나지 않음(장소 기록 실패 [FAILED] 와 구분). */
    REQUEST_FAILED,

    /** 다른 Gleo 명령을 처리하는 중이라 이번 명령은 받지 않음. */
    BUSY,
}

/** Gleo 명령 뒤 앱 틀이 따를 화면 요청 — 정차 중에만 따름(주행 중 화면 전환 없음 · 스펙 #69 2절). */
sealed interface GleoScreenRequest {
    /** 「내 여행」 칸 목록. */
    data object OpenTrips : GleoScreenRequest

    /** 「추천」 칸을 이 지역 순위(B-02)로. [region] 의 시군구가 null 이면 시도 전체 · [region] 이 null 이면 지역 고르기(B-01)부터. */
    data class OpenRecommendations(val region: Region?) : GleoScreenRequest

    /** 기록 칸 · 주행 화면의 기존 「여행 종료」 확인 팝업(음성만으로 끝내지 않음 · U2). */
    data object ConfirmEndTrip : GleoScreenRequest
}
