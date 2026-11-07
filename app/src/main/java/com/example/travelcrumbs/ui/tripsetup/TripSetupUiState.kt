package com.example.travelcrumbs.ui.tripsetup

import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.ui.components.UiMessage

/** 여행 시작 설정(A-04)이 그릴 것. 불변. */
data class TripSetupUiState(
    /** 동행자 코드(COMPANION_TYPE)를 부르는 중. */
    val loading: Boolean = true,
    /** 여행명(trip_nm) — 날짜 기본값, 사용자가 고칠 수 있음. */
    val tripName: String = "",
    /** 「수정」을 눌러 입력칸이 열렸는지. */
    val editingName: Boolean = false,
    /** 「수정」을 눌러 추천(Gleo AI 자리)을 기다리는 중. */
    val suggesting: Boolean = false,
    /** 동행자 버튼 — cd_dtl COMPANION_TYPE 순서대로. */
    val companions: List<CodeItem> = emptyList(),
    val selectedCompanions: Set<String> = emptySet(),
    /** 동행자 코드를 못 불러옴(서버 꺼짐 등) — 「다시 시도」. */
    val loadFailed: Boolean = false,
    /** 「여행 시작」 저장 중. */
    val starting: Boolean = false,
    val message: UiMessage? = null,
    /** 저장이 끝난 여행명 — 화면이 이 값을 보면 홈으로 돌아감. */
    val startedTripName: String? = null,
) {
    val canStart: Boolean
        get() = !loading && !starting && !suggesting && tripName.isNotBlank() && selectedCompanions.isNotEmpty()
}
