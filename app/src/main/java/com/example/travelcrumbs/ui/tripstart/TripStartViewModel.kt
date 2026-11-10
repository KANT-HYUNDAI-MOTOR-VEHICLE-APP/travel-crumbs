package com.example.travelcrumbs.ui.tripstart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuideRequestState
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.NewTrip
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A-01 여행 시작(와이어프레임 v6 · #51 U3) — 여행명(기본 「11월 10일 여행」) · 동행자 칩 · 목적지(선택 · 앱 장소 목록) · 위치 동의(처음 한 번).
 * 「기록 시작」 → trip_mst RECORDING · PRIVATE → 목적지가 있으면 성호님 길안내 입구([GuideRequestTracker.guide])로 실제 차량 내비 길안내 ·
 * 없으면 내비 앱을 엶. 내비가 앞으로 뜨고(나눈 상태면 내비 칸) 기록은 그대로 이어짐. 내비 미연결 · 열기 실패면 한 줄 안내 + 기록은 시작.
 * 입력 중 값(이름 · 동행자 · 목적지)은 SavedStateHandle 에 둠.
 */
@HiltViewModel
class TripStartViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val userRepository: CurrentUserRepository,
    private val placeCatalogRepository: PlaceCatalogRepository,
    private val guideRequests: GuideRequestTracker,
    private val naviRepository: NaviRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val places = MutableStateFlow<List<Place>>(emptyList())
    private val progress = MutableStateFlow(Progress())

    private val inputs = combine(
        savedStateHandle.getStateFlow(KEY_NAME, ""),
        savedStateHandle.getStateFlow(KEY_COMPANIONS, emptyList<String>()),
        savedStateHandle.getStateFlow<String?>(KEY_DESTINATION, null),
    ) { name, companions, destination ->
        Inputs(name, companions.mapNotNull { c -> CompanionType.entries.firstOrNull { it.name == c } }.toSet(), destination)
    }

    val uiState: StateFlow<TripStartUiState> = combine(
        inputs,
        places,
        userRepository.consents,
        progress,
    ) { input, list, consents, step ->
        val today = Instant.ofEpochMilli(timeSource.nowMillis()).atZone(timeSource.zoneId()).toLocalDate()
        TripStartUiState(
            defaultMonth = today.monthValue,
            defaultDay = today.dayOfMonth,
            tripNm = input.name,
            companions = input.companions,
            destinations = list.map { DestinationItem(it.id, it.name, it.address) },
            destinationId = input.destinationId?.takeIf { id -> list.any { it.id == id } },
            consentNeeded = consents[ConsentType.LOCATION_COLLECT]?.agreed != true,
            starting = step.starting,
            done = step.done,
            message = step.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = TripStartUiState(),
    )

    init {
        viewModelScope.launch { places.value = placeCatalogRepository.candidates() }
    }

    fun onNameChange(value: String) {
        savedStateHandle[KEY_NAME] = value.take(AppConfig.TRIP_NAME_MAX_LENGTH)
    }

    /** 동행자 칩 — 「혼자」는 다른 동행자와 함께 고를 수 없음(고르면 나머지가 풀림). */
    fun onToggleCompanion(type: CompanionType) {
        // 화면 상태(stateIn)는 한 박자 늦을 수 있어 저장 칸을 바로 읽음 — 빠르게 두 칩을 눌러도 둘 다 남게
        val now = savedStateHandle.get<List<String>>(KEY_COMPANIONS).orEmpty()
            .mapNotNull { c -> CompanionType.entries.firstOrNull { it.name == c } }.toSet()
        val next = when {
            type in now -> now - type
            type == CompanionType.SOLO -> setOf(CompanionType.SOLO)
            else -> now - CompanionType.SOLO + type
        }
        savedStateHandle[KEY_COMPANIONS] = ArrayList(next.map { it.name })
    }

    /** 목적지 고르기 — null = 「목적지 없음」. */
    fun onSelectDestination(id: String?) {
        savedStateHandle[KEY_DESTINATION] = id
    }

    /**
     * 「기록 시작」(「동의하고 기록 시작」) — [defaultTripNm] 은 화면이 만든 기본 이름(이름 칸이 비었을 때 저장).
     * 동의가 필요하면 LOCATION_COLLECT 동의 행 → 여행 저장 → 내비(길안내 요청 또는 내비 앱 열기).
     */
    fun onStart(defaultTripNm: String) {
        val state = uiState.value
        if (state.starting || state.done) return
        progress.value = Progress(starting = true)
        val destination = places.value.firstOrNull { it.id == state.destinationId }
        viewModelScope.launch {
            if (state.consentNeeded) userRepository.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
            recordingRepository.startTrip(
                NewTrip(
                    tripNm = state.tripNm.trim().ifEmpty { defaultTripNm },
                    companions = state.companions,
                    destPlaceNm = destination?.name,
                ),
            )
            val naviOk = if (destination != null) requestGuide(destination) else naviRepository.openNaviApp()
            progress.value = Progress(done = true, message = if (naviOk) null else UiMessage.NAVI_START_FAILED)
        }
    }

    /** 길안내 요청 — 입구가 바로 실패(내비 미연결)로 바꾸면 거짓 · 요청 중 · 시작이면 참(결과는 입구가 앱 수명으로 이어 받음). */
    private fun requestGuide(place: Place): Boolean {
        // 입구는 guide() 안에서 상태를 바로 바꿈(요청 중 또는 미연결 실패) — 그 값을 봄
        guideRequests.guide(place)
        return guideRequests.state.value !is GuideRequestState.Failed
    }

    /** 화면이 시작 끝을 받음 — 다음에 열면 처음부터. */
    fun onDoneHandled() {
        progress.value = Progress()
        savedStateHandle[KEY_NAME] = ""
        savedStateHandle[KEY_COMPANIONS] = ArrayList<String>()
        savedStateHandle[KEY_DESTINATION] = null
    }

    private data class Inputs(val name: String, val companions: Set<CompanionType>, val destinationId: String?)

    private data class Progress(val starting: Boolean = false, val done: Boolean = false, val message: UiMessage? = null)

    private companion object {
        const val KEY_NAME = "trip_start_name"
        const val KEY_COMPANIONS = "trip_start_companions"
        const val KEY_DESTINATION = "trip_start_destination"
    }
}
