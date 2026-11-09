package com.example.travelcrumbs.ui.settings

import com.example.travelcrumbs.ui.components.UiMessage

/** C-03 설정이 그릴 것(와이어프레임 v6 · #51 U2) — 닉네임 · 동의 유형별 최신 상태 · 닉네임 수정 팝업 · 한 줄 안내. */
data class SettingsUiState(
    val nicknameNm: String = "",
    /** 위치정보 수집 — 동의 중이면 동의한 날 · 아니면 null. */
    val locationAgreedOn: ConsentDate? = null,
    /** 추천 공개 동의 중인가. */
    val recoAgreed: Boolean = false,
    /** 닉네임 수정 팝업의 입력 중 값 — null 이면 닫힘. */
    val nicknameDraft: String? = null,
    val message: UiMessage? = null,
)

data class ConsentDate(val year: Int, val month: Int, val day: Int)
