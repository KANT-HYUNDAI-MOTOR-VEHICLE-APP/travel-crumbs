package com.example.travelcrumbs.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.TimelinePlace
import com.example.travelcrumbs.ui.components.CrumbRoute
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.PlaceCard
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.theme.Dimens
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 여행 상세(A-08 · 정차) — 여행명 · 「제목 수정」 · 요약(총 거리 · 소요시간 · 장소 수) · 장소 타임라인.
 * 지도 대신 점 · 선 경로 그림(지도는 나중에) · 공유는 시나리오 1 밖(결정 로그 D10). 「닫기」는 [onClose].
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
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            val detail = uiState.detail
            if (uiState.loadFailed) {
                TcButton(text = stringResource(R.string.retry), onClick = viewModel::onRetry)
            } else if (detail != null) {
                // 여행명 — trip_mst.trip_nm · 수정하면 data_change_hist 1행
                if (uiState.editingTitle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall),
                    ) {
                        OutlinedTextField(
                            value = uiState.titleInput,
                            onValueChange = viewModel::onTitleChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TcButton(
                            text = stringResource(R.string.trip_setup_cancel),
                            onClick = viewModel::onCancelTitle,
                            kind = TcButtonKind.TEXT,
                        )
                        TcButton(
                            text = stringResource(R.string.place_input_save),
                            onClick = viewModel::onSaveTitle,
                            kind = TcButtonKind.PRIMARY,
                            enabled = uiState.titleInput.isNotBlank(),
                            loading = uiState.busy,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        Text(
                            text = detail.tripName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TcButton(
                            text = stringResource(R.string.trip_detail_edit_title),
                            onClick = viewModel::onEditTitle,
                            iconRes = R.drawable.ic_edit,
                            loading = uiState.busy,
                        )
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 왼쪽 경로 그림 · 오른쪽 장소 타임라인(지시서 8장 비율)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
                ) {
                    CrumbRoute(
                        route = uiState.route,
                        emptyText = stringResource(R.string.trip_detail_route_empty),
                        modifier = Modifier
                            .weight(Dimens.MapWeight)
                            .fillMaxHeight(),
                    )
                    Column(
                        modifier = Modifier
                            .weight(Dimens.InfoWeight)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall),
                    ) {
                        // 장소 타임라인 — trip_place_dtl(기록 순서) · 장소 경험 카드(지시서 16장)
                        if (detail.places.isEmpty()) {
                            Text(
                                text = stringResource(R.string.trip_detail_no_place),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        detail.places.forEach { place -> TimelineCard(place) }
                    }
                }
            }
        }
        Row(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
            TcButton(
                text = stringResource(R.string.trip_detail_close),
                onClick = onClose,
                iconRes = R.drawable.ic_close,
            )
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}

@Composable
private fun TimelineCard(place: TimelinePlace) {
    val time = runCatching {
        OffsetDateTime.parse(place.recordedAt).atZoneSameInstant(ZoneId.systemDefault()).format(TIME)
    }.getOrDefault("")
    val details = listOfNotNull(
        place.placeTypeName,
        when (place.reviewPolarityCode) {
            "POSITIVE" -> stringResource(R.string.trip_detail_positive)
            "NEGATIVE" -> stringResource(R.string.trip_detail_negative)
            else -> null
        },
    ).joinToString(" · ")
    PlaceCard(
        time = time,
        name = place.userPlaceName ?: stringResource(R.string.trip_detail_draft_place),
        details = details,
        tags = place.tagNames,
        memo = place.memo,
        isDraft = place.isDraft,
    )
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
