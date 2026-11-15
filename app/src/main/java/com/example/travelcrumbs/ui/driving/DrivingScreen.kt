package com.example.travelcrumbs.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.DangerButton
import com.example.travelcrumbs.ui.components.ActionTile
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.components.ScreenHeader
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.ui.driving.components.EndTripConfirmDialog
import com.example.travelcrumbs.ui.driving.components.NaviGuideCard
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 주행 전용 화면(스펙 app-shell-v5 2-5 · 와이어프레임 v6 A-02 · #51 U0) — 레일 오른쪽 내용 영역에 그림(D29).
 * 1 여행명 · 상태 · 경과 → 2 기록된 장소 수 → 3 (넓은 칸만) 「내비 안내」 카드 → 4 버튼 4개(D31) → 한 줄 안내.
 * 가짜 지도 자리는 없음 — 실제 차량 내비가 옆 칸(나눔) 또는 앞(나누지 않음)에 뜸. 좁은 칸(나눈 칸 · 내비가 옆에 있음)은 카드 없이 버튼만.
 * 입력 칸 · 스크롤 · 애니메이션 · 「새 여행 시작」 없음. 버튼은 한 줄 4개 · 좁으면 2 × 2(순서 그대로).
 */
@Composable
fun DrivingScreen(
    modifier: Modifier = Modifier,
    viewModel: DrivingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { viewModel.onScreenPaused() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= Dimens.DrivingWideMinWidth
        val compactHeight = maxHeight < 900.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd, top = Dimens.ScreenTop),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            ScreenHeader(title = stringResource(R.string.nav_records), parked = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(if (compactHeight) Dimens.ItemSpacing else Dimens.ScreenHeaderToBody - Dimens.ItemSpacing))
            if (!compactHeight) {
                Text(
                    text = stringResource(R.string.driving_focus_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = tripNameText(uiState.tripName),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tripStatusText(uiState.tripStatus),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                        Text(
                            text = elapsedText(uiState.elapsedMinutes),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.trip_place_count, uiState.placeCount),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (wide && uiState.naviDestinationName != null) {
                NaviGuideCard(
                    destinationName = uiState.naviDestinationName,
                    naviAppAvailable = uiState.naviAppAvailable,
                    onOpenNavi = viewModel::onOpenNavi,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.DrivingNaviCompactHeight),
                )
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.DrivingButtonSpacing),
            ) {
                ActionTile(
                    title = stringResource(R.string.action_record_place),
                    subtitle = null,
                    iconRes = R.drawable.ic_place,
                    emphasized = true,
                    enabled = uiState.recordingActionsEnabled,
                    onClick = viewModel::onRecordPlace,
                    modifier = Modifier.weight(1.7f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.DrivingButtonSpacing),
                ) {
                    DrivingButton(DrivingButtonSpec(R.string.action_voice, true, viewModel::onVoice), Modifier.weight(1f))
                    DrivingButton(
                        DrivingButtonSpec(
                            if (uiState.pauseButtonShowsResume) R.string.action_resume_recording else R.string.action_pause_recording,
                            uiState.pauseButtonEnabled,
                            viewModel::onTogglePause,
                        ),
                        Modifier.weight(1f),
                    )
                    DrivingButton(
                        DrivingButtonSpec(R.string.action_end_trip, uiState.recordingActionsEnabled, viewModel::onEndTrip, DrivingButtonKind.DANGER),
                        Modifier.weight(1f),
                    )
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

/** 버튼 모양(디자인 시스템 12절) — 주요 행동 「이 장소 기록」 하나만 Primary · 「여행 종료」는 Danger · 나머지 Secondary. */
private enum class DrivingButtonKind { PRIMARY, SECONDARY, DANGER }

private data class DrivingButtonSpec(
    val labelRes: Int,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val kind: DrivingButtonKind = DrivingButtonKind.SECONDARY,
)

@Composable
private fun DrivingButton(
    spec: DrivingButtonSpec,
    modifier: Modifier = Modifier,
) {
    val label: @Composable RowScope.() -> Unit = {
        Text(
            text = stringResource(spec.labelRes),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val sized = modifier.fillMaxWidth().heightIn(min = Dimens.DrivingButtonHeight)
    when (spec.kind) {
        DrivingButtonKind.PRIMARY -> PrimaryButton(onClick = spec.onClick, modifier = sized, enabled = spec.enabled, content = label)
        DrivingButtonKind.SECONDARY -> SecondaryButton(onClick = spec.onClick, modifier = sized, enabled = spec.enabled, content = label)
        DrivingButtonKind.DANGER -> DangerButton(onClick = spec.onClick, modifier = sized, enabled = spec.enabled, content = label)
    }
}
