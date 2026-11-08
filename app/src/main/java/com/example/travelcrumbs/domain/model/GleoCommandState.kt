package com.example.travelcrumbs.domain.model

/** 앱이 직접 관측한 명령 상태. 음성 원문·좌표·여행 식별자는 보관하지 않음. */
data class GleoCommandState(
    val noticeId: Long = 0L,
    val phase: GleoCommandPhase = GleoCommandPhase.IDLE,
)

enum class GleoCommandPhase {
    IDLE,
    PREPARING,
    PROCESSING,
    SAVED,
    INVALID_REQUEST,
    NOT_READY,
    CONSENT_REQUIRED,
    UNCONFIRMED,
}
