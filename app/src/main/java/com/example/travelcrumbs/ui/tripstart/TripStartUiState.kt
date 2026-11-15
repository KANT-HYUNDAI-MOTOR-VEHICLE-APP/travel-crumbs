package com.example.travelcrumbs.ui.tripstart

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.ui.components.UiMessage

/** A-01 여행 시작이 그릴 것(와이어프레임 v6 · #51 U3). 좌표 없음. */
data class TripStartUiState(
    /** 입력 예시로 보여 줄 오늘 날짜의 월 · 일. */
    val defaultMonth: Int = 1,
    val defaultDay: Int = 1,
    /** 이름 칸의 입력 중 값 — 필수 입력. */
    val tripNm: String = "",
    val tripNameError: Boolean = false,
    val canStart: Boolean = false,
    val companions: Set<CompanionType> = emptySet(),
    /** 목적지 후보(앱 장소 목록 · places.json). */
    val destinations: List<DestinationItem> = emptyList(),
    /** 고른 목적지 id — null 이면 「목적지 없음」. */
    val destinationId: String? = null,
    /** 위치정보 수집 동의를 아직 안 받음(처음 한 번) — 동의 카드 · 「동의하고 기록 시작」. */
    val consentNeeded: Boolean = true,
    /** 시작 행동 뒤 위치정보 이용 동의를 별도 단계로 표시. */
    val consentStepVisible: Boolean = false,
    /** 「기록 시작」을 누른 뒤 — 버튼을 한 번만 받음. */
    val starting: Boolean = false,
    /** 시작 끝 — 화면이 받아 기록 화면으로 돌아감([message] 가 있으면 그 한 줄과 함께). */
    val done: Boolean = false,
    val message: UiMessage? = null,
)

data class DestinationItem(
    val id: String,
    val name: String,
    val address: String,
)
