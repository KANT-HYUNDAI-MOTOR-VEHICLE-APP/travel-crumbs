package com.example.travelcrumbs.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
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
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)
    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)

    /** 로그인된 사용자 — 화면이 열리면 한 번 부름(홈 A-03 인사말). */
    private val profile = MutableStateFlow<SdkStatus<UserProfile>>(SdkStatus.Idle)

    /** 위치정보 동의 창(A-03-1)을 띄웠는지. */
    private val locationConsentDialog = MutableStateFlow(false)

    init {
        loadProfile()
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

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.progress,
        message,
        sdkCheck,
        profile,
        locationConsentDialog,
    ) { progress, shown, sdk, user, consentDialog ->
        sdk.copy(
            recording = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            message = shown,
            nickname = if (user is SdkStatus.Ready) user.value.nickname else null,
            userUnavailable = user is SdkStatus.Failed,
            showLocationConsent = consentDialog,
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
     * A-04 는 다음 묶음 — 지금은 「준비 중」 안내.
     */
    fun onStartNewTrip() {
        viewModelScope.launch {
            consentRepository.isAgreed(ConsentType.LOCATION_COLLECT).fold(
                onSuccess = { agreed ->
                    if (agreed) message.value = UiMessage.NOT_READY else locationConsentDialog.value = true
                },
                onFailure = { message.value = UiMessage.SERVER_UNAVAILABLE },
            )
        }
    }

    /** 동의 창 「동의하고 시작」 — user_consent_hist 에 동의 1건 기록 후 여행 시작 설정(A-04)으로. */
    fun onAgreeLocationConsent() {
        viewModelScope.launch {
            consentRepository.agree(ConsentType.LOCATION_COLLECT).fold(
                onSuccess = {
                    locationConsentDialog.value = false
                    message.value = UiMessage.NOT_READY
                },
                onFailure = { message.value = UiMessage.SERVER_UNAVAILABLE },
            )
        }
    }

    /** 동의 창 「동의 안 함」 — 아무것도 저장하지 않고 홈 유지. */
    fun onDeclineLocationConsent() {
        locationConsentDialog.value = false
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

    private fun Throwable.toCode(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}
