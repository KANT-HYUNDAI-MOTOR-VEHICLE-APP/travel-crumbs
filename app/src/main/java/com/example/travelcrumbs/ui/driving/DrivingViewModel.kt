package com.example.travelcrumbs.ui.driving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 주행 전용 화면 — 여행명 · 상태 · 경과 · 장소 수 · 버튼 4개 · 여행 종료 확인 팝업(스펙 app-shell-v5 2-5 · 2-6).
 * 팝업 표시는 SavedStateHandle 에 둬 프로세스 재생성 뒤 다시 보임(V32).
 */
@HiltViewModel
class DrivingViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordPlace: RecordPlaceUseCase,
    private val startVoiceInput: StartVoiceInputUseCase,
    drivingSignalRepository: DrivingSignalRepository,
    private val savedStateHandle: SavedStateHandle,
    private val ratingRepository: RatingRepository,
    private val naviRepository: NaviRepository,
    gleoActionRepository: GleoActionRepository,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    /** 내비 안내 카드 — 안내 중인 목적지 이름(null = 안내 중 아님 · 모름) · 내비 앱 유무. */
    private val navi = MutableStateFlow(NaviCard())
    private var naviRefresh: Job? = null

    val uiState: StateFlow<DrivingUiState> = combine(
        recordingRepository.progress,
        savedStateHandle.getStateFlow(KEY_END_CONFIRM, false),
        message,
        navi,
    ) { progress, endConfirm, shown, card ->
        DrivingUiState(
            tripName = progress.tripName,
            tripStatus = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            placeCount = progress.placeCount,
            endConfirmVisible = endConfirm,
            message = shown,
            naviDestinationName = card.destinationName,
            naviAppAvailable = card.appAvailable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = DrivingUiState(),
    )

    init {
        // 팝업 열린 채 정차로 바뀌면 팝업 닫힘 · 상태 그대로(N11) — 다시 주행이 돼도 저절로 열리지 않음.
        viewModelScope.launch {
            drivingSignalRepository.drivingState.collect { state ->
                if (state == DrivingState.PARKED) savedStateHandle[KEY_END_CONFIRM] = false
            }
        }
        // Gleo 「여행 끝낼게」(#69 U2) — 저장소는 바꾸지 않고 기존 확인 팝업만 엶 · 명령마다 한 번.
        viewModelScope.launch {
            gleoActionRepository.commandState.collect { command ->
                if (command.screen == GleoScreenRequest.ConfirmEndTrip && savedStateHandle.get<Long>(KEY_GLEO_END) != command.noticeId) {
                    savedStateHandle[KEY_GLEO_END] = command.noticeId
                    savedStateHandle[KEY_END_CONFIRM] = true
                }
            }
        }
    }

    /** 「이 장소 기록」 — 위치만 임시저장 · 「위치를 저장했어요 · 태그는 정차 후에」(A-02 · #51 U4). */
    fun onRecordPlace() {
        viewModelScope.launch {
            message.value = when (recordPlace()) {
                ActionResult.DONE -> UiMessage.PLACE_DRAFT_SAVED
                ActionResult.NOT_READY, ActionResult.FAILED, ActionResult.UNCONFIRMED -> UiMessage.NOT_READY
            }
        }
    }

    fun onVoice() = runAction { startVoiceInput() }

    /** 「기록 일시정지」 · 「기록 재개」 — 확인 없이 한 번 터치 = 한 전이(D31 · V66). */
    fun onTogglePause() {
        viewModelScope.launch { recordingRepository.togglePause() }
    }

    /** 「여행 종료」 — 확인 팝업만 엶 · 상태는 그대로(D22 · RECORDING · PAUSED 모두 · N24). */
    fun onEndTrip() {
        savedStateHandle[KEY_END_CONFIRM] = true
    }

    /** 「계속 기록」 · 시스템 뒤로 · 팝업 바깥 누름(레일 칸 포함) — 팝업만 닫음 · PAUSED 면 PAUSED 그대로(N24). */
    fun onContinueRecording() {
        savedStateHandle[KEY_END_CONFIRM] = false
    }

    /** 「종료」 — COMPLETED · 종료 시각 = 지금 · 팝업 닫음 · 화면은 주행 화면 그대로. */
    fun onConfirmEndTrip() {
        savedStateHandle[KEY_END_CONFIRM] = false
        viewModelScope.launch {
            // 별점(#41) — 안내받은 추천 로드맵이 있으면 정차 뒤 기록 화면에서 「따라간 코스 어땠나요?」(주행 중엔 안 띄움).
            // 종료 시각은 기록 저장소가 정한 값 하나 · 이미 끝난 여행이면 null(아무 일 없음)
            recordingRepository.endTrip()?.let { endedAt -> ratingRepository.markGuidedTripsEnded(endedAt) }
        }
    }

    /**
     * 화면이 앞에 옴 — 내비 안내 상태를 바로 한 번 · 그 뒤 [AppConfig.NAVI_STATUS_REFRESH_MILLIS] 마다 다시 읽음(#51 U0).
     * 화면이 뒤로 가면 [onScreenPaused] 가 멈춤(주행 중 CPU).
     */
    fun onScreenResumed() {
        naviRefresh?.cancel()
        naviRefresh = viewModelScope.launch {
            while (true) {
                navi.value = NaviCard(destinationName = guidingDestination(), appAvailable = naviRepository.naviAppAvailable())
                delay(AppConfig.NAVI_STATUS_REFRESH_MILLIS)
            }
        }
    }

    fun onScreenPaused() {
        naviRefresh?.cancel()
        naviRefresh = null
    }

    /** 「내비 보기」 — 내비 앱을 앞으로(나눈 상태면 내비 칸 · 아니면 앱 영역 전체). */
    fun onOpenNavi() {
        naviRepository.openNaviApp()
    }

    /** 내비가 안내 중인 목적지 이름 — 연결 안 됨 · 조회 실패 · 한도 지남 · 안내 중 아님이면 null(이름만 · 좌표는 쓰지 않음). */
    private suspend fun guidingDestination(): String? {
        if (!naviRepository.ready.value) return null
        val route = runCatching {
            withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.activeRoute() }
        }.getOrNull() ?: return null
        return route.destinationName.takeIf { route.routing && it.isNotBlank() }
    }

    private data class NaviCard(val destinationName: String? = null, val appAvailable: Boolean = false)

    fun onMessageShown() {
        message.value = null
    }

    private fun runAction(action: suspend () -> ActionResult) {
        viewModelScope.launch {
            if (action() == ActionResult.NOT_READY) message.value = UiMessage.NOT_READY
        }
    }

    private companion object {
        const val KEY_END_CONFIRM = "end_confirm_visible"
        const val KEY_GLEO_END = "gleo_end_confirm_notice"
    }
}
