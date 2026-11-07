package com.example.travelcrumbs.ui.tripsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.TripName
import com.example.travelcrumbs.domain.TripNameSuggester
import com.example.travelcrumbs.domain.model.TripAlreadyRecordingException
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 여행 시작 설정(A-04) — 여행명 기본값 · 동행자 선택 · 「여행 시작」.
 * 저장: trip_mst 1행 + trip_companion_dtl n행 + trip_status_hist(RECORDING) 1행 — 서버가 한 번에.
 */
@HiltViewModel
class TripSetupViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val codeRepository: CodeRepository,
    private val nameSuggester: TripNameSuggester,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val state = MutableStateFlow(TripSetupUiState())
    val uiState: StateFlow<TripSetupUiState> = state.asStateFlow()

    /** 화면이 열릴 때 한 번 — 이전에 열었던 값은 모두 지움. */
    fun prepare() {
        val today = Instant.ofEpochMilli(timeSource.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
        state.value = TripSetupUiState(tripName = TripName.default(today))
        loadCompanions()
    }

    fun onRetry() = loadCompanions()

    /** 「수정」 — 추천 자리(Gleo AI)에서 이름을 받아 입력칸을 엶. 추천이 실패하면 지금 이름 그대로. */
    fun onEditName() {
        val current = state.value
        if (current.editingName || current.suggesting) return
        state.update { it.copy(suggesting = true) }
        viewModelScope.launch {
            val suggested = runCatching { nameSuggester.suggest(current.tripName) }
                .getOrDefault(current.tripName)
                .take(TripName.MAX_LENGTH)
            state.update { it.copy(tripName = suggested, editingName = true, suggesting = false) }
        }
    }

    fun onNameChange(text: String) {
        state.update { it.copy(tripName = text.take(TripName.MAX_LENGTH)) }
    }

    /** 동행자 여러 개 선택 — 「혼자」는 다른 동행자와 함께 고를 수 없음(가안 · 결정 로그 D12). */
    fun onToggleCompanion(code: String) {
        state.update { s ->
            val selected = s.selectedCompanions
            val next = when {
                code in selected -> selected - code
                code == SOLO -> setOf(SOLO)
                else -> selected - SOLO + code
            }
            s.copy(selectedCompanions = next)
        }
    }

    fun onStart() {
        val current = state.value
        if (!current.canStart) return
        state.update { it.copy(starting = true) }
        // 화면 순서대로 보냄 → trip_companion_dtl 도 그 순서로 쌓임
        val codes = current.companions.map { it.code }.filter { it in current.selectedCompanions }
        viewModelScope.launch {
            tripRepository.startTrip(current.tripName.trim(), codes).fold(
                onSuccess = { trip -> state.update { it.copy(starting = false, startedTripName = trip.tripName) } },
                onFailure = { error ->
                    val message = if (error is TripAlreadyRecordingException) {
                        UiMessage.TRIP_ALREADY_RECORDING
                    } else {
                        UiMessage.SERVER_UNAVAILABLE
                    }
                    state.update { it.copy(starting = false, message = message) }
                },
            )
        }
    }

    /** 화면이 닫힐 때(취소 · 시작 완료) — 다음에 열 때 지난 값이 남지 않게. */
    fun reset() {
        state.value = TripSetupUiState()
    }

    fun onMessageShown() {
        state.update { it.copy(message = null) }
    }

    private fun loadCompanions() {
        state.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            codeRepository.codes(COMPANION_TYPE).fold(
                onSuccess = { codes -> state.update { it.copy(loading = false, companions = codes) } },
                onFailure = {
                    state.update {
                        it.copy(loading = false, loadFailed = true, message = UiMessage.SERVER_UNAVAILABLE)
                    }
                },
            )
        }
    }

    private companion object {
        const val COMPANION_TYPE = "COMPANION_TYPE"
        const val SOLO = "SOLO"
    }
}
