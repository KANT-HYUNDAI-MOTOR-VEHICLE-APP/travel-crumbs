package com.example.travelcrumbs.ui.tripdetail

import com.example.travelcrumbs.domain.model.TripDetail
import com.example.travelcrumbs.domain.model.TripRoute
import com.example.travelcrumbs.ui.components.UiMessage

/** 여행 상세(A-08)가 그릴 것. 불변. */
data class TripDetailUiState(
    val loading: Boolean = true,
    val detail: TripDetail? = null,
    /** 경로 그림(지시서 17장) — 못 불러오면 null(그림 자리에 안내). */
    val route: TripRoute? = null,
    val loadFailed: Boolean = false,
    /** 「제목 수정」 입력칸이 열렸는지. */
    val editingTitle: Boolean = false,
    val titleInput: String = "",
    /** 추천(Gleo 자리)을 기다리거나 저장 중. */
    val busy: Boolean = false,
    val message: UiMessage? = null,
)
