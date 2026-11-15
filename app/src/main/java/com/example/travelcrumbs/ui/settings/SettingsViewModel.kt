package com.example.travelcrumbs.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.InputValidation
import com.example.travelcrumbs.domain.PlaceRecordCleanup
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ConsentState
import com.example.travelcrumbs.domain.model.ConsentType
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
 * C-03 설정(와이어프레임 v6 · #51 U2) — 닉네임 수정(저장하면 공개 로드맵 이름도 바로 바뀜 · 추천은 닉네임을 사용자 표에서 읽음) ·
 * 동의 유형별 최신 상태 · 철회(추천 공개 철회 → 내 공개 여행 모두 비공개) · 근처 로드맵 위치 이용 철회 · 다시 동의(#55). 로그아웃은 로그인 담당 몫(보고서 남은 결정).
 * 닉네임 입력 중 값은 SavedStateHandle 에 둬 프로세스 재생성 뒤에도 그대로.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: CurrentUserRepository,
    private val tripRepository: TripRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
    private val placeCleanup: PlaceRecordCleanup,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.user,
        userRepository.consents,
        savedStateHandle.getStateFlow<String?>(KEY_NICKNAME_DRAFT, null),
        combine(
            message,
            savedStateHandle.getStateFlow(KEY_CLEAR_ASK, false),
            savedStateHandle.getStateFlow(KEY_NEARBY_ASK, false),
        ) { shown, ask, nearbyAsk -> Asks(shown, ask, nearbyAsk) },
    ) { user, consents, draft, asks ->
        SettingsUiState(
            nicknameNm = user.nicknameNm,
            locationAgreedOn = consents[ConsentType.LOCATION_COLLECT]?.agreedOn(),
            nearbyLocationAgreedOn = consents[ConsentType.LOCATION_RECO]?.agreedOn(),
            nearbyConsentAsk = asks.nearbyConsent,
            recoAgreed = consents[ConsentType.RECO_PUBLIC]?.agreed == true,
            nicknameDraft = draft,
            nicknameError = draft != null && !InputValidation.nickname(draft),
            clearAllAsk = asks.clearAll,
            message = asks.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    /** 「수정」 — 지금 닉네임으로 팝업을 엶. */
    fun onEditNickname() {
        savedStateHandle[KEY_NICKNAME_DRAFT] = uiState.value.nicknameNm
    }

    fun onNicknameChange(value: String) {
        savedStateHandle[KEY_NICKNAME_DRAFT] = value.take(AppConfig.NICKNAME_MAX_LENGTH + 1)
    }

    fun onDismissNickname() {
        savedStateHandle[KEY_NICKNAME_DRAFT] = null
    }

    /** 팝업 「저장」 — 빈 값이면 팝업 그대로. */
    fun onSaveNickname() {
        val draft = savedStateHandle.get<String>(KEY_NICKNAME_DRAFT) ?: return
        viewModelScope.launch {
            runCatching { userRepository.updateNickname(draft) }
                .onSuccess { saved ->
                    if (saved) {
                        savedStateHandle[KEY_NICKNAME_DRAFT] = null
                        message.value = UiMessage.NICKNAME_SAVED
                    }
                }
                .onFailure { message.value = UiMessage.SAVE_FAILED }
        }
    }

    /** 위치정보 수집 「철회」 — 다음 여행 시작 때 다시 물음(A-01) · 그 전까지 장소는 위치 없이 기록. */
    fun onWithdrawLocation() {
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.LOCATION_COLLECT, agreed = false)
            // 철회 = 저장된 위치도 지움(심사 개인정보 · #51 리뷰 P1) — 좌표 · 주소 칸을 물리적으로 비움
            // + 자동 기록(#66 3.9)의 기록 대기 · 진행 중 직접 안내 삭제 · 사람이 안 고친 자동 이름 비움
            placeCleanup.withdrawLocation()
            message.value = UiMessage.LOCATION_WITHDRAWN
        }
    }

    /** 추천 공개 「철회」 — 동의 철회 행 + 내 공개 여행 모두 비공개(추천에서 바로 빠짐). */
    fun onWithdrawReco() {
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.RECO_PUBLIC, agreed = false)
            tripRepository.makeAllPrivate()
            message.value = UiMessage.RECO_WITHDRAWN
        }
    }

    /**
     * 근처 로드맵 위치 이용 「철회」(#55 · 2-9) — 철회 행 · 알림. 홈 구역은 다음에 보일 때 동의 안 함(메모리 거리는 홈 도우미가 지움) ·
     * 지울 저장 위치 없음 · LOCATION_COLLECT 와 여행 기록 위치는 그대로(독립).
     */
    fun onWithdrawNearbyLocation() {
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.LOCATION_RECO, agreed = false)
            message.value = UiMessage.NEARBY_LOCATION_WITHDRAWN
        }
    }

    /** 근처 로드맵 위치 이용 「동의」 — 동의 팝업만 엶(N21). */
    fun onAskNearbyConsent() {
        savedStateHandle[KEY_NEARBY_ASK] = true
    }

    /** 팝업 「닫기」 · 시스템 뒤로 — 이력 행 0. */
    fun onDismissNearbyConsent() {
        savedStateHandle[KEY_NEARBY_ASK] = false
    }

    /** 팝업 「동의」 — 동의 행 · 팝업 닫힘 · 알림 없음. */
    fun onAgreeNearbyConsent() {
        savedStateHandle[KEY_NEARBY_ASK] = false
        viewModelScope.launch { userRepository.recordConsent(ConsentType.LOCATION_RECO, agreed = true) }
    }

    /** 「모두 지우기」 — 확인 팝업만 엶. */
    fun onAskClearAll() {
        savedStateHandle[KEY_CLEAR_ASK] = true
    }

    fun onDismissClearAll() {
        savedStateHandle[KEY_CLEAR_ASK] = false
    }

    /** 확인 팝업 「지우기」 — 내 여행 · 동행 · 장소 · 태그 관계를 물리 삭제(진행 중 기록도 끝남 · 동의 이력은 남김). */
    fun onConfirmClearAll() {
        savedStateHandle[KEY_CLEAR_ASK] = false
        viewModelScope.launch {
            placeCleanup.deleteAll()
            message.value = UiMessage.DATA_CLEARED
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    /** 동의한 날(동의 중일 때만). */
    private fun ConsentState.agreedOn(): ConsentDate? = takeIf { it.agreed }?.let {
        val date = Instant.ofEpochMilli(it.at).atZone(timeSource.zoneId()).toLocalDate()
        ConsentDate(date.year, date.monthValue, date.dayOfMonth)
    }

    private data class Asks(val message: UiMessage?, val clearAll: Boolean, val nearbyConsent: Boolean)

    private companion object {
        const val KEY_NICKNAME_DRAFT = "settings_nickname_draft"
        const val KEY_CLEAR_ASK = "settings_clear_all_ask"
        const val KEY_NEARBY_ASK = "settings_nearby_consent_ask"
    }
}
