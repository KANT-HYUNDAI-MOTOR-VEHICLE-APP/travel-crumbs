package com.example.travelcrumbs.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.place.PinnedPlacesRepository
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.place.PlaceStatsRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuideRequestState
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 홈(스펙 app-shell 2-2 · navigation 2.1 · 2.2) — 기록 상태는 주행 화면과 같은 출처(RecordingRepository · D1).
 * 「여기로 안내」 · 「고정」 · 「고정 해제」 · 실패 폴백(「다시 시도」 · 「더미 경로」 · 닫기)은 정차일 때만
 * (navigation 3.1 · U35 · U18 · U51 ~ U56) — 결과는 상태 칸으로.
 *
 * 길안내 요청 · 대기 · TIMEOUT 대조 · 늦은 시작 판정 · 핀 남기기는 화면과 무관한 입구([GuideRequestTracker] · 앱 수명)가 함 —
 * 이 ViewModel 은 입구 상태를 화면 상태로 바꾸고, 정차일 때만 입구를 부름.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    recordingRepository: RecordingRepository,
    private val recordPlace: RecordPlaceUseCase,
    drivingSignalRepository: DrivingSignalRepository,
    gleoActionRepository: GleoActionRepository,
    private val naviRepository: NaviRepository,
    private val placeCatalogRepository: PlaceCatalogRepository,
    private val pinnedPlacesRepository: PinnedPlacesRepository,
    private val placeStatsRepository: PlaceStatsRepository,
    private val guideRequests: GuideRequestTracker,
    private val demoFeatures: DemoFeatures,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)
    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)
    private val placeStats = MutableStateFlow<SdkStatus<List<PlaceStatItem>>>(SdkStatus.Idle)

    /**
     * 더미 경로 패널(E13)을 연 실패의 시도 번호 — 패널은 이 번호와 입구의 지금 실패가 같을 때만 보임(파생).
     * 새 요청 · 늦은 시작 판정 · 다른 장소의 실패면 입구 상태가 바뀌어 저절로 사라짐 — 쓰기 순서와 무관(R4-1).
     * 화면 상태뿐 · 기기 저장 · SavedStateHandle 복원 대상 아님(navigation 3.1 · T108).
     */
    private val dummyRouteAttempt = MutableStateFlow<Long?>(null)

    /**
     * 길안내 연결 화면(2.3)을 연 때의 입구 변경 번호 — 화면은 입구 상태의 [GuideRequestState.seq] 가 이 번호 이상일 때만 보임(파생).
     * 「여기로 안내」가 입구를 부르기 **전에** 다음 번호로 써 둠 → 입구 상태가 바뀌기 전엔 화면이 닫힌 채, 바뀐 뒤엔 열림 —
     * 「요청 중인데 화면 안 열림」 같은 사이값이 생기지 않음. 화면 상태뿐 · 복원 대상 아님(T129).
     */
    private val guideScreenFrom = MutableStateFlow<Long?>(null)

    /** 화면이 고른 두 보기의 열림 번호 — combine 인자 수를 줄이려고 하나로 묶음. */
    private val openMarks = combine(guideScreenFrom, dummyRouteAttempt) { from, dummy ->
        OpenMarks(guideScreenFrom = from, dummyRouteAttempt = dummy)
    }

    /**
     * [guideScreenFrom] = 길안내 연결 화면을 연 입구 변경 번호(null = 닫힘) ·
     * [dummyRouteAttempt] = 더미 경로를 켠 실패의 시도 번호(null = 꺼짐).
     */
    private data class OpenMarks(val guideScreenFrom: Long?, val dummyRouteAttempt: Long?)

    /** 후보 — 읽기 전 null. 조작 메서드가 바로 찾을 수 있게 값으로 들고 있음. */
    private val candidates = MutableStateFlow<List<Place>?>(null)

    private val sdkCheck = combine(
        drivingSignalRepository.drivingState,
        naviRepository.ready,
        naviState,
        gleoActionRepository.openedByGleoAction,
    ) { driving, naviReady, navi, openedByGleo ->
        HomeUiState(
            drivingState = driving,
            naviReady = naviReady,
            navi = navi,
            openedByGleoAction = openedByGleo,
        )
    }

    private val places = combine(
        candidates,
        pinnedPlacesRepository.pinnedIds.map<List<String>, List<String>?> { it }.onStart { emit(null) },
        guideRequests.state,
        placeStats,
        openMarks,
    ) { all, pinnedIds, request, stats, marks ->
        PlacesPart.of(all, pinnedIds, request, stats, marks.guideScreenFrom, marks.dummyRouteAttempt)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.progress,
        message,
        sdkCheck,
        places,
    ) { progress, shown, sdk, part ->
        val parked = sdk.drivingState == DrivingState.PARKED
        sdk.copy(
            recording = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            message = shown,
            pinnedPlaces = part.pinnedPlaces,
            candidates = part.candidates,
            placesVisible = parked,
            canPinMore = part.canPinMore,
            routeRequest = part.routeRequest,
            placeStats = part.placeStats,
            // 주행 · 모름이면 전용 화면 · 더미 경로는 없음(U54 넓힘 · 닫힘은 아래 init 구독이 기록)
            guideScreen = part.guideScreen.takeIf { parked },
            dummyRoute = part.dummyRoute.takeIf { parked },
            dummyRouteAvailable = demoFeatures.dummyRoute,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch { candidates.value = placeCatalogRepository.candidates() }
        // 주행 · 모름이 되면 길안내 연결 화면 · 더미 경로를 닫음 — 정차로 돌아와도 홈 목록(U54 넓힘 · T124 · T125).
        // 주행 중엔 홈 화면이 그려지지 않아 uiState 구독이 멈추므로 따로 받음(주행 판정 출처는 하나 · 공유 흐름).
        viewModelScope.launch {
            drivingSignalRepository.drivingState.collect { driving ->
                if (driving != DrivingState.PARKED) closeGuideScreen()
            }
        }
    }

    fun onRecordPlace() {
        viewModelScope.launch {
            if (recordPlace() == ActionResult.NOT_READY) message.value = UiMessage.NOT_READY
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    /**
     * 「여기로 안내」(navigation 3.2 「길안내 요청」) — 요청을 시작하면(또는 그 순간 미연결로 바로 실패하면) 길안내 연결 화면(2.3)이 열림(U68).
     * 주행 · 모름 · 요청 중이면 무시(T5 · 화면도 그대로).
     */
    fun onGuide(placeId: String) {
        if (!uiState.value.placesVisible) return
        val place = candidates.value?.firstOrNull { it.id == placeId } ?: return
        val current = guideRequests.state.value
        if (current is GuideRequestState.Requesting) return
        // 입구가 상태를 바꾸기 전에 「다음 변경부터 화면에 보임」을 써 둠 — 요청 중 · 즉시 실패 어느 쪽이든 그 상태와 함께 열림
        dummyRouteAttempt.value = null
        guideScreenFrom.value = current.seq + 1
        guideRequests.guide(place)
    }

    /** 「길안내 화면 닫기」(2.3 G6 · 시스템 뒤로) — 요청은 취소하지 않음 · 결과는 홈 E3 에(T122). 주행 · 모름이면 무시(T132). */
    fun onCloseGuideScreen() {
        if (!uiState.value.placesVisible) return
        closeGuideScreen()
    }

    private fun closeGuideScreen() {
        guideScreenFrom.value = null
        dummyRouteAttempt.value = null
    }

    /** 화면이 다시 앞으로 옴(시작 · 재개 · R1) — 바로 앞 요청이 아직 시작 알림을 못 받았으면 입구가 내비 상태로 대조. */
    fun onScreenResumed() {
        guideRequests.recheck()
    }

    /**
     * 「다시 시도」(navigation 3.2 「실패 폴백」 · E11) — 실패한 그 장소로 「여기로 안내」와 같은 길.
     * 그 장소를 고정 해제했어도 요청(T112) · 정차 · 실패가 아니면 무시(T104 · T105).
     */
    fun onRetry() {
        if (!uiState.value.placesVisible) return
        guideRequests.retry()
    }

    /**
     * 「더미 경로」(E12 · E13) — 실패한 장소의 이름 · 주소만 패널에 · 내비 요청 · 핀 · 서버 · 기기 저장 · 로그 없음(U56 · T101).
     * debug 빌드 · 정차 · 길안내 연결 화면 열림 · 실패일 때만(켜져 있으면 같은 번호를 다시 씀 — 그대로).
     */
    fun onShowDummyRoute() {
        if (!demoFeatures.dummyRoute || !uiState.value.placesVisible || uiState.value.guideScreen == null) return
        val failed = guideRequests.state.value as? GuideRequestState.Failed ?: return
        dummyRouteAttempt.value = failed.attempt
    }

    /** 「고정」 — 정차일 때만 · 최대면 저장소가 그대로 둠(T36). */
    fun onPin(placeId: String) {
        if (!uiState.value.placesVisible) return
        viewModelScope.launch { pinnedPlacesRepository.pin(placeId) }
    }

    /** 「고정 해제」 — 정차일 때만 · 핀(여행 기록)에는 영향 없음. */
    fun onUnpin(placeId: String) {
        if (!uiState.value.placesVisible) return
        viewModelScope.launch { pinnedPlacesRepository.unpin(placeId) }
    }

    /** debug SDK 확인 카드가 보일 때만 부름(release 엔 카드가 없음 · D9). */
    fun refreshNavi() {
        if (naviState.value is SdkStatus.Loading) return
        naviState.value = SdkStatus.Loading
        viewModelScope.launch {
            naviState.value = runCatching {
                withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.routeState() }
            }.fold(
                onSuccess = { SdkStatus.Ready(it) },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    /** debug 카드의 「안내 건수」 한 줄(navigation 4.6 · U41) — 카드가 보일 때만 부름. */
    fun refreshPlaceStats() {
        if (placeStats.value is SdkStatus.Loading) return
        placeStats.value = SdkStatus.Loading
        viewModelScope.launch {
            val names = (candidates.value ?: placeCatalogRepository.candidates()).associate { it.id to it.name }
            placeStats.value = placeStatsRepository.topStats(AppConfig.PLACE_STATS_CARD_LIMIT).fold(
                onSuccess = { stats ->
                    SdkStatus.Ready(stats.map { PlaceStatItem(names[it.placeId] ?: it.placeId, it.startCount) })
                },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    private fun Throwable.toCode(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

    /** 「가 볼 장소」 · 「장소 고르기」 부분 — 후보에 없는 고정 id 는 보이지 않고 세지 않음(navigation 2.1). */
    private data class PlacesPart(
        val pinnedPlaces: List<PlaceItem>?,
        val candidates: List<CandidateItem>,
        val canPinMore: Boolean,
        val routeRequest: RouteRequestState,
        val placeStats: SdkStatus<List<PlaceStatItem>>,
        val guideScreen: PlaceItem?,
        val dummyRoute: PlaceItem?,
    ) {
        companion object {
            fun of(
                all: List<Place>?,
                pinnedIds: List<String>?,
                request: GuideRequestState,
                stats: SdkStatus<List<PlaceStatItem>>,
                screenFrom: Long?,
                dummyAttempt: Long?,
            ): PlacesPart {
                val screen = guideScreenOf(request, screenFrom)
                val byId = all.orEmpty().associateBy { it.id }
                val pinned = if (all == null || pinnedIds == null) {
                    null
                } else {
                    pinnedIds.mapNotNull { byId[it] }.map { PlaceItem(it.id, it.name, it.address) }
                }
                val pinnedSet = pinned.orEmpty().map { it.id }.toSet()
                return PlacesPart(
                    pinnedPlaces = pinned,
                    candidates = all.orEmpty().map { CandidateItem(it.id, it.name, it.address, it.id in pinnedSet) },
                    canPinMore = pinnedSet.size < AppConfig.PINNED_PLACES_MAX,
                    routeRequest = request.toUi(),
                    placeStats = stats,
                    guideScreen = screen,
                    dummyRoute = if (screen == null) null else dummyRouteOf(request, dummyAttempt),
                )
            }
        }
    }
}

/** 입구 상태 → 화면 상태(navigation 3.1) — 시작됨은 요청할 때의 장소 이름 · 실패는 이유 · 장소 id. */
internal fun GuideRequestState.toUi(): RouteRequestState = when (this) {
    GuideRequestState.Idle -> RouteRequestState.Idle
    is GuideRequestState.Requesting -> RouteRequestState.Requesting(place.id)
    is GuideRequestState.Started -> RouteRequestState.Started(place.name)
    is GuideRequestState.Failed -> RouteRequestState.Failed(reason, place.id)
}

/**
 * 길안내 연결 화면(2.3)에 그릴 장소 — 화면을 연 뒤의 입구 상태([GuideRequestState.seq] ≥ [screenFrom])일 때만 · 그 상태의 장소.
 * 입구 상태 하나에서 나오므로 화면의 장소와 요청 상태가 어긋날 수 없음.
 */
internal fun guideScreenOf(request: GuideRequestState, screenFrom: Long?): PlaceItem? {
    if (screenFrom == null || request.seq < screenFrom) return null
    val place = when (request) {
        GuideRequestState.Idle -> return null
        is GuideRequestState.Requesting -> request.place
        is GuideRequestState.Started -> request.place
        is GuideRequestState.Failed -> request.place
    }
    return PlaceItem(place.id, place.name, place.address)
}

/**
 * 더미 경로(2.3 G1-D · 옛 E13)에 그릴 장소 — 입구가 지금 실패이고 그 시도 번호가 패널을 연 번호와 같을 때만(R4-1 · 파생).
 * 요청 중 · 시작됨이면 [dummyAttempt] 가 무엇이든 없음 → 「요청 중 + 패널 열림」은 만들 수 없음.
 */
internal fun dummyRouteOf(request: GuideRequestState, dummyAttempt: Long?): PlaceItem? {
    val failed = request as? GuideRequestState.Failed ?: return null
    if (dummyAttempt == null || failed.attempt != dummyAttempt) return null
    return PlaceItem(failed.place.id, failed.place.name, failed.place.address)
}
