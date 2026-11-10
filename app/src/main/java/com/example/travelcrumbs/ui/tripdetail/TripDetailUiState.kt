package com.example.travelcrumbs.ui.tripdetail

import com.example.travelcrumbs.ui.components.PlaceTagSheetItem
import com.example.travelcrumbs.ui.components.RoadmapPlaceItem
import com.example.travelcrumbs.ui.components.SketchPinItem
import com.example.travelcrumbs.ui.components.TripSummaryItem
import com.example.travelcrumbs.ui.components.UiMessage

/** A-08 이 그릴 것(#51 U6) — 좌표 없음. */
data class TripDetailUiState(
    val summary: TripSummaryItem? = null,
    val durationMinutes: Long = 0L,
    val places: List<RoadmapPlaceItem> = emptyList(),
    val sketch: List<SketchPinItem> = emptyList(),
    /** 받은 별점 평균 · 참여 수(공개 로드맵 · 기기 안 별점 기록). */
    val ratingAvg: Double = 0.0,
    val ratingCnt: Int = 0,
    /** 여행명 고치기 팝업 입력 중 값 — null 이면 닫힘. */
    val renameDraft: String? = null,
    val consentAsk: Boolean = false,
    /** 「여행 삭제」 확인 팝업. */
    val deleteAsk: Boolean = false,
    /** 삭제 끝(여기서 지움 또는 고른 여행이 없어짐) — 화면이 받아 보관함 목록으로. */
    val deleted: Boolean = false,
    val tagSheet: PlaceTagSheetItem? = null,
    val message: UiMessage? = null,
)
