package com.example.travelcrumbs.ui.settings

import com.example.travelcrumbs.ui.components.UiMessage

/** C-03 설정이 그릴 것(와이어프레임 v6 · #51 U2) — 닉네임 · 동의 유형별 최신 상태 · 닉네임 수정 팝업 · 한 줄 안내. */
data class SettingsUiState(
    val nicknameNm: String = "",
    /** 위치정보 수집 — 동의 중이면 동의한 날 · 아니면 null. */
    val locationAgreedOn: ConsentDate? = null,
    /** 근처 로드맵 위치 이용(LOCATION_RECO · #55) — 동의 중이면 동의한 날 · 아니면(동의 안 함 · 이력 없음) null. */
    val nearbyLocationAgreedOn: ConsentDate? = null,
    /** 근처 로드맵 위치 이용 동의 팝업(2-9 「동의」) — 재생성 뒤에도 유지. */
    val nearbyConsentAsk: Boolean = false,
    /** 추천 공개 동의 중인가. */
    val recoAgreed: Boolean = false,
    /** 닉네임 수정 팝업의 입력 중 값 — null 이면 닫힘. */
    val nicknameDraft: String? = null,
    val nicknameError: Boolean = false,
    /** 「내 기록 모두 지우기」 확인 팝업. */
    val clearAllAsk: Boolean = false,
    val message: UiMessage? = null,
)

data class ConsentDate(val year: Int, val month: Int, val day: Int)
