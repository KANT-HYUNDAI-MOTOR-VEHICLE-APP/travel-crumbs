package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus

/** 화면이 그릴 것만 — 불변. */
data class HomeUiState(
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val naviReady: Boolean = false,
    val navi: SdkStatus<NaviRouteState> = SdkStatus.Idle,
    val openedByGleoAction: Boolean = false,
)
