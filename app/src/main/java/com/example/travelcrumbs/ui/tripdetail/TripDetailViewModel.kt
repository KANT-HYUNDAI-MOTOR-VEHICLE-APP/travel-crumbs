package com.example.travelcrumbs.ui.tripdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.TripName
import com.example.travelcrumbs.domain.TripNameSuggester
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 여행 상세(A-08) — 요약(거리 · 소요시간 · 장소 수) · 장소 타임라인 · 「제목 수정」.
 * 「제목 수정」은 이름 추천 자리(TripNameSuggester · Gleo 연결 지점 G2)를 거쳐 입력칸을 엶.
 * 저장하면 trip_mst.trip_nm 수정 + 데이터변경이력(data_change_hist) 1행 — 서버가 함.
 */
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val nameSuggester: TripNameSuggester,
) : ViewModel() {

    private val state = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = state.asStateFlow()

    private var tripUuid: String? = null

    fun load(uuid: String) {
        tripUuid = uuid
        state.value = TripDetailUiState()
        viewModelScope.launch {
            tripRepository.detail(uuid).fold(
                onSuccess = { detail -> state.update { it.copy(loading = false, detail = detail) } },
                onFailure = {
                    state.update { it.copy(loading = false, loadFailed = true, message = UiMessage.SERVER_UNAVAILABLE) }
                },
            )
        }
        // 경로 그림은 따로 — 실패해도 상세는 보임
        viewModelScope.launch {
            tripRepository.route(uuid).onSuccess { drawn -> state.update { it.copy(route = drawn) } }
        }
    }

    fun onRetry() {
        tripUuid?.let(::load)
    }

    /** 「제목 수정」 — 추천을 받아 입력칸을 엶. 추천이 실패하면 지금 이름 그대로. */
    fun onEditTitle() {
        val current = state.value.detail?.tripName ?: return
        if (state.value.busy || state.value.editingTitle) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val suggested = runCatching { nameSuggester.suggest(current) }.getOrDefault(current).take(TripName.MAX_LENGTH)
            state.update { it.copy(busy = false, editingTitle = true, titleInput = suggested) }
        }
    }

    fun onTitleChange(text: String) {
        state.update { it.copy(titleInput = text.take(TripName.MAX_LENGTH)) }
    }

    fun onCancelTitle() {
        state.update { it.copy(editingTitle = false, titleInput = "") }
    }

    fun onSaveTitle() {
        val uuid = tripUuid ?: return
        val title = state.value.titleInput.trim()
        if (title.isEmpty() || state.value.busy) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            tripRepository.rename(uuid, title).fold(
                onSuccess = { detail ->
                    state.update { it.copy(busy = false, editingTitle = false, titleInput = "", detail = detail) }
                },
                onFailure = { state.update { it.copy(busy = false, message = UiMessage.SERVER_UNAVAILABLE) } },
            )
        }
    }

    fun onMessageShown() {
        state.update { it.copy(message = null) }
    }
}
