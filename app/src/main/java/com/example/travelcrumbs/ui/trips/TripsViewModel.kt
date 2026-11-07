package com.example.travelcrumbs.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.model.TripSummary
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 여행 목록(A-07 간단판) — 내 여행(취소 제외, 최근 순). 누르면 여행 상세(A-08). */
data class TripsUiState(
    val loading: Boolean = true,
    val trips: List<TripSummary> = emptyList(),
    val loadFailed: Boolean = false,
    /** 상세를 연 여행 UUID — null 이면 목록. */
    val selectedTripUuid: String? = null,
    val message: UiMessage? = null,
)

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val state = MutableStateFlow(TripsUiState())
    val uiState: StateFlow<TripsUiState> = state.asStateFlow()

    /** 화면이 열릴 때마다 다시 읽음(방금 끝낸 여행 · 바뀐 제목 반영). */
    fun load() {
        state.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            tripRepository.myTrips().fold(
                onSuccess = { trips -> state.update { it.copy(loading = false, trips = trips) } },
                onFailure = {
                    state.update { it.copy(loading = false, loadFailed = true, message = UiMessage.SERVER_UNAVAILABLE) }
                },
            )
        }
    }

    fun onSelect(tripUuid: String) {
        state.update { it.copy(selectedTripUuid = tripUuid) }
    }

    /** 상세 「닫기」 — 목록으로 돌아와 다시 읽음. */
    fun onDetailClosed() {
        state.update { it.copy(selectedTripUuid = null) }
        load()
    }

    fun onMessageShown() {
        state.update { it.copy(message = null) }
    }
}
