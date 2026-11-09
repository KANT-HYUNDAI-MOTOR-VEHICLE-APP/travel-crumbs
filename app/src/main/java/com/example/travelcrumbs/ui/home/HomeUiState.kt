package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.components.UiMessage

/**
 * 기록 칸(정차)이 그릴 것 — 맨 위 영역(여행 상태 · 경과 · 장소 수 · 안내) + debug SDK 확인 카드 값(스펙 app-shell-v5 3-4 · D34). 불변.
 * 최근 여행 1개 자리는 이번엔 늘 빈 상태(N26 — 여행 목록은 LLM반 CRUD)라 칸을 두지 않음 · 화면이 빈 상태 한 줄만 그림.
 */
data class HomeUiState(
    val tripStatus: TripStatus = TripStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val placeCount: Int = 0,
    val message: UiMessage? = null,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val naviReady: Boolean = false,
    val navi: SdkStatus<NaviRouteState> = SdkStatus.Idle,
    val openedByGleoAction: Boolean = false,
) {
    /**
     * 맨 위 영역 모양 — 진행 중(RECORDING · PAUSED): 기록 상태 · 경과 · 장소 수 · 「이 장소 기록」 /
     * 아님: 「여행 시작」 · 최근 여행 자리 · 「추천 보러 가기」(D34 · 다른 모양의 요소는 그리지 않음).
     */
    val tripInProgress: Boolean get() = tripStatus.inProgress
}
