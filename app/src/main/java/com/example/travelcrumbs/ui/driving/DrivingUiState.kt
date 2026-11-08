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
    /** 「이 장소 기록」 · 「여행 종료」 — 기록 중일 때만(D26). 「음성」은 늘. */
    val recordingActionsEnabled: Boolean get() = tripStatus == TripStatus.RECORDING
}
