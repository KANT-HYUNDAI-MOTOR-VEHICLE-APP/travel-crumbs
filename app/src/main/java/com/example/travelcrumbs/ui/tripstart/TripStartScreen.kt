package com.example.travelcrumbs.ui.tripstart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.AppTextField
import com.example.travelcrumbs.ui.components.ActionTile
import com.example.travelcrumbs.ui.components.BackButton
import com.example.travelcrumbs.ui.components.NeutralFilledButton
import com.example.travelcrumbs.ui.components.NeutralOutlineButton
import com.example.travelcrumbs.ui.components.ScreenHeader
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.components.companionLabelRes
import com.example.travelcrumbs.ui.recommendations.components.RecoChip
import com.example.travelcrumbs.ui.tripstart.components.TripStartLabel
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * A-01 여행 시작(와이어프레임 v6 · #51 U3) — 기록 칸 안에서 C-02 「여행 시작」으로 엶 · 정차 전용(주행이 되면 앱 틀이 주행 화면으로).
 * 제목 아래에서 정보 입력 영역과 큰 행동 영역을 나누어 여행 이름 · 동행자 · 목적지 · 위치 동의 · 「기록 시작」을 표시한다.
 * 시작이 끝나면 [onDone](안내 한 줄과 함께) · 시스템 뒤로 · 머리글 「뒤로」는 같은 [onCancel](입력 버림 · 확인 없음).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripStartScreen(
    onDone: (UiMessage?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripStartViewModel = viewModel(),
    inspectionState: TripStartUiState? = null,
) {
    val collectedState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiState = inspectionState ?: collectedState
    val focusManager = LocalFocusManager.current
    var nameFocusedOnce by remember { mutableStateOf(false) }

    BackHandler(onBack = onCancel)
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            val message = uiState.message
            viewModel.onDoneHandled()
            onDone(message)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.nav_records),
            parked = true,
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd, top = Dimens.ScreenTop),
        )
        Spacer(Modifier.height(Dimens.ScreenHeaderToBody))
        if (uiState.consentStepVisible) {
            ConsentStep(
                starting = uiState.starting,
                onDecline = viewModel::onDeclineConsent,
                onConfirm = viewModel::onConfirmConsent,
                modifier = Modifier.padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd),
            )
        } else {
            Text(
                text = stringResource(R.string.trip_start_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd),
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(690.dp).padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Column(
                    modifier = Modifier.weight(640f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    Text(text = stringResource(R.string.trip_start_name_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppTextField(
                        value = uiState.tripNm,
                        onValueChange = viewModel::onNameChange,
                        placeholder = stringResource(R.string.trip_name_placeholder),
                        isError = uiState.tripNameError,
                        errorText = if (uiState.tripNameError) stringResource(R.string.input_trip_name_error) else null,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                            if (focusState.isFocused) nameFocusedOnce = true else if (nameFocusedOnce) viewModel.onNameFocusLost()
                        },
                    )
                    Text(text = stringResource(R.string.trip_start_companion_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        CompanionType.entries.forEach { type ->
                            RecoChip(
                                label = stringResource(companionLabelRes(type)),
                                selected = type in uiState.companions,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.onToggleCompanion(type)
                                },
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(668f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    RecoChip(
                        label = stringResource(R.string.trip_start_dest_none),
                        selected = uiState.destinationId == null,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onSelectDestination(null)
                        },
                    )
                    uiState.destinations.take(2).forEach { place ->
                        DestinationChoice(
                            item = place,
                            selected = uiState.destinationId == place.id,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.onSelectDestination(place.id)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            BottomActions(
                cancelLabel = stringResource(R.string.action_cancel_work),
                confirmLabel = stringResource(R.string.action_start_recording),
                confirmEnabled = uiState.canStart,
                onCancel = onCancel,
                onConfirm = viewModel::onStart,
                modifier = Modifier.padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd),
            )
        }
    }
}

@Composable
private fun ConsentStep(starting: Boolean, onDecline: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.trip_start_consent_page_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.trip_start_consent_page_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        AppCard(modifier = Modifier.fillMaxWidth().height(690.dp)) {
            Text(stringResource(R.string.trip_start_consent_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ConsentRow(R.string.trip_start_consent_collect, R.string.trip_start_consent_collect_value)
            ConsentRow(R.string.trip_start_consent_purpose, R.string.trip_start_consent_purpose_value)
            ConsentRow(R.string.trip_start_consent_period, R.string.trip_start_consent_period_value)
            ConsentRow(R.string.trip_start_consent_storage, R.string.trip_start_consent_storage_value)
            ConsentRow(R.string.trip_start_consent_third_party, R.string.trip_start_consent_third_party_value)
            Text(stringResource(R.string.trip_start_consent_withdraw), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        BottomActions(
            cancelLabel = stringResource(R.string.trip_start_consent_decline),
            confirmLabel = stringResource(R.string.trip_start_consent_confirm),
            confirmEnabled = !starting,
            onCancel = onDecline,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ConsentRow(labelRes: Int, valueRes: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(200f))
        Text(stringResource(valueRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(866f))
    }
}

@Composable
private fun BottomActions(
    cancelLabel: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().height(Dimens.PrimaryButtonHeight), horizontalArrangement = Arrangement.spacedBy(30.dp)) {
        NeutralOutlineButton(onClick = onCancel, modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(cancelLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        NeutralFilledButton(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(confirmLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun DestinationChoice(item: DestinationItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.secondaryContainer else colors.surface,
        border = BorderStroke(
            if (selected) Dimens.SelectedBorderWidth else Dimens.ButtonBorderWidth,
            if (selected) colors.secondary else colors.outline,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) Text("✓", style = MaterialTheme.typography.bodyLarge, color = colors.secondary, maxLines = 1)
            }
            Text(
                text = item.address,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
