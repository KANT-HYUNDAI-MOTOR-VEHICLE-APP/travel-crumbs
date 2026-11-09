package com.example.travelcrumbs.ui.driving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<DrivingUiState> = combine(
        recordingRepository.progress,
        savedStateHandle.getStateFlow(KEY_END_CONFIRM, false),
        message,
    ) { progress, endConfirm, shown ->
        DrivingUiState(
            tripName = progress.tripName,
            tripStatus = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            placeCount = progress.placeCount,
            endConfirmVisible = endConfirm,
            message = shown,
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
    }

    fun onRecordPlace() = runAction { recordPlace() }

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
            recordingRepository.endTrip()
            // 별점(#41) — 안내받은 추천 로드맵이 있으면 정차 뒤 기록 화면에서 「따라간 코스 어땠나요?」(주행 중엔 안 띄움)
            ratingRepository.markGuidedTripsEnded()
        }
    }

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
    }
}
