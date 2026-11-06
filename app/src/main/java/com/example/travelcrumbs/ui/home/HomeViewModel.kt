package com.example.travelcrumbs.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.pin.PinRepository
import com.example.travelcrumbs.data.place.PinnedPlacesRepository
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.place.PlaceStatsRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.awaitRouteResult
import com.example.travelcrumbs.domain.isHeadingTo
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 홈(스펙 app-shell 2-2 · navigation 2.1 · 2.2) — 기록 상태는 주행 화면과 같은 출처(RecordingRepository · D1).
 * 「여기로 안내」 · 「고정」 · 「고정 해제」는 정차일 때만(navigation 3.1 · U35 · U18) — 결과는 상태 칸으로.
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
    private val pinRepository: PinRepository,
    private val placeStatsRepository: PlaceStatsRepository,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)
    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)
    private val routeRequest = MutableStateFlow<RouteRequestState>(RouteRequestState.Idle)
    private val placeStats = MutableStateFlow<SdkStatus<List<PlaceStatItem>>>(SdkStatus.Idle)

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
        routeRequest,
        placeStats,
    ) { all, pinnedIds, request, stats ->
        PlacesPart.of(all, pinnedIds, request, stats)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.progress,
        message,
        sdkCheck,
        places,
    ) { progress, shown, sdk, part ->
        sdk.copy(
            recording = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            message = shown,
            pinnedPlaces = part.pinnedPlaces,
            candidates = part.candidates,
            placesVisible = sdk.drivingState == DrivingState.PARKED,
            canPinMore = part.canPinMore,
            routeRequest = part.routeRequest,
            placeStats = part.placeStats,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch { candidates.value = placeCatalogRepository.candidates() }
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
     * 바로 앞 길안내 요청 — 결과 전(요청 중) 또는 TIMEOUT 뒤(늦은 시작 · 재개 대조를 아직 받음 · 스펙 3.2 「바로 앞 요청 X」).
     * 내비 응답([answer])은 한도가 지나도 계속 기다림 — 늦은 onRouteStarted 를 받기 위함. 새 요청을 시작하면 버림(T94).
     */
    private class GuideRequest(val place: Place, val answer: Deferred<RouteRequestResult>) {
        /** 한도가 지나 TIMEOUT 으로 보였음 — 이 뒤엔 시작됨으로만 바뀔 수 있음. */
        var timedOut = false
        val jobs = mutableListOf<Job>()

        fun stop() {
            answer.cancel()
            jobs.forEach { it.cancel() }
        }
    }

    /**
     * 바로 앞 요청 칸 — 「요청 중」 · 「TIMEOUT 뒤 기억」은 화면 상태([routeRequest])와 이 칸이 함께 나타냄(S2).
     * 이 칸을 쓰는 곳: [onGuide](바꿈) · [finish](비움 · TIMEOUT 표시) · [markStarted](비움) · [forget](비움 — 늦은 알림의
     * 목적지가 다름 · 기억 기한 지남). 읽는 곳: [onGuide](요청 중 여부) · [onScreenResumed](대조 대상) ·
     * [naviIsHeadingTo](기다린 뒤 아직 같은 요청인가) — 경로를 더하면 이 목록과 [routeRequest] 를 함께 맞출 것.
     */
    private var lastRequest: GuideRequest? = null

    /** 「여기로 안내」(navigation 3.2 「길안내 요청」) — 주행 · 모름 · 요청 중이면 무시. */
    fun onGuide(placeId: String) {
        if (!uiState.value.placesVisible) return
        if (lastRequest?.timedOut == false) return
        val place = candidates.value?.firstOrNull { it.id == placeId } ?: return
        if (!naviRepository.ready.value) {
            // 버튼 비활성과 연결 끊김이 엇갈림 — 내비에 요청하지 않음(T9)
            routeRequest.value = RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED)
            return
        }
        // 다른(또는 같은) 장소를 새로 요청 — 바로 앞 TIMEOUT 요청은 더 대조하지 않음(T94)
        lastRequest?.stop()
        val request = GuideRequest(place, viewModelScope.async { naviRepository.requestRoute(place) })
        lastRequest = request
        routeRequest.value = RouteRequestState.Requesting(place.id)
        request.jobs += viewModelScope.launch {
            var result = awaitRouteResult(naviRepository.ready, AppConfig.ROUTE_START_TIMEOUT_MILLIS) {
                request.answer.await()
            }
            // R1 — 한도가 지남(앱이 얼려져 시작 알림을 놓친 경우 포함): 내비가 지금 그 장소로 안내 중이면 시작됨
            if (result == TIMEOUT && naviIsHeadingTo(request)) result = RouteRequestResult.Started
            finish(request, result)
        }
    }

    /**
     * 화면이 다시 앞으로 옴(시작 · 재개 · R1) — 바로 앞 요청이 아직 시작 알림을 못 받았으면(요청 중 · TIMEOUT) 내비 상태로 대조.
     * 그 장소로 안내 중이면 시작됨 · 아니면 그대로(요청 중이면 한도가 끝냄 · T95).
     */
    fun onScreenResumed() {
        val request = lastRequest ?: return
        viewModelScope.launch {
            if (naviIsHeadingTo(request)) markStarted(request)
        }
    }

    /** 요청 중인 요청의 결과(시작 · 오류 · 끊김 · 한도) — 이미 정해졌거나 TIMEOUT 뒤면 무시. */
    private fun finish(request: GuideRequest, result: RouteRequestResult) {
        if (lastRequest !== request || request.timedOut) return
        when {
            result == RouteRequestResult.Started -> markStarted(request)
            result == TIMEOUT -> {
                // TIMEOUT 으로 보이되 이 요청을 기억 — 늦은 onRouteStarted 를 받으면 시작됨(스펙 3.2 · T91)
                request.timedOut = true
                routeRequest.value = RouteRequestState.Failed(RouteFailReason.TIMEOUT)
                request.jobs += viewModelScope.launch {
                    val late = try {
                        request.answer.await()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        null
                    }
                    if (late != RouteRequestResult.Started) return@launch
                    // 늦은 시작 알림도 목적지를 대조 — 사용자가 내비에서 다른 곳을 직접 정해 시작한 알림일 수 있음(심사 N1)
                    if (naviIsHeadingTo(request)) markStarted(request) else forget(request)
                }
                // TIMEOUT 뒤 기억에 기한 — 지나면 늦은 알림 · 재개 대조를 더 받지 않음(리스너 제거)
                request.jobs += viewModelScope.launch {
                    delay(AppConfig.TIMED_OUT_REQUEST_MEMORY_MILLIS)
                    forget(request)
                }
            }
            result is RouteRequestResult.Failed -> {
                lastRequest = null
                routeRequest.value = RouteRequestState.Failed(result.reason)
                request.stop()
            }
        }
    }

    /** TIMEOUT 뒤 기억한 요청을 버림 — 결과(TIMEOUT 문구)는 그대로 · 리스너 · 대기를 거둠. */
    private fun forget(request: GuideRequest) {
        if (lastRequest !== request || !request.timedOut) return
        lastRequest = null
        request.stop()
    }

    /** 바로 앞 요청이 시작됨 — 요청마다 한 번만(결과 문구 · 핀 1건 · 기록 중일 때만은 저장소가 봄 · T92 · T93). */
    private fun markStarted(request: GuideRequest) {
        if (lastRequest !== request) return
        lastRequest = null
        routeRequest.value = RouteRequestState.Started(request.place.name)
        viewModelScope.launch { recordPin(request.place.id) }
        // 기다리던 응답 · 한도 · 늦은 알림 대기를 거둠(내비 리스너 제거) — 같은 요청이 다시 정해지지 않음
        request.stop()
    }

    /**
     * 내비가 지금 [request] 의 장소로 안내 중인가 — 조회 실패 · 한도 지남이면 아님.
     * 기다리는 사이 그 요청이 이미 정해졌거나 새 요청으로 바뀌었으면 조회하지 않음.
     */
    private suspend fun naviIsHeadingTo(request: GuideRequest): Boolean = try {
        // 얼림이 풀린 직후 쌓인 콜백(늦은 onRouteStarted 포함)이 먼저 지나가게 — 그 사이 조회는 옛 상태를 받을 수 있음
        delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
        lastRequest === request &&
            withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.activeRoute() }
                ?.isHeadingTo(request.place) == true
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Log.w(TAG, "내비 상태 조회 실패: ${error.javaClass.simpleName}")
        false
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

    /** 핀 남기기 · 보내기 — 화면 상태에 영향 없음(navigation 4.6) · 실패해도 앱은 그대로. */
    private suspend fun recordPin(placeId: String) {
        try {
            pinRepository.recordGuidanceStart(placeId)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w(TAG, "핀 남기기 실패: ${error.javaClass.simpleName}")
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
    ) {
        companion object {
            fun of(
                all: List<Place>?,
                pinnedIds: List<String>?,
                request: RouteRequestState,
                stats: SdkStatus<List<PlaceStatItem>>,
            ): PlacesPart {
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
                    routeRequest = request,
                    placeStats = stats,
                )
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        val TIMEOUT = RouteRequestResult.Failed(RouteFailReason.TIMEOUT)
    }
}
