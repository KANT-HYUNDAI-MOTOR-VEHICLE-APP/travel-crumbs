package com.example.travelcrumbs.ui.tripsetup

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.FieldLabel
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.components.TcChip
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 여행 시작 설정(A-04) — 여행명(날짜 기본값 · 「수정」) · 동행자 복수 선택 · 「취소」 / 「여행 시작」.
 * 「취소」는 아무것도 저장하지 않고 홈으로. 「여행 시작」이 저장되면 [onStarted].
 */
@Composable
fun TripSetupScreen(
    onCancel: () -> Unit,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripSetupViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.prepare() }
    LaunchedEffect(uiState.startedTripName) {
        if (uiState.startedTripName != null) {
            viewModel.reset()
            onStarted()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Text(
                text = stringResource(R.string.trip_setup_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // 여행명 — trip_mst.trip_nm
            FieldLabel(text = stringResource(R.string.trip_setup_name_label))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                if (uiState.editingName) {
                    OutlinedTextField(
                        value = uiState.tripName,
                        onValueChange = viewModel::onNameChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = uiState.tripName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    TcButton(
                        text = stringResource(R.string.trip_setup_name_edit),
                        onClick = viewModel::onEditName,
                        iconRes = R.drawable.ic_edit,
                        loading = uiState.suggesting,
                    )
                }
            }
            Text(
                text = stringResource(R.string.trip_setup_name_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 동행자 — trip_companion_dtl.companion_type_cd (버튼 글자 = cd_dtl.cd_nm)
            FieldLabel(text = stringResource(R.string.trip_setup_companion_label))
            if (uiState.loadFailed) {
                TcButton(text = stringResource(R.string.retry), onClick = viewModel::onRetry)
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    uiState.companions.forEach { companion ->
                        TcChip(
                            label = companion.label,
                            selected = companion.code in uiState.selectedCompanions,
                            onClick = { viewModel.onToggleCompanion(companion.code) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.ItemSpacing))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                TcButton(
                    text = stringResource(R.string.trip_setup_cancel),
                    onClick = {
                        viewModel.reset()
                        onCancel()
                    },
                )
                // 이 화면의 주요 행동 — 여행 기록 시작
                TcButton(
                    text = stringResource(R.string.trip_setup_start),
                    onClick = viewModel::onStart,
                    kind = TcButtonKind.PRIMARY,
                    iconRes = R.drawable.ic_record,
                    enabled = uiState.canStart || uiState.starting,
                    loading = uiState.starting,
                )
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}
