package com.example.travelcrumbs.ui.tripstart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.components.companionLabelRes
import com.example.travelcrumbs.ui.recommendations.components.RecoChip
import com.example.travelcrumbs.ui.tripstart.components.TripStartLabel
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * A-01 여행 시작(와이어프레임 v6 · #51 U3) — 기록 칸 안에서 C-02 「여행 시작」으로 엶 · 정차 전용(주행이 되면 앱 틀이 주행 화면으로).
 * 위에서 아래로 제목 · 여행 이름 · 동행자 칩 · 목적지(선택) · 위치 동의(처음 한 번) · 내비 안내 한 줄 · 「기록 시작」.
 * 시작이 끝나면 [onDone](안내 한 줄과 함께) · 시스템 뒤로는 [onCancel].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripStartScreen(
    onDone: (UiMessage?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripStartViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onCancel)
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            val message = uiState.message
            viewModel.onDoneHandled()
            onDone(message)
        }
    }

    val defaultName = stringResource(R.string.trip_name_default_date, uiState.defaultMonth, uiState.defaultDay)
    Column(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Text(
                text = stringResource(R.string.trip_start_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            TripStartLabel(R.string.trip_start_name_label)
            OutlinedTextField(
                value = uiState.tripNm,
                onValueChange = viewModel::onNameChange,
                placeholder = { Text(text = defaultName, style = MaterialTheme.typography.titleLarge) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            TripStartLabel(R.string.trip_start_companion_label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                CompanionType.entries.forEach { type ->
                    RecoChip(
                        label = stringResource(companionLabelRes(type)),
                        selected = type in uiState.companions,
                        onClick = { viewModel.onToggleCompanion(type) },
                    )
                }
            }
            TripStartLabel(R.string.trip_start_dest_label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                RecoChip(
                    label = stringResource(R.string.trip_start_dest_none),
                    selected = uiState.destinationId == null,
                    onClick = { viewModel.onSelectDestination(null) },
                )
                uiState.destinations.forEach { place ->
                    RecoChip(
                        label = place.name,
                        selected = uiState.destinationId == place.id,
                        onClick = { viewModel.onSelectDestination(place.id) },
                    )
                }
            }
            if (uiState.consentNeeded) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.trip_start_consent_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.trip_start_consent_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = CONSENT_BODY_MAX_LINES,
                    )
                }
            }
            Text(
                text = stringResource(R.string.trip_start_navi_hint),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        PrimaryButton(
            onClick = { viewModel.onStart(defaultName) },
            enabled = !uiState.starting,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.ItemSpacing).height(Dimens.HomeButtonHeight),
        ) {
            Text(
                text = stringResource(if (uiState.consentNeeded) R.string.action_consent_and_start else R.string.action_start_recording),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** 동의 카드 본문 줄 수 — 수집 항목 · 목적 · 보유 기간 · 제3자 제공(#51 리뷰 P3). */
private const val CONSENT_BODY_MAX_LINES = 4
