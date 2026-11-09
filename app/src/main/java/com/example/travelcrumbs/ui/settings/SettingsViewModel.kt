package com.example.travelcrumbs.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
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
 * 동의 유형별 최신 상태 · 철회(추천 공개 철회 → 내 공개 여행 모두 비공개). 로그아웃은 로그인 담당 몫(보고서 남은 결정).
 * 닉네임 입력 중 값은 SavedStateHandle 에 둬 프로세스 재생성 뒤에도 그대로.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: CurrentUserRepository,
    private val tripRepository: TripRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.user,
        userRepository.consents,
        savedStateHandle.getStateFlow<String?>(KEY_NICKNAME_DRAFT, null),
        message,
    ) { user, consents, draft, shown ->
        val location = consents[ConsentType.LOCATION_COLLECT]?.takeIf { it.agreed }
        SettingsUiState(
            nicknameNm = user.nicknameNm,
            locationAgreedOn = location?.let {
                val date = Instant.ofEpochMilli(it.at).atZone(timeSource.zoneId()).toLocalDate()
                ConsentDate(date.year, date.monthValue, date.dayOfMonth)
            },
            recoAgreed = consents[ConsentType.RECO_PUBLIC]?.agreed == true,
            nicknameDraft = draft,
            message = shown,
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
        savedStateHandle[KEY_NICKNAME_DRAFT] = value.take(AppConfig.NICKNAME_MAX_LENGTH)
    }

    fun onDismissNickname() {
        savedStateHandle[KEY_NICKNAME_DRAFT] = null
    }

    /** 팝업 「저장」 — 빈 값이면 팝업 그대로. */
    fun onSaveNickname() {
        val draft = savedStateHandle.get<String>(KEY_NICKNAME_DRAFT) ?: return
        viewModelScope.launch {
            if (userRepository.updateNickname(draft)) {
                savedStateHandle[KEY_NICKNAME_DRAFT] = null
                message.value = UiMessage.NICKNAME_SAVED
            }
        }
    }

    /** 위치정보 수집 「철회」 — 다음 여행 시작 때 다시 물음(A-01) · 그 전까지 장소는 위치 없이 기록. */
    fun onWithdrawLocation() {
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.LOCATION_COLLECT, agreed = false)
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

    fun onMessageShown() {
        message.value = null
    }

    private companion object {
        const val KEY_NICKNAME_DRAFT = "settings_nickname_draft"
    }
}
