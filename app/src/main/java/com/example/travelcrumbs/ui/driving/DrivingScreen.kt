package com.example.travelcrumbs.ui.driving

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.ui.driving.components.EndTripConfirmDialog
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 주행 전용 화면(스펙 app-shell-v5 2-5) — 레일 오른쪽 내용 영역에 그림(D29).
 * 1 여행명 · 상태 · 경과 → 2 기록된 장소 수 → 3 지도 자리(남는 공간 · 창이 작으면 높이 0 까지 줄어듦) → 4 버튼 3개.
 * 입력 칸 · 스크롤 · 애니메이션 · 길안내 버튼 · 「새 여행 시작」 없음. 좁으면 버튼을 세로로 쌓음.
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
                text = tripNameText(uiState.tripName),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = tripStatusText(uiState.tripStatus),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = elapsedText(uiState.elapsedMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.trip_place_count, uiState.placeCount),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            MapPlaceholder(modifier = Modifier.weight(1f))
            val buttons = listOf(
                DrivingButtonSpec(R.string.action_record_place, uiState.recordingActionsEnabled, viewModel::onRecordPlace),
                DrivingButtonSpec(R.string.action_voice, enabled = true, onClick = viewModel::onVoice),
                DrivingButtonSpec(R.string.action_end_trip, uiState.recordingActionsEnabled, viewModel::onEndTrip),
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

    if (uiState.endConfirmVisible) {
        EndTripConfirmDialog(
            onContinue = viewModel::onContinueRecording,
            onEnd = viewModel::onConfirmEndTrip,
        )
    }
}

/** 지도 자리(N15) — 테두리 영역 + 한 줄 「지도 준비 중」 · 누를 수 없음 · 움직임 없음. */
@Composable
private fun MapPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .border(Dimens.MapPlaceholderBorder, MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.map_placeholder),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private data class DrivingButtonSpec(
    val labelRes: Int,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun DrivingButton(
    spec: DrivingButtonSpec,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = spec.onClick,
        enabled = spec.enabled,
        modifier = modifier.height(Dimens.DrivingButtonHeight),
    ) {
        Text(
            text = stringResource(spec.labelRes),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
