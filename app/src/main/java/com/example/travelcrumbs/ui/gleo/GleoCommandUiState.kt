package com.example.travelcrumbs.ui.gleo

import com.example.travelcrumbs.domain.model.GleoCommandState

/** 앱 틀 위에 그릴 짧은 명령 상태만 포함. */
data class GleoCommandUiState(
    val command: GleoCommandState = GleoCommandState(),
)
