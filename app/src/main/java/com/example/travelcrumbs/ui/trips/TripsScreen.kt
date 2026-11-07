package com.example.travelcrumbs.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.example.travelcrumbs.domain.model.TripSummary
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.tripdetail.TripDetailScreen
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                fontWeight = FontWeight.Bold,
            )
            when {
                uiState.loadFailed -> Button(onClick = viewModel::load) { Text(text = stringResource(R.string.retry)) }
                !uiState.loading && uiState.trips.isEmpty() ->
                    Text(text = stringResource(R.string.trips_empty), style = MaterialTheme.typography.bodyLarge)
                else -> uiState.trips.forEach { trip -> TripRow(trip, onClick = { viewModel.onSelect(trip.tripUuid) }) }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }
}

@Composable
private fun TripRow(trip: TripSummary, onClick: () -> Unit) {
    val date = runCatching {
        OffsetDateTime.parse(trip.startAt).atZoneSameInstant(ZoneId.systemDefault()).format(DATE)
    }.getOrDefault("")
    val summary = if (trip.statusCode == RECORDING) {
        stringResource(R.string.trips_recording, date, trip.placeCount)
    } else {
        stringResource(R.string.trips_summary, date, (trip.distanceMeters ?: 0) / 1000.0, trip.placeCount)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Dimens.ItemSpacing)) {
            Text(text = trip.tripName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private const val RECORDING = "RECORDING"
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
