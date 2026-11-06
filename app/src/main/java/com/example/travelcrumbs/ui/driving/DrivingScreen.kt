package com.example.travelcrumbs.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.ui.components.NoticeLine
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
            Text(
                text = recordingStatusText(uiState.recording),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = elapsedText(uiState.elapsedMinutes),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            val buttons: List<Pair<Int, () -> Unit>> = listOf(
                R.string.action_record_place to viewModel::onRecordPlace,
                R.string.action_voice_input to viewModel::onVoiceInput,
                uiState.recording.toggleLabelRes() to viewModel::onTogglePause,
            )
            if (stackButtons) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    buttons.forEach { (label, onClick) ->
                        DrivingButton(label, onClick, Modifier.fillMaxWidth())
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    buttons.forEach { (label, onClick) ->
                        DrivingButton(label, onClick, Modifier.weight(1f))
                    }
                }
            }
            NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
        }
    }
}

@Composable
private fun DrivingButton(
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(Dimens.DrivingButtonHeight),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun RecordingStatus.toggleLabelRes(): Int = when (this) {
    RecordingStatus.RECORDING -> R.string.action_pause_recording
    RecordingStatus.PAUSED -> R.string.action_resume_recording
}
