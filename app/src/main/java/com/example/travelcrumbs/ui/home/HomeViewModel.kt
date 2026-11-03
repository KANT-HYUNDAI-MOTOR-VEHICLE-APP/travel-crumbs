package com.example.travelcrumbs.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.llm.LlmRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class HomeViewModel @Inject constructor(
    drivingSignalRepository: DrivingSignalRepository,
    private val naviRepository: NaviRepository,
    private val llmRepository: LlmRepository,
) : ViewModel() {

    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)
    private val llmState = MutableStateFlow<SdkStatus<String>>(SdkStatus.Idle)

    val uiState: StateFlow<HomeUiState> = combine(
        drivingSignalRepository.drivingState,
        naviRepository.ready,
        naviState,
        llmState,
    ) { driving, naviReady, navi, llm ->
        HomeUiState(
            drivingState = driving,
            naviReady = naviReady,
            navi = navi,
            llm = llm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(drivingState = DrivingState.UNKNOWN),
    )

    init {
        refreshNavi()
    }

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

    fun askLlm() {
        if (llmState.value is SdkStatus.Loading) return
        llmState.value = SdkStatus.Loading
        viewModelScope.launch {
            llmState.value = runCatching {
                withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) {
                    llmRepository.ask(AppConfig.SMOKE_PROMPT)
                }
            }.fold(
                onSuccess = { SdkStatus.Ready(it) },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    private fun Throwable.toCode(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
