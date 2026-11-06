package com.example.travelcrumbs.ui.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.draft.DraftRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 틀 — 주행 판정 · 선택된 목적지 · 「이어서 쓰기」(스펙 app-shell 2-1 · 2-6 · 3-4).
 * 선택된 목적지는 SavedStateHandle 에 둬 프로세스 재생성 뒤 복원.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    drivingSignalRepository: DrivingSignalRepository,
    private val draftRepository: DraftRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedDestination = savedStateHandle
        .getStateFlow(KEY_DESTINATION, AppDestination.START.id)
        .map { AppDestination.fromId(it) }

    val uiState: StateFlow<AppShellUiState> = combine(
        drivingSignalRepository.drivingState.distinctUntilChanged(),
        selectedDestination,
        draftRepository.savedDraft,
    ) { driving, destination, draft ->
        AppShellUiState(
            drivingState = driving,
            selectedDestination = destination,
            draftAvailable = draft != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        // 첫 값 전엔 모름 → 주행 화면(안전 쪽 · 스펙 D12)
        initialValue = AppShellUiState(drivingState = DrivingState.UNKNOWN),
    )

    init {
        // 정차 → 주행 · 모름 순간 입력 중 내용을 임시 저장(스펙 2-6) — uiState 구독과 따로 돌려
        // 화면이 안 보이는 사이(백그라운드)의 전환도 놓치지 않음. 판정 흐름은 저장소가 공유하므로 SDK 콜백은 하나.
        viewModelScope.launch {
            drivingSignalRepository.drivingState
                .map { it != DrivingState.PARKED }
                .distinctUntilChanged()
                .collect { showDriving -> if (showDriving) draftRepository.saveActiveInput() }
        }
    }

    /** 레일 항목 — 같은 단계 바꿔치기(쌓지 않음). */
    fun selectDestination(destination: AppDestination) {
        savedStateHandle[KEY_DESTINATION] = destination.id
    }

    /** 시스템 뒤로 — 홈이 아닌 곳에서만 불림 → 홈. */
    fun onBack() {
        selectDestination(AppDestination.START)
    }

    /** 「이어서 쓰기」 — 저장 당시 목적지로 이동 + 그 화면이 복원받음 · 저장 비움. */
    fun resumeDraft() {
        viewModelScope.launch {
            val draft = draftRepository.takeForRestore() ?: return@launch
            selectDestination(AppDestination.fromId(draft.destinationId))
        }
    }

    private companion object {
        const val KEY_DESTINATION = "selected_destination"
    }
}
