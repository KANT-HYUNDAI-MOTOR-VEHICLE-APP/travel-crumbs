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

    /** 「여기로 안내」(navigation 3.2 「길안내 요청」) — 주행 · 모름 · 요청 중이면 무시. */
    fun onGuide(placeId: String) {
        if (!uiState.value.placesVisible) return
        if (routeRequest.value is RouteRequestState.Requesting) return
        val place = candidates.value?.firstOrNull { it.id == placeId } ?: return
        if (!naviRepository.ready.value) {
            // 버튼 비활성과 연결 끊김이 엇갈림 — 내비에 요청하지 않음(T9)
            routeRequest.value = RouteRequestState.Failed(RouteFailReason.NOT_CONNECTED)
            return
        }
        routeRequest.value = RouteRequestState.Requesting(place.id)
        viewModelScope.launch {
            val result = awaitRouteResult(naviRepository.ready, AppConfig.ROUTE_START_TIMEOUT_MILLIS) {
                naviRepository.requestRoute(place)
            }
            routeRequest.value = when (result) {
                RouteRequestResult.Started -> RouteRequestState.Started(place.name)
                is RouteRequestResult.Failed -> RouteRequestState.Failed(result.reason)
            }
            if (result == RouteRequestResult.Started) recordPin(place.id)
        }
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
    }
}
