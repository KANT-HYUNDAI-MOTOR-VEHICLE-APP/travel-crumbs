package com.example.travelcrumbs.ui.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.components.toSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A-07 내 여행 보관함(와이어프레임 v6 · #51 U6) — 끝난 여행 카드(여행명 · 날짜 · 장소 수 · 동행자 · 공개 상태 · 받은 별점) ·
 * 공개 토글 즉시 반영(처음 공개면 추천 공개 동의를 물음) · 카드 → A-08. 고른 여행은 SavedStateHandle 에.
 * 받은 별점 = 내 공개 로드맵이 추천에 올라 받은 별점(기기 안 별점 기록 · 추천 번호는 [AppConfig.LOCAL_TRIP_RECO_NO_OFFSET] + 여행 번호).
 */
@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val userRepository: CurrentUserRepository,
    ratingRepository: RatingRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<TripsUiState> = combine(
        tripRepository.trips,
        ratingRepository.records,
        savedStateHandle.getStateFlow<Long?>(KEY_SELECTED, null),
        savedStateHandle.getStateFlow<Long?>(KEY_CONSENT_ASK, null),
        message,
    ) { trips, ratings, selected, ask, shown ->
        TripsUiState(
            trips = trips.filter { it.status == TripStatus.COMPLETED }.map { trip ->
                val mine = ratings.ratingOf(AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + trip.tripNo)
                trip.toSummary(timeSource.zoneId(), ratingAvg = mine?.ratingScore?.toDouble() ?: 0.0, ratingCnt = if (mine == null) 0 else 1)
            },
            selectedTripNo = selected,
            consentAskTripNo = ask,
            message = shown,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = TripsUiState(),
    )

    /** 앱 틀의 진입 값이 지난번과 다르면 처음부터 — [openTripNo] 가 있으면 그 여행 상세(A-08) · 없으면 목록. */
    fun onEntry(entry: Int, openTripNo: Long?) {
        if (savedStateHandle.get<Int>(KEY_ENTRY) == entry) return
        savedStateHandle[KEY_ENTRY] = entry
        savedStateHandle[KEY_SELECTED] = openTripNo
        savedStateHandle[KEY_CONSENT_ASK] = null
    }

    fun onOpenTrip(tripNo: Long) {
        savedStateHandle[KEY_SELECTED] = tripNo
    }

    /** A-08 에서 뒤로 — 목록. */
    fun onCloseTrip() {
        savedStateHandle[KEY_SELECTED] = null
    }

    /** 공개 토글 — 끄면 바로 비공개 · 켜면 동의가 있으면 바로 공개 · 없으면 동의 팝업. */
    fun onTogglePublic(tripNo: Long, public: Boolean) {
        viewModelScope.launch {
            if (!public) {
                tripRepository.setVisibility(tripNo, Visibility.PRIVATE)
            } else if (userRepository.consents.first()[ConsentType.RECO_PUBLIC]?.agreed == true) {
                tripRepository.setVisibility(tripNo, Visibility.ANONYMOUS_PUBLIC)
            } else {
                savedStateHandle[KEY_CONSENT_ASK] = tripNo
            }
        }
    }

    /** 동의 팝업 「동의하고 공개」. */
    fun onConsentAndPublish() {
        val tripNo = savedStateHandle.get<Long>(KEY_CONSENT_ASK) ?: return
        savedStateHandle[KEY_CONSENT_ASK] = null
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
            tripRepository.setVisibility(tripNo, Visibility.ANONYMOUS_PUBLIC)
        }
    }

    fun onDismissConsent() {
        savedStateHandle[KEY_CONSENT_ASK] = null
    }

    fun onMessageShown() {
        message.value = null
    }

    private companion object {
        const val KEY_ENTRY = "trips_entry"
        const val KEY_SELECTED = "trips_selected_trip_no"
        const val KEY_CONSENT_ASK = "trips_consent_ask_trip_no"
    }
}
