package com.example.travelcrumbs.ui.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 틀 — 주행 판정 · 정차 목적지 · 주행 중 칸 · 「이어서 쓰기」(스펙 app-shell-v5 2-1 · 2-7 · 3-2 · 3-5).
 * 두 목적지와 「주행 모드였나」는 SavedStateHandle 에 둬 프로세스 재생성 뒤 복원.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    drivingSignalRepository: DrivingSignalRepository,
    private val unfinishedInputRepository: UnfinishedInputRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 판정 — 아래 init 이 목적지를 먼저 고친 뒤 이 값을 바꿈(전환 순간 옛 목적지가 한 번 보이지 않게). null = 첫 값 전. */
    private val drivingState = MutableStateFlow<DrivingState?>(null)

    val uiState: StateFlow<AppShellUiState> = combine(
        drivingState,
        savedStateHandle.getStateFlow(KEY_DESTINATION, AppDestination.START.id),
        savedStateHandle.getStateFlow(KEY_DRIVING_DESTINATION, AppDestination.DRIVING_START.id),
        unfinishedInputRepository.savedInput,
    ) { driving, selected, drivingDestination, unfinished ->
        AppShellUiState(
            // 첫 값 전엔 모름 → 주행처럼(안전 쪽 · #6 D12)
            drivingState = driving ?: DrivingState.UNKNOWN,
            selectedDestination = AppDestination.fromId(selected),
            drivingDestination = AppDestination.fromId(drivingDestination),
            unfinishedInputAvailable = unfinished != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = AppShellUiState(drivingState = DrivingState.UNKNOWN),
    )

    init {
        // 모드 전환 규칙(2-7) — uiState 구독과 따로 돌려 화면이 안 보이는 사이(백그라운드)의 전환도 놓치지 않음.
        // 판정 흐름은 저장소가 공유하므로 SDK 콜백은 하나.
        viewModelScope.launch {
            drivingSignalRepository.drivingState
                .distinctUntilChanged()
                .collect { state ->
                    val nowDriving = state != DrivingState.PARKED
                    val wasDriving = savedStateHandle.get<Boolean>(KEY_WAS_DRIVING_MODE) ?: false
                    if (nowDriving && !wasDriving) onDrivingStarted()
                    if (!nowDriving && wasDriving) onParked()
                    savedStateHandle[KEY_WAS_DRIVING_MODE] = nowDriving
                    drivingState.value = state
                }
        }
    }

    /** 정차 → 주행 · 모름: 「기록」 칸(주행 전용 화면) · 정차 목적지는 그대로 기억 · 입력 중 내용 보존. */
    private suspend fun onDrivingStarted() {
        savedStateHandle[KEY_DRIVING_DESTINATION] = AppDestination.DRIVING_START.id
        unfinishedInputRepository.saveActiveInput()
    }

    /** 주행 · 모름 → 정차: 「기록」 칸이었으면 주행 직전 목적지 · 다른 칸이었으면 그 칸의 정차 화면(N20). */
    private fun onParked() {
        val chosen = AppDestination.fromId(savedStateHandle[KEY_DRIVING_DESTINATION])
        if (chosen != AppDestination.DRIVING_START) savedStateHandle[KEY_DESTINATION] = chosen.id
    }

    /** 레일 칸 — 같은 단계 바꿔치기(쌓지 않음). 주행 · 모름이면 주행 중 칸 · 정차면 정차 목적지를 바꿈. */
    fun selectDestination(destination: AppDestination) {
        val parked = drivingState.value == DrivingState.PARKED
        savedStateHandle[if (parked) KEY_DESTINATION else KEY_DRIVING_DESTINATION] = destination.id
    }

    /** 홈 오른쪽 위 「설정」(정차 홈에만 있음 · D21). */
    fun openSettings() {
        savedStateHandle[KEY_DESTINATION] = AppDestination.SETTINGS.id
    }

    /** 정차 중 시스템 뒤로 — 홈이 아닌 곳에서만 불림 → 홈. */
    fun onBack() {
        savedStateHandle[KEY_DESTINATION] = AppDestination.START.id
    }

    /** 「이어서 쓰기」 — 저장 당시 목적지로 이동 + 그 화면이 복원받음 · 저장 비움. */
    fun resumeUnfinishedInput() {
        viewModelScope.launch {
            val saved = unfinishedInputRepository.takeForRestore() ?: return@launch
            savedStateHandle[KEY_DESTINATION] = AppDestination.fromId(saved.destinationId).id
        }
    }

    private companion object {
        const val KEY_DESTINATION = "selected_destination"
        const val KEY_DRIVING_DESTINATION = "driving_destination"
        const val KEY_WAS_DRIVING_MODE = "was_driving_mode"
    }
}
