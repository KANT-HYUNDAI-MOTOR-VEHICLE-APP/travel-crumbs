package com.example.travelcrumbs.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TripCard
import com.example.travelcrumbs.ui.components.tripSummaryText
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.tripdetail.TripDetailScreen

/**
 * 여행(레일 「여행」 · A-07 간단판) — 내 여행 목록(여행명 · 날짜 · 거리 · 장소 수).
 * 누르면 여행 상세(A-08)를 이 자리에 엶.
 */
@Composable
fun TripsScreen(
    modifier: Modifier = Modifier,
    viewModel: TripsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    val selected = uiState.selectedTripUuid
    if (selected != null) {
        TripDetailScreen(tripUuid = selected, onClose = viewModel::onDetailClosed, modifier = modifier)
        return
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
                text = stringResource(R.string.trips_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            when {
                uiState.loadFailed -> TcButton(text = stringResource(R.string.retry), onClick = viewModel::load)
                !uiState.loading && uiState.trips.isEmpty() ->
                    Text(
                        text = stringResource(R.string.trips_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                // 여행 카드(지시서 15장) — 카드 전체가 한 터치 영역
                else -> uiState.trips.forEach { trip ->
                    TripCard(
                        title = trip.tripName,
                        summary = tripSummaryText(trip),
                        onClick = { viewModel.onSelect(trip.tripUuid) },
                    )
                }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}
