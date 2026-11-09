package com.example.travelcrumbs.ui.roadmap

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.ui.components.PlaceTagSheet
import com.example.travelcrumbs.ui.roadmap.components.PublishContent
import com.example.travelcrumbs.ui.roadmap.components.ReviewContent

/**
 * A-05 로드맵 검토 · A-06 저장 · 공개(와이어프레임 v6 · #51 U5) — 기록 칸 안 내용 영역(레일 그대로) · 정차 전용.
 * 저장이 끝나면 [onDone](내 여행 보관함으로). 시스템 뒤로: A-06 → A-05 → 「나중에」(검토 끝).
 */
@Composable
fun RoadmapReviewScreen(
    tripNo: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoadmapReviewViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tripNo) { viewModel.load(tripNo) }
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            viewModel.onDoneHandled()
            onDone()
        }
    }
    BackHandler(onBack = viewModel::onBack)

    when (uiState.step) {
        RoadmapStep.REVIEW -> ReviewContent(
            state = uiState,
            onEditPlace = viewModel::onEditPlace,
            onSave = viewModel::onSaveRoadmap,
            modifier = modifier,
        )
        RoadmapStep.PUBLISH -> PublishContent(
            state = uiState,
            onChoosePublic = viewModel::onChoosePublic,
            onSave = viewModel::onPublish,
            modifier = modifier,
        )
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
