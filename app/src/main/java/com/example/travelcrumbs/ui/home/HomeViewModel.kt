package com.example.travelcrumbs.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.place.PlaceRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.UserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.domain.model.UserProfile
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** 홈(스펙 app-shell 2-2) — 기록 상태는 주행 화면과 같은 출처(RecordingRepository · D1). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    recordingRepository: RecordingRepository,
    private val recordPlace: RecordPlaceUseCase,
    drivingSignalRepository: DrivingSignalRepository,
    gleoActionRepository: GleoActionRepository,
    private val naviRepository: NaviRepository,
    private val userRepository: UserRepository,
    private val consentRepository: ConsentRepository,
    private val tripRepository: TripRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)
    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)

    /** 로그인된 사용자 — 화면이 열리면 한 번 부름(홈 A-03 인사말). */
    private val profile = MutableStateFlow<SdkStatus<UserProfile>>(SdkStatus.Idle)

    /** 홈 위에 띄운 것 — 위치정보 동의 창(A-03-1) 또는 여행 시작 설정(A-04). 둘은 동시에 뜨지 않음. */
    private val overlay = MutableStateFlow(Overlay.NONE)

    init {
        loadProfile()
        // 기록 중인 여행이 바뀌거나 장소가 늘면(홈 · 주행 화면의 「이 장소 기록」) 임시 기록을 다시 읽음
        viewModelScope.launch {
            tripRepository.activeTrip
                .map { trip -> trip?.let { it.tripUuid to it.placeCount } }
                .distinctUntilChanged()
                .collect { key ->
                    if (key == null) placeRepository.clearDrafts() else placeRepository.refreshDrafts(key.first)
                }
        }
    }

    private val sdkCheck = combine(
        drivingSignalRepository.drivingState,
        naviRepository.ready,
        naviState,
        gleoActionRepository.openedByGleoAction,
    ) { driving, naviReady, navi, openedByGleo ->
        HomeUiState(
            drivingState = driving,
            naviReady = naviReady,
            navi = navi,
            openedByGleoAction = openedByGleo,
        )
    }

    /** 홈 위에 띄운 것 + 기록 중인 여행(A-05 맨 위 영역) + 입력 안 한 임시 기록(A-06). */
    private val tripState = combine(overlay, tripRepository.activeTrip, placeRepository.drafts) { open, trip, drafts ->
        Triple(open, trip, drafts.size)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.progress,
        message,
        sdkCheck,
        profile,
        tripState,
    ) { progress, shown, sdk, user, (open, trip, draftCount) ->
        sdk.copy(
            recording = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            message = shown,
            nickname = if (user is SdkStatus.Ready) user.value.nickname else null,
            userUnavailable = user is SdkStatus.Failed,
            showLocationConsent = open == Overlay.LOCATION_CONSENT,
            showTripSetup = open == Overlay.TRIP_SETUP,
            activeTripName = trip?.tripName,
            placeCount = trip?.placeCount ?: 0,
            draftCount = if (trip != null) draftCount else 0,
            showPlaceInput = open == Overlay.PLACE_INPUT,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    fun onRecordPlace() {
        viewModelScope.launch {
            if (recordPlace() == ActionResult.NOT_READY) message.value = UiMessage.NOT_READY
        }
    }

    /** 사용자 정보 다시 부르기 — 서버를 켠 뒤 화면에서 다시 시도할 때. */
    fun loadProfile() {
        if (profile.value is SdkStatus.Loading) return
        profile.value = SdkStatus.Loading
        viewModelScope.launch {
            profile.value = userRepository.myProfile().fold(
                onSuccess = { SdkStatus.Ready(it) },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    /**
     * 「새 여행 시작」(A-03) — 위치정보 동의가 있으면 여행 시작 설정(A-04)으로, 없으면 동의 창(A-03-1).
     */
    fun onStartNewTrip() {
        viewModelScope.launch {
            consentRepository.isAgreed(ConsentType.LOCATION_COLLECT).fold(
                onSuccess = { agreed -> if (agreed) openTripSetup() else overlay.value = Overlay.LOCATION_CONSENT },
                onFailure = { message.value = UiMessage.SERVER_UNAVAILABLE },
            )
        }
    }

    /** 동의 창 「동의하고 시작」 — user_consent_hist 에 동의 1건 기록 후 여행 시작 설정(A-04)으로. */
    fun onAgreeLocationConsent() {
        viewModelScope.launch {
            consentRepository.agree(ConsentType.LOCATION_COLLECT).fold(
                onSuccess = {
                    overlay.value = Overlay.NONE
                    openTripSetup()
                },
                onFailure = { message.value = UiMessage.SERVER_UNAVAILABLE },
            )
        }
    }

    /** 동의 창 「동의 안 함」 — 아무것도 저장하지 않고 홈 유지. */
    fun onDeclineLocationConsent() {
        overlay.value = Overlay.NONE
    }

    /** 「장소 경험 입력」 — 임시 기록이 있을 때만 A-06. */
    fun onOpenPlaceInput() {
        overlay.value = Overlay.PLACE_INPUT
    }

    /** A-06 이 닫힘(저장 · 나중에 입력) — 임시 기록 수는 저장소가 이미 갱신. */
    fun onPlaceInputClosed() {
        overlay.value = Overlay.NONE
    }

    /** 여행 시작 설정 「취소」 — 저장 없이 홈. */
    fun onTripSetupCancel() {
        overlay.value = Overlay.NONE
    }

    /** 여행 시작 저장 완료 — 홈으로 돌아와 안내. 여행 기록 중 화면(A-05)은 묶음 4. */
    fun onTripStarted() {
        overlay.value = Overlay.NONE
        message.value = UiMessage.TRIP_STARTED
    }

    /** 기록 중인 여행이 있으면 새로 시작하지 않음(서버도 409 로 막음) — 없을 때만 A-04. */
    private suspend fun openTripSetup() {
        tripRepository.currentTrip().fold(
            onSuccess = { trip ->
                if (trip != null) message.value = UiMessage.TRIP_ALREADY_RECORDING else overlay.value = Overlay.TRIP_SETUP
            },
            onFailure = { message.value = UiMessage.SERVER_UNAVAILABLE },
        )
    }

    fun onMessageShown() {
        message.value = null
    }

    /** debug SDK 확인 카드가 보일 때만 부름(release 엔 카드가 없음 · D9). */
    fun refreshNavi() {
        if (naviState.value is SdkStatus.Loading) return
        naviState.value = SdkStatus.Loading
        viewModelScope.launch {
            naviState.value = runCatching {
                withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.routeState() }
            }.fold(
                onSuccess = { SdkStatus.Ready(it) },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    private enum class Overlay { NONE, LOCATION_CONSENT, TRIP_SETUP, PLACE_INPUT }

    private fun Throwable.toCode(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}
