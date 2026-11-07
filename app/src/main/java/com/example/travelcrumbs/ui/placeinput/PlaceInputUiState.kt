package com.example.travelcrumbs.ui.placeinput

import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.ui.components.UiMessage

/** 장소 경험 입력(A-06)이 그릴 것. 불변. */
data class PlaceInputUiState(
    val loading: Boolean = true,
    /** 지금 입력하는 임시 기록 — 없으면 입력할 것이 없음. */
    val draft: TripPlace? = null,
    /** 남은 임시 기록 수(지금 것 포함). */
    val draftCount: Int = 0,
    /** 기록한 위치 가까이 있는 내비 장소(가까운 순). */
    val candidates: List<NaviPlace> = emptyList(),
    val selectedCandidate: NaviPlace? = null,
    /** 사용자장소명(user_place_nm). */
    val placeName: String = "",
    val categories: List<CodeItem> = emptyList(),
    val placeTypes: List<CodeItem> = emptyList(),
    val selectedCategory: String? = null,
    val selectedType: String? = null,
    val polarities: List<CodeItem> = emptyList(),
    val selectedPolarity: String? = null,
    val tags: List<Tag> = emptyList(),
    val selectedTags: Set<Long> = emptySet(),
    val memo: String = "",
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    /** 「나중에 입력」 확인 창. */
    val showLeaveConfirm: Boolean = false,
    val message: UiMessage? = null,
    /** 저장 · 나중에 입력이 끝나 화면을 닫을 때. */
    val closed: Boolean = false,
) {
    /** 고른 카테고리의 유형만. */
    val visibleTypes: List<CodeItem>
        get() = placeTypes.filter { it.categoryCode == selectedCategory }

    val canSave: Boolean
        get() = draft != null && !saving && placeName.isNotBlank()
}
