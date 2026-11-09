package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRequestStatus
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.components.PlaceTagSheetItem
import com.example.travelcrumbs.ui.components.RatingDialogItem
import com.example.travelcrumbs.ui.components.TripSummaryItem
import com.example.travelcrumbs.ui.components.UiMessage

/**
 * 기록 칸(정차)이 그릴 것 — 맨 위 영역(여행 상태 · 경과 · 장소 수 · 안내) + 「가 볼 장소」 · 「장소 고르기」(스펙 navigation 3.1)
 * + debug SDK 확인 카드 값(스펙 app-shell-v5 3-4 · D34). 불변. 좌표는 넣지 않음(화면이 그리지 않으므로 · navigation 3.1).
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
    /** 「가 볼 장소」 — 고정 순서 · 기기 저장을 아직 못 읽었으면 null(읽기 전 · E4 · E6 둘 다 그리지 않음). */
    val pinnedPlaces: List<PlaceItem>? = null,
    /** 「장소 고르기」 — 후보 JSON 순서 · 고정 여부. */
    val candidates: List<CandidateItem> = emptyList(),
    /** 두 구역을 그리나 — 정차(PARKED)일 때만. */
    val placesVisible: Boolean = false,
    /** 고정 수 < 최대 — 거짓이면 한도 안내 · 「고정」 비활성. */
    val canPinMore: Boolean = true,
    /** 마지막 길안내 요청 상태. */
    val routeRequest: RouteRequestState = RouteRequestState.Idle,
    /** debug 진단 카드의 「안내 건수」 한 줄(U41). */
    val placeStats: SdkStatus<List<PlaceStatItem>> = SdkStatus.Idle,
    /**
     * 더미 경로 패널(E13)에 그릴 장소(이름 · 주소) — 없으면 null. 실패 상태에서 「더미 경로」를 눌렀을 때만 ·
     * 좌표 · 현재 위치 칸 없음(navigation 3.1 · U53).
     */
    val dummyRoute: PlaceItem? = null,
    /**
     * 길안내 연결 화면(2.3)에 그릴 장소(id · 이름 · 주소) — null 이면 닫힘(홈 목록). 열림이면 홈 목록(맨 위 영역 · 두 구역 · debug 카드)을
     * 그리지 않음. 좌표 · 현재 위치 칸 없음(navigation 3.1 · U53).
     */
    val guideScreen: PlaceItem? = null,
    /** 이 빌드에 「더미 경로」가 있나 — debug 만(U55). 버튼 표시는 아래 [dummyRouteVisible]. */
    val dummyRouteAvailable: Boolean = false,
    /** 다녀온 로드맵 별점 자동 팝업(#41 B-06) — 없으면 null. */
    val ratingPrompt: RatingDialogItem? = null,
    /** 여행이 하나라도 있나(v6 — 첫 실행엔 없음 · #51 U1). */
    val hasTrip: Boolean = true,
    /** 진행 중 여행 이름 — 없으면 「새 여행」. */
    val tripName: String? = null,
    /** C-02 인사(user_mst.nickname_nm · #51 U2). */
    val nicknameNm: String = "",
    /** C-02 최근 여행 카드 — 가장 최근 끝난 여행 1개 · 없으면 null(「최근 여행이 없어요」). */
    val recentTrip: TripSummaryItem? = null,
    /** A-04 여행 종료 확인 팝업(정차 「여행 종료」). */
    val endConfirmVisible: Boolean = false,
    /** A-01 여행 시작 화면이 열림(진행 중 여행이 없을 때만 · #51 U3). */
    val tripStartOpen: Boolean = false,
    /** A-03 장소 기록 · 태그 시트 — 정차일 때만 · 없으면 null(#51 U4). */
    val tagSheet: PlaceTagSheetItem? = null,
) {
    /**
     * 맨 위 영역 모양 — 진행 중(RECORDING · PAUSED): 기록 상태 · 경과 · 장소 수 · 「이 장소 기록」 /
     * 아님: 「여행 시작」 · 최근 여행 자리 · 「추천 보러 가기」(D34 · 다른 모양의 요소는 그리지 않음).
     */
    val tripInProgress: Boolean get() = tripStatus.inProgress

    /** 「여기로 안내」 누를 수 있음 = 내비 연결됨 · 요청 중 아님(navigation 3.1 — 따로 칸을 두지 않음). */
    val guideEnabled: Boolean get() = naviReady && routeRequest !is RouteRequestState.Requesting

    /** 「다시 시도」(G4) 보임 = 길안내 연결 화면 열림 · 실패(이유 무관 · U51 · U71) · 누를 수 있음 = 「여기로 안내」와 같은 식. */
    val retryVisible: Boolean get() = guideScreen != null && routeRequest is RouteRequestState.Failed
    val retryEnabled: Boolean get() = retryVisible && guideEnabled

    /** 「더미 경로」 보임 = 실패 · debug 빌드 · 누를 수 있음 = 패널이 닫혀 있음(내비 연결과 무관). */
    val dummyRouteVisible: Boolean get() = retryVisible && dummyRouteAvailable
    val dummyRouteEnabled: Boolean get() = dummyRouteVisible && dummyRoute == null
}

data class PlaceItem(
    val id: String,
    val name: String,
    val address: String,
)

data class CandidateItem(
    val id: String,
    val name: String,
    val address: String,
    val pinned: Boolean,
)

/**
 * 길안내 요청 상태(navigation 3.1 · 3.2) — 결과는 다음 요청을 시작할 때까지 남음(U7).
 * [status] = 표준 코드 `NAVI_REQ_STATUS` 와의 대응(navigation 4.1) · 대기는 코드 없음.
 */
sealed interface RouteRequestState {
    val status: NaviRequestStatus?

    data object Idle : RouteRequestState {
        override val status: NaviRequestStatus? get() = null
    }

    data class Requesting(val placeId: String) : RouteRequestState {
        override val status: NaviRequestStatus get() = NaviRequestStatus.REQUESTING
    }

    /** 요청할 때의 장소 이름으로(요청 중 고정 해제해도 · T42). */
    data class Started(val placeName: String) : RouteRequestState {
        override val status: NaviRequestStatus get() = NaviRequestStatus.SUCCESS
    }

    /** [placeId] = 실패한 그 요청의 장소 — 「다시 시도」 · 「더미 경로」의 대상(그 뒤 고정 해제해도 그대로 · 3.1). */
    data class Failed(val reason: RouteFailReason, val placeId: String) : RouteRequestState {
        override val status: NaviRequestStatus get() = NaviRequestStatus.FAILED
    }
}

/** debug 카드 한 줄 — 장소 이름(후보에 없으면 id) · 안내 시작 건수. */
data class PlaceStatItem(
    val label: String,
    val startCount: Int,
)
