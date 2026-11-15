package com.example.travelcrumbs.ui.roadmap

import com.example.travelcrumbs.ui.components.PlaceTagSheetItem
import com.example.travelcrumbs.ui.components.RoadmapPlaceItem
import com.example.travelcrumbs.ui.components.SketchPinItem
import com.example.travelcrumbs.ui.components.UiMessage

/** 로드맵 검토 안의 단계(와이어프레임 v6) — A-05 검토 → A-06 저장 · 공개. */
enum class RoadmapStep {
    REVIEW,
    PUBLISH,
}

/** A-05 · A-06 이 그릴 것(#51 U5) — 좌표 · 주소 · 시각 없음(공개 미리보기에도). */
data class RoadmapReviewUiState(
    val step: RoadmapStep = RoadmapStep.REVIEW,
    val tripNo: Long? = null,
    val tripNm: String? = null,
    val durationMinutes: Long = 0L,
    val places: List<RoadmapPlaceItem> = emptyList(),
    val sketch: List<SketchPinItem> = emptyList(),
    /** A-06 — 공개(닉네임 표시)를 골랐나. */
    val publicChosen: Boolean = false,
    /** 추천 공개 동의를 아직 안 받음 — 공개를 고르면 동의 카드 · 「동의하고 저장」. */
    val recoConsentNeeded: Boolean = true,
    val nicknameNm: String = "",
    /** 공개 미리보기 — 지역(시도 · 시군구 · 모르면 null) · 대표 태그. */
    val regionSidoNm: String? = null,
    val regionSigunguNm: String? = null,
    val representativeTagNm: String? = null,
    val tagSheet: PlaceTagSheetItem? = null,
    val message: UiMessage? = null,
    /** 저장 끝 — 화면이 받아 내 여행 보관함(A-07)으로. */
    val done: Boolean = false,
) {
    val placeCnt: Int get() = places.size
}
