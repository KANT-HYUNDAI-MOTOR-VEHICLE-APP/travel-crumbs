package com.example.travelcrumbs.ui.trips

import com.example.travelcrumbs.ui.components.TripSummaryItem
import com.example.travelcrumbs.ui.components.UiMessage

/** A-07 내 여행 보관함이 그릴 것(와이어프레임 v6 · #51 U6). */
data class TripsUiState(
    /** 끝난 여행 — 최근 순. */
    val trips: List<TripSummaryItem> = emptyList(),
    /** A-08 로 연 여행 — null 이면 목록. */
    val selectedTripNo: Long? = null,
    /** 공개로 바꾸려는데 추천 공개 동의가 없어 묻는 중인 여행 — null 이면 팝업 없음. */
    val consentAskTripNo: Long? = null,
    val message: UiMessage? = null,
)
