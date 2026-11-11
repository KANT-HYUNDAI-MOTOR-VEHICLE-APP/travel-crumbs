package com.example.travelcrumbs.ui.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.gleo.freshScreenRequests
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.Region
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
 * 저장된 id 가 없어진 목적지(`home` · `inbox`)면 「기록」으로 읽음(AppDestination.fromId · 3-5).
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    drivingSignalRepository: DrivingSignalRepository,
    private val unfinishedInputRepository: UnfinishedInputRepository,
    private val savedStateHandle: SavedStateHandle,
    gleoActionRepository: GleoActionRepository,
) : ViewModel() {

    /** 칸 진입 값 — 추천 칸 처음부터(#37) · 내 여행 칸에서 열 여행(#51 U2 · U6). combine 인자 수를 줄이려고 하나로. */
    private val entries = combine(
        savedStateHandle.getStateFlow(KEY_RECOMMENDATIONS_ENTRY, 0),
        savedStateHandle.getStateFlow(KEY_TRIPS_ENTRY, 0),
        savedStateHandle.getStateFlow<Long?>(KEY_TRIPS_OPEN, null),
        savedStateHandle.getStateFlow<String?>(KEY_RECOMMENDATIONS_SIDO, null),
        savedStateHandle.getStateFlow<String?>(KEY_RECOMMENDATIONS_SIGUNGU, null),
    ) { recommendations, trips, openTripNo, sido, sigungu ->
        Entries(recommendations, trips, openTripNo, sido?.let { Region(it, sigungu) })
    }

    private data class Entries(val recommendations: Int, val trips: Int, val openTripNo: Long?, val recoRegion: Region?)

    /** 판정 — 아래 init 이 목적지를 먼저 고친 뒤 이 값을 바꿈(전환 순간 옛 목적지가 한 번 보이지 않게). null = 첫 값 전. */
    private val drivingState = MutableStateFlow<DrivingState?>(null)

    val uiState: StateFlow<AppShellUiState> = combine(
        drivingState,
        savedStateHandle.getStateFlow(KEY_DESTINATION, AppDestination.START.id),
        savedStateHandle.getStateFlow(KEY_DRIVING_DESTINATION, AppDestination.DRIVING_START.id),
        unfinishedInputRepository.savedInput,
        entries,
    ) { driving, selected, drivingDestination, unfinished, entry ->
        AppShellUiState(
            // 첫 값 전엔 모름 → 주행처럼(안전 쪽 · #6 D12)
            drivingState = driving ?: DrivingState.UNKNOWN,
            selectedDestination = AppDestination.fromId(selected),
            drivingDestination = AppDestination.fromId(drivingDestination),
            unfinishedInputAvailable = unfinished != null,
            recommendationsEntry = entry.recommendations,
            tripsEntry = entry.trips,
            tripsOpenTripNo = entry.openTripNo,
            recommendationsRegion = entry.recoRegion,
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
        // Gleo 화면 요청(#69) — 지금 따를 것만(늦게 만들어진 화면은 지난 명령을 따르지 않음).
        viewModelScope.launch {
            gleoActionRepository.freshScreenRequests().collect { command ->
                command.screen?.let { onGleoScreenRequest(command.noticeId, it) }
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
        if (parked && destination == AppDestination.RECOMMENDATIONS) {
            // 레일로 들어오면 Gleo 가 남긴 지역을 쓰지 않고 B-01 부터.
            savedStateHandle[KEY_RECOMMENDATIONS_SIDO] = null
            savedStateHandle[KEY_RECOMMENDATIONS_SIGUNGU] = null
            enterRecommendations()
        }
        if (parked && destination == AppDestination.TRIPS) {
            savedStateHandle[KEY_TRIPS_OPEN] = null
            savedStateHandle[KEY_TRIPS_ENTRY] = (savedStateHandle.get<Int>(KEY_TRIPS_ENTRY) ?: 0) + 1
        }
    }

    /**
     * 기록 칸 「추천 보러 가기」(진행 중 여행이 아닐 때만 보임 · D34) → 「추천」 칸.
     * [region] = Gleo 가 고른 지역(#69) — 추천 칸이 진입하자마자 그 지역 순위(B-02)를 엶. null 이면 B-01 부터.
     */
    fun openRecommendations(region: Region? = null) {
        savedStateHandle[KEY_DESTINATION] = AppDestination.RECOMMENDATIONS.id
        savedStateHandle[KEY_RECOMMENDATIONS_SIDO] = region?.sidoNm
        savedStateHandle[KEY_RECOMMENDATIONS_SIGUNGU] = region?.sigunguNm
        enterRecommendations()
    }

    /**
     * Gleo 명령 뒤 화면 요청(#69 2절) — 명령마다 한 번만(화면 재구성 · 프로세스 재생성 뒤 다시 따르지 않음).
     * 주행 · 모름에서는 화면을 옮기지 않음(주행 중 허용 칸은 「기록」뿐 · 종료 확인은 주행 화면 ViewModel 이 받음).
     * 종료 확인은 정차 중 「기록」 칸으로 옮겨 그 칸의 기존 팝업이 보이게 함.
     */
    fun onGleoScreenRequest(noticeId: Long, request: GleoScreenRequest) {
        if (savedStateHandle.get<Long>(KEY_GLEO_HANDLED) == noticeId) return
        savedStateHandle[KEY_GLEO_HANDLED] = noticeId
        if (drivingState.value != DrivingState.PARKED) return
        when (request) {
            GleoScreenRequest.OpenTrips -> openTrip(null)
            is GleoScreenRequest.OpenRecommendations -> openRecommendations(request.region)
            GleoScreenRequest.ConfirmEndTrip -> savedStateHandle[KEY_DESTINATION] = AppDestination.RECORDS.id
        }
    }

    /**
     * 내 여행 하나를 엶(C-02 최근 여행 카드 · A-06 저장 뒤 · #51) — 「내 여행」 칸으로 · 그 칸이 [tripNo] 상세(A-08)부터.
     * [tripNo] null = 보관함(A-07) 목록부터.
     */
    fun openTrip(tripNo: Long?) {
        savedStateHandle[KEY_DESTINATION] = AppDestination.TRIPS.id
        savedStateHandle[KEY_TRIPS_OPEN] = tripNo
        savedStateHandle[KEY_TRIPS_ENTRY] = (savedStateHandle.get<Int>(KEY_TRIPS_ENTRY) ?: 0) + 1
    }

    /** 사용자가 「추천」을 새로 고름 → 추천 칸이 B-01 부터(#37 사람 결정). 주행 → 정차 복원은 이 값을 바꾸지 않음. */
    private fun enterRecommendations() {
        savedStateHandle[KEY_RECOMMENDATIONS_ENTRY] = (savedStateHandle.get<Int>(KEY_RECOMMENDATIONS_ENTRY) ?: 0) + 1
    }

    /** 정차 중 시스템 뒤로 — 「기록」이 아닌 칸에서만 불림 → 「기록」. */
    fun onBack() {
        savedStateHandle[KEY_DESTINATION] = AppDestination.START.id
    }

    /** 「이어서 쓰기」 — 저장 당시 목적지로 이동(없어진 id 면 「기록」 · V76) + 그 화면이 복원받음 · 저장 비움. */
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
        const val KEY_RECOMMENDATIONS_ENTRY = "recommendations_entry"
        const val KEY_TRIPS_ENTRY = "trips_entry"
        const val KEY_TRIPS_OPEN = "trips_open_trip_no"
        const val KEY_RECOMMENDATIONS_SIDO = "recommendations_sido"
        const val KEY_RECOMMENDATIONS_SIGUNGU = "recommendations_sigungu"
        const val KEY_GLEO_HANDLED = "gleo_handled_notice"
    }
}
