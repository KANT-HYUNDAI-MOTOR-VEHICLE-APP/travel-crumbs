package com.example.travelcrumbs.ui.gleo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.domain.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** AppShellScreen 화면 수준에서 구독. 기존 앱 틀·CRUD ViewModel의 원본 상태를 변경하지 않음. */
@HiltViewModel
class GleoCommandViewModel @Inject constructor(
    private val repository: GleoActionRepository,
) : ViewModel() {
    val uiState: StateFlow<GleoCommandUiState> = repository.commandState
        .map { GleoCommandUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
            initialValue = GleoCommandUiState(),
        )

    /** 표시 소비는 저장·콜백을 다시 실행하지 않음. */
    fun onNoticeShown(noticeId: Long) = repository.clearCommandNotice(noticeId)
}
