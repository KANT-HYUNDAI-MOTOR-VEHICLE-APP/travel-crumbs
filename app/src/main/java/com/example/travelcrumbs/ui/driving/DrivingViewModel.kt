package com.example.travelcrumbs.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 주행 전용 화면 — 기록 상태 · 경과 · 버튼 3개(스펙 app-shell 2-5). */
@HiltViewModel
class DrivingViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordPlace: RecordPlaceUseCase,
    private val startVoiceInput: StartVoiceInputUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<DrivingUiState> = combine(
        recordingRepository.progress,
        message,
    ) { progress, shown ->
        DrivingUiState(
            recording = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            message = shown,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DrivingUiState(),
    )

    fun onRecordPlace() = runAction { recordPlace() }

    fun onVoiceInput() = runAction { startVoiceInput() }

    fun onTogglePause() {
        viewModelScope.launch { recordingRepository.togglePause() }
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
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
