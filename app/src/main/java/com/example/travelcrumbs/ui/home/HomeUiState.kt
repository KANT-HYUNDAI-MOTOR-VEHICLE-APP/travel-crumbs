package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.components.UiMessage

/** 홈이 그릴 것 — 맨 위 영역(기록 상태 · 경과 · 안내) + debug SDK 확인 카드 값. 불변. */
data class HomeUiState(
    val recording: RecordingStatus = RecordingStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val message: UiMessage? = null,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val naviReady: Boolean = false,
    val navi: SdkStatus<NaviRouteState> = SdkStatus.Idle,
    val openedByGleoAction: Boolean = false,
)
