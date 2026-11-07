package com.example.travelcrumbs.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.TimelinePlace
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.theme.Dimens
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 여행 상세(A-08 · 정차) — 여행명 · 「제목 수정」 · 요약(총 거리 · 소요시간 · 장소 수) · 장소 타임라인.
 * 지도 · 공유는 시나리오 1 밖(공유 제외 · 결정 로그 D10). 「닫기」는 [onClose].
 */
@Composable
fun TripDetailScreen(
    tripUuid: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(tripUuid) { viewModel.load(tripUuid) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            val detail = uiState.detail
            if (uiState.loadFailed) {
                Button(onClick = viewModel::onRetry) { Text(text = stringResource(R.string.retry)) }
            } else if (detail != null) {
                // 여행명 — trip_mst.trip_nm · 수정하면 data_change_hist 1행
                if (uiState.editingTitle) {
                    OutlinedTextField(
                        value = uiState.titleInput,
                        onValueChange = viewModel::onTitleChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        TextButton(onClick = viewModel::onCancelTitle) {
                            Text(text = stringResource(R.string.trip_setup_cancel))
                        }
                        Button(
                            onClick = viewModel::onSaveTitle,
                            enabled = !uiState.busy && uiState.titleInput.isNotBlank(),
                        ) { Text(text = stringResource(R.string.place_input_save)) }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        Text(
                            text = detail.tripName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = viewModel::onEditTitle, enabled = !uiState.busy) {
                            Text(text = stringResource(R.string.trip_detail_edit_title))
                        }
                    }
                }

                // 요약 — trip_mst.dist · duration · place_cnt (종료 때 계산)
                Text(
                    text = stringResource(
                        R.string.trip_detail_summary,
                        (detail.distanceMeters ?: 0) / 1000.0,
                        (detail.durationSeconds ?: 0) / 3600,
                        (detail.durationSeconds ?: 0) % 3600 / 60,
                        detail.placeCount,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )

                // 장소 타임라인 — trip_place_dtl(기록 순서)
                if (detail.places.isEmpty()) {
                    Text(text = stringResource(R.string.trip_detail_no_place), style = MaterialTheme.typography.bodyLarge)
                }
                detail.places.forEach { place -> TimelineRow(place) }
            }
        }
        Row(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
            Button(onClick = onClose, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.trip_detail_close), style = MaterialTheme.typography.titleLarge)
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}

@Composable
private fun TimelineRow(place: TimelinePlace) {
    val time = runCatching {
        OffsetDateTime.parse(place.recordedAt).atZoneSameInstant(ZoneId.systemDefault()).format(TIME)
    }.getOrDefault("")
    val name = place.userPlaceName ?: stringResource(R.string.trip_detail_draft_place)
    val details = listOfNotNull(
        place.placeTypeName,
        when (place.reviewPolarityCode) {
            "POSITIVE" -> stringResource(R.string.trip_detail_positive)
            "NEGATIVE" -> stringResource(R.string.trip_detail_negative)
            else -> null
        },
        place.tagNames.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        place.memo,
    ).joinToString(" · ")
    Column {
        Text(text = "$time  $name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (details.isNotEmpty()) {
            Text(text = details, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
