package com.example.travelcrumbs.ui.placeinput

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 장소 경험 입력(A-06 · 정차) — 위에서 아래로
 * 장소명(내비 후보 → 직접 입력) · 장소 유형(카테고리 → 유형) · 좋았어요/별로예요 · 경험 태그 · 짧은 메모 · 「나중에 입력」/「저장」.
 * 저장 · 나중에 입력이 끝나면 [onClose].
 */
@Composable
fun PlaceInputScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceInputViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.prepare() }
    LaunchedEffect(uiState.closed) {
        if (uiState.closed) {
            viewModel.reset()
            onClose()
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
                text = stringResource(R.string.place_input_title, uiState.draftCount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (uiState.loadFailed) {
                Button(onClick = viewModel::onRetry) { Text(text = stringResource(R.string.retry)) }
                return@Column
            }

            // 장소명 — trip_place_dtl.user_place_nm · 내비 후보를 고르면 place_mst 로 연결
            Label(R.string.place_input_name_label)
            if (uiState.candidates.isNotEmpty()) {
                ChipRow {
                    uiState.candidates.forEach { place ->
                        FilterChip(
                            selected = uiState.selectedCandidate == place,
                            onClick = { viewModel.onSelectCandidate(place) },
                            label = { Text(text = place.name, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.height(Dimens.HomeButtonHeight),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = uiState.placeName,
                onValueChange = viewModel::onNameChange,
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.place_input_name_hint)) },
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            // 장소 유형 — 카테고리(PLACE_CATEGORY)를 고르면 그 유형(PLACE_TYPE)만
            Label(R.string.place_input_type_label)
            ChipRow {
                uiState.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category.code,
                        onClick = { viewModel.onSelectCategory(category.code) },
                        label = { Text(text = category.label, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    )
                }
            }
            if (uiState.visibleTypes.isNotEmpty()) {
                ChipRow {
                    uiState.visibleTypes.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedType == type.code,
                            onClick = { viewModel.onSelectType(type.code) },
                            label = { Text(text = type.label, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.height(Dimens.HomeButtonHeight),
                        )
                    }
                }
            }

            // 좋았어요 / 별로예요 — review_polarity_cd (REVIEW_POLARITY)
            Label(R.string.place_input_polarity_label)
            ChipRow {
                uiState.polarities.forEach { polarity ->
                    FilterChip(
                        selected = uiState.selectedPolarity == polarity.code,
                        onClick = { viewModel.onSelectPolarity(polarity.code) },
                        label = { Text(text = polarity.label, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    )
                }
            }

            // 경험 태그 — trip_place_tag_rel
            Label(R.string.place_input_tag_label)
            ChipRow {
                uiState.tags.forEach { tag ->
                    FilterChip(
                        selected = tag.tagNo in uiState.selectedTags,
                        onClick = { viewModel.onToggleTag(tag.tagNo) },
                        label = { Text(text = tag.name, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.height(Dimens.HomeButtonHeight),
                    )
                }
            }

            // 짧은 메모 — memo
            Label(R.string.place_input_memo_label)
            OutlinedTextField(
                value = uiState.memo,
                onValueChange = viewModel::onMemoChange,
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                TextButton(onClick = viewModel::onLater, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                    Text(text = stringResource(R.string.place_input_later), style = MaterialTheme.typography.titleLarge)
                }
                Button(
                    onClick = viewModel::onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier.height(Dimens.HomeButtonHeight),
                ) {
                    Text(text = stringResource(R.string.place_input_save), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    if (uiState.showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onContinueInput,
            text = { Text(text = stringResource(R.string.place_input_leave_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = viewModel::onKeepDraft) { Text(text = stringResource(R.string.place_input_keep_draft)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onContinueInput) {
                    Text(text = stringResource(R.string.place_input_continue))
                }
            },
        )
    }
}

@Composable
private fun Label(textRes: Int) {
    Text(text = stringResource(textRes), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) { content() }
}
