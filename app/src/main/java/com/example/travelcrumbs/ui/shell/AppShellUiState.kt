package com.example.travelcrumbs.ui.shell

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.navigation.AppDestination

/** 앱 틀이 그릴 것(스펙 app-shell 3-2). */
data class AppShellUiState(
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    /** 정차 화면에서 고른 목적지 — 주행 중에도 기억. */
    val selectedDestination: AppDestination = AppDestination.START,
    /** 임시 저장 있음 → 「이어서 쓰기」 띠. */
    val draftAvailable: Boolean = false,
) {
    /** 판정이 정차가 아니면(주행 · 모름) 주행 전용 화면. */
    val showDrivingScreen: Boolean get() = drivingState != DrivingState.PARKED
}
