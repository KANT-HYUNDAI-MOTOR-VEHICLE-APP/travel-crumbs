package com.example.travelcrumbs.ui.shell

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.ui.navigation.AppDestination

/** 앱 틀이 그릴 것(스펙 app-shell-v5 3-2). 레일은 판정과 무관하게 늘 보이고 내용 영역만 바뀜(D29). */
data class AppShellUiState(
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    /** 정차 화면에서 고른 목적지 — 주행 직전 목적지 · 주행 중에도 기억. */
    val selectedDestination: AppDestination = AppDestination.START,
    /** 주행 · 모름 중 고른 레일 칸 — 주행이 시작될 때마다 「기록」. */
    val drivingDestination: AppDestination = AppDestination.DRIVING_START,
    /** 작성 중 내용 있음 → 정차일 때 「이어서 쓰기」 띠(D27). */
    val unfinishedInputAvailable: Boolean = false,
    /** 레일 「추천」 · 「추천 보러 가기」(정차)를 누를 때마다 오르는 값 — 추천 칸이 바뀐 걸 보고 B-01 부터(#37). */
    val recommendationsEntry: Int = 0,
    /** 레일 「내 여행」 · 여행 카드를 누를 때마다 오르는 값 — 내 여행 칸이 바뀐 걸 보고 처음부터(#51 U6). */
    val tripsEntry: Int = 0,
    /** 내 여행 칸이 바로 열 여행(A-08) — null 이면 보관함(A-07). */
    val tripsOpenTripNo: Long? = null,
    /** 추천 칸이 바로 열 로드맵(B-04 · 기록 칸 근처 로드맵 카드 · #55) — null 이면 B-01 부터. [recommendationsOpenSidoNm] = 그 로드맵 시도. */
    val recommendationsOpenTripNo: Long? = null,
    val recommendationsOpenSidoNm: String? = null,
    /** 추천 칸이 진입하자마자 고를 지역(Gleo 「경주 근처 추천」 · #69) — null 이면 B-01 부터. */
    val recommendationsRegion: Region? = null,
) {
    /** 판정이 정차가 아니면(주행 · 모름) 주행 모드 — 「기록」 칸은 주행 전용 화면 · 그 밖은 주행 제한 안내. */
    val isDrivingMode: Boolean get() = drivingState != DrivingState.PARKED

    /** 레일 강조 칸 — 주행 · 모름: 주행 중 고른 칸 · 정차: 고른 목적지(숨은 목적지 없음 · D35). */
    val highlightedRailItem: AppDestination
        get() = if (isDrivingMode) drivingDestination else selectedDestination
}
