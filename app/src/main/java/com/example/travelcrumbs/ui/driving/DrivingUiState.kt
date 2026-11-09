package com.example.travelcrumbs.ui.driving

import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.components.UiMessage

/** 주행 전용 화면이 그릴 것(스펙 app-shell-v5 3-3). 불변. */
data class DrivingUiState(
    /** 없으면 화면이 「새 여행」. */
    val tripName: String? = null,
    val tripStatus: TripStatus = TripStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val placeCount: Int = 0,
    /** 여행 종료 확인 팝업 표시. */
    val endConfirmVisible: Boolean = false,
    val message: UiMessage? = null,
) {
    /** 「이 장소 기록」 · 「여행 종료」 — 진행 중 여행(RECORDING · PAUSED)일 때만(D26 · N23 · N24). 「음성」은 늘. */
    val recordingActionsEnabled: Boolean get() = tripStatus.inProgress

    /** 버튼 3 — RECORDING 「기록 일시정지」 · PAUSED 「기록 재개」 · 그 밖 「기록 일시정지」 비활성(D31). */
    val pauseButtonShowsResume: Boolean get() = tripStatus == TripStatus.PAUSED
    val pauseButtonEnabled: Boolean get() = tripStatus.inProgress
}
