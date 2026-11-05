package com.example.travelcrumbs.ui.driving

import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.ui.components.UiMessage

/** 주행 전용 화면이 그릴 것(스펙 app-shell 3-3). */
data class DrivingUiState(
    val recording: RecordingStatus = RecordingStatus.RECORDING,
    val elapsedMinutes: Long = 0L,
    val message: UiMessage? = null,
)
