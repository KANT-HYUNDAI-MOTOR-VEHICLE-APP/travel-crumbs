package com.example.travelcrumbs.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.components.TripStatusCard
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.recordingStatusText
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 주행 전용 화면(스펙 app-shell 2-5) — 상태 · 경과 · 큰 버튼 3개만.
 * 입력 칸 · 스크롤 · 애니메이션 · 레일 · 길안내 버튼 없음. 좁으면 버튼을 세로로 쌓음.
 */
@Composable
fun DrivingScreen(
    modifier: Modifier = Modifier,
    viewModel: DrivingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val stackButtons = maxWidth < Dimens.DrivingButtonsRowMinWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            // 여행 상태(지시서 14장) — 상태 점 + 상태명 + 경과
            TripStatusCard(
                statusText = recordingStatusText(uiState.recording),
                active = uiState.recording == RecordingStatus.RECORDING,
                elapsedText = elapsedText(uiState.elapsedMinutes),
            )
            Spacer(modifier = Modifier.weight(1f))
            // 큰 버튼 3개 — 「이 장소 기록」만 Primary(한 화면에 주요 행동 하나 · 지시서 4장)
            val buttons: List<DrivingAction> = listOf(
                DrivingAction(R.string.action_record_place, R.drawable.ic_place, TcButtonKind.PRIMARY, viewModel::onRecordPlace),
                DrivingAction(R.string.action_voice_input, R.drawable.ic_mic, TcButtonKind.SECONDARY, viewModel::onVoiceInput),
                DrivingAction(
                    uiState.recording.toggleLabelRes(),
                    uiState.recording.toggleIconRes(),
                    TcButtonKind.SECONDARY,
                    viewModel::onTogglePause,
                ),
            )
            if (stackButtons) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    buttons.forEach { DrivingButton(it, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    buttons.forEach { DrivingButton(it, Modifier.weight(1f)) }
                }
            }
            NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
        }
    }
}

private class DrivingAction(
    val labelRes: Int,
    val iconRes: Int,
    val kind: TcButtonKind,
    val onClick: () -> Unit,
)

@Composable
private fun DrivingButton(
    action: DrivingAction,
    modifier: Modifier = Modifier,
) {
    TcButton(
        text = stringResource(action.labelRes),
        onClick = action.onClick,
        kind = action.kind,
        iconRes = action.iconRes,
        minHeight = Dimens.DrivingButtonHeight,
        textStyle = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}

private fun RecordingStatus.toggleLabelRes(): Int = when (this) {
    RecordingStatus.RECORDING -> R.string.action_pause_recording
    RecordingStatus.PAUSED -> R.string.action_resume_recording
}

private fun RecordingStatus.toggleIconRes(): Int = when (this) {
    RecordingStatus.RECORDING -> R.drawable.ic_pause
    RecordingStatus.PAUSED -> R.drawable.ic_play
}
