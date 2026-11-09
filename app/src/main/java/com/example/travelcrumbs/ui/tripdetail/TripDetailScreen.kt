package com.example.travelcrumbs.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.ConfirmDialog
import com.example.travelcrumbs.ui.components.DangerButton
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.PlaceTagSheet
import com.example.travelcrumbs.ui.components.RoadmapPlaceCard
import com.example.travelcrumbs.ui.components.RouteSketchBox
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.components.durationText
import com.example.travelcrumbs.ui.components.tripMetaText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.trips.components.RecoConsentDialog
import com.example.travelcrumbs.ui.trips.components.RenameTripDialog

/**
 * A-08 내 로드맵 상세(와이어프레임 v6 · #51 U6) — 「내 여행」 칸 안(A-07 카드로 엶 · 뒤로는 목록).
 * 제목 · 「이름 수정」 · 날짜 · 장소 수 · 동행자 · 기록 시간 · 받은 별점 · 공개 토글 · 간소화 경로 · 장소(누르면 A-03 시트로 고침).
 */
@Composable
fun TripDetailScreen(
    tripNo: Long,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(tripNo) { viewModel.load(tripNo) }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            viewModel.onDeletedHandled()
            onDeleted()
        }
    }
    val summary = uiState.summary

    Column(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tripNameText(summary?.tripNm),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(onClick = viewModel::onEditName, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.trip_rename_action), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            DangerButton(onClick = viewModel::onAskDelete, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.trip_delete_action), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        if (summary != null) {
            Text(
                text = tripMetaText(summary) + stringResource(R.string.reco_meta_separator) + durationText(uiState.durationMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val toggleLabel = stringResource(R.string.trips_public_toggle, summary.tripNm.orEmpty())
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.ratingCnt > 0) {
                        stringResource(R.string.trip_detail_rating, uiState.ratingAvg, uiState.ratingCnt)
                    } else {
                        stringResource(R.string.trip_detail_rating_none)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(if (summary.public) R.string.trip_public_badge else R.string.trip_private_badge),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Switch(
                    checked = summary.public,
                    onCheckedChange = viewModel::onTogglePublic,
                    modifier = Modifier.semantics { contentDescription = toggleLabel },
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= Dimens.RoadmapWideMinWidth) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    RouteSketchBox(pins = uiState.sketch, modifier = Modifier.weight(Dimens.DetailRouteWeight).fillMaxHeight())
                    Column(
                        modifier = Modifier.weight(1f - Dimens.DetailRouteWeight).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        uiState.places.forEach { place -> RoadmapPlaceCard(item = place, onEdit = { viewModel.onEditPlace(place.tripPlaceNo) }) }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    RouteSketchBox(pins = uiState.sketch, modifier = Modifier.fillMaxWidth().height(Dimens.RoadmapSketchHeight))
                    uiState.places.forEach { place -> RoadmapPlaceCard(item = place, onEdit = { viewModel.onEditPlace(place.tripPlaceNo) }) }
                }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    uiState.renameDraft?.let { draft ->
        RenameTripDialog(value = draft, onValueChange = viewModel::onRenameChange, onDismiss = viewModel::onDismissRename, onSave = viewModel::onSaveRename)
    }
    if (uiState.deleteAsk) {
        ConfirmDialog(
            titleRes = R.string.trip_delete_title,
            bodyRes = R.string.trip_delete_body,
            confirmRes = R.string.action_delete,
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDelete,
        )
    }
    if (uiState.consentAsk) {
        RecoConsentDialog(onAgree = viewModel::onConsentAndPublish, onDismiss = viewModel::onDismissConsent)
    }
    uiState.tagSheet?.let { sheet ->
        PlaceTagSheet(
            item = sheet,
            onNameChange = viewModel::onSheetNameChange,
            onMemoChange = viewModel::onSheetMemoChange,
            onToggleTag = viewModel::onSheetToggleTag,
            onSkip = viewModel::onSheetSkip,
            onSave = viewModel::onSheetSave,
        )
    }
}
