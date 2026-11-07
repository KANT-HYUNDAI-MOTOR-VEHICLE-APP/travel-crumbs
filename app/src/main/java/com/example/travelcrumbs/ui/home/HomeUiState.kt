package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripRoute
import com.example.travelcrumbs.domain.model.TripSummary
import com.example.travelcrumbs.ui.components.UiMessage

/** 홈이 그릴 것 — 맨 위 영역(기록 상태 · 경과 · 안내) + debug SDK 확인 카드 값. 불변. */
data class HomeUiState(
    /** 로그인된 사용자 닉네임(user_mst.nickname_nm) — 아직 못 불렀으면 null. */
    val nickname: String? = null,
    /** 사용자 정보를 불러오지 못함(서버 꺼짐 등). */
    val userUnavailable: Boolean = false,
    /** 위치정보 동의 창(A-03-1) 표시 여부. */
    val showLocationConsent: Boolean = false,
    /** 기록 중인 여행명(trip_mst.trip_nm) — 없으면 null. */
    val activeTripName: String? = null,
    /** 기록 중인 여행의 장소 수(trip_mst.place_cnt) — A-05 「기록된 장소 N」. */
    val placeCount: Int = 0,
    /** 입력 안 한 임시 기록 수(trip_place_dtl draft_yn = Y) — A-06 으로 들어가는 버튼. */
    val draftCount: Int = 0,
    /** 「여행 종료」 확인 창(A-08). */
    val showEndConfirm: Boolean = false,
    /** 종료한 여행의 상세(A-08)를 홈 자리에 띄웠으면 그 여행 UUID. */
    val tripDetailUuid: String? = null,
    /** 장소 경험 입력(A-06)을 홈 자리에 띄웠는지. */
    val showPlaceInput: Boolean = false,
    /** 여행 시작 설정(A-04)을 홈 자리에 띄웠는지. */
    val showTripSetup: Boolean = false,
    /** 경로 그림(지도 자리 · 지시서 17장) — 기록 중인 여행 또는 최근 여행. 없으면 null. */
    val route: TripRoute? = null,
    /** 경로 그림이 기록 중인 여행인지(아니면 최근 여행). */
    val routeIsActive: Boolean = false,
    /** 경로 그림이 최근 여행이면 그 여행명. */
    val routeTripName: String? = null,
    /** 최근 여행 카드(완료된 여행 · 최신순). */
    val recentTrips: List<TripSummary> = emptyList(),
    val recording: RecordingStatus = RecordingStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val message: UiMessage? = null,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val naviReady: Boolean = false,
    val navi: SdkStatus<NaviRouteState> = SdkStatus.Idle,
    val openedByGleoAction: Boolean = false,
)
