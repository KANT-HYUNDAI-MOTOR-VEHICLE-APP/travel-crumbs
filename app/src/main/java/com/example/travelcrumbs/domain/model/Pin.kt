package com.example.travelcrumbs.domain.model

/**
 * 핀 — 진행 중인 여행의 목적지 기록 한 건(스펙 navigation 4.1 · 결정 기록 U45).
 * 기기 안 원본 · 서버로 보내도 지우지 않음(U46). 계정 · 좌표 · 이름 · 주소 · 결과 칸 없음.
 */
data class Pin(
    /** 여행 안 순서(1부터 · 빈 번호 없음). */
    val sequence: Int,
    val placeId: String,
    /** 안내 시작 시각(벽시계 밀리초). */
    val startedAtMillis: Long,
    /** 그 시각의 시간대(UTC 와의 차이 초) — 서버로 보낼 때 시간대가 붙은 시각으로. */
    val utcOffsetSeconds: Int,
    val uploadState: PinUploadState,
)

/** 서버 보냄 상태(스펙 navigation 4.1) — 「대기」가 재시도 대기열. */
enum class PinUploadState {
    PENDING,
    SENT,
    REJECTED,
}
