package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.components.UiMessage

/** 홈이 그릴 것 — 맨 위 영역(여행 상태 · 경과 · 안내) + debug SDK 확인 카드 값(스펙 app-shell-v5 3-4). 불변. */
data class HomeUiState(
    val tripStatus: TripStatus = TripStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val message: UiMessage? = null,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val naviReady: Boolean = false,
    val navi: SdkStatus<NaviRouteState> = SdkStatus.Idle,
    val openedByGleoAction: Boolean = false,
) {
    /** 「이 장소 기록」 — 기록 중일 때만(D26). */
    val recordPlaceEnabled: Boolean get() = tripStatus == TripStatus.RECORDING

    /** 임시 「새 여행 시작」 — 기록 중이 아닐 때만(D26 · N19 — 기록 중엔 자리만 지키고 비활성). */
    val startNewTripEnabled: Boolean get() = tripStatus != TripStatus.RECORDING
}
