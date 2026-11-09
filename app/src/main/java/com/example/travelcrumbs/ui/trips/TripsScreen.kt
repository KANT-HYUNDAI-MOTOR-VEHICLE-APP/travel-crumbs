package com.example.travelcrumbs.ui.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.components.TripSummaryCard
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.tripdetail.TripDetailScreen
import com.example.travelcrumbs.ui.trips.components.RecoConsentDialog

/**
 * 「내 여행」 칸(와이어프레임 v6 A-07 보관함 · #51 U6) — 끝난 여행 카드 · 공개 토글 · 카드 → A-08 상세(이 칸 안).
 * [entry] · [openTripNo] — 앱 틀이 레일 「내 여행」 · 여행 카드(C-02 · A-06 저장 뒤)마다 올리는 값 · 열 여행.
 * 시스템 뒤로: A-08 → 목록 · 목록에선 앱 틀이 받음(「기록」으로).
 */
@Composable
fun TripsScreen(
    entry: Int,
    openTripNo: Long?,
    modifier: Modifier = Modifier,
    viewModel: TripsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(entry) { viewModel.onEntry(entry, openTripNo) }

    val selected = uiState.selectedTripNo
    if (selected != null) {
        BackHandler(onBack = viewModel::onCloseTrip)
        TripDetailScreen(tripNo = selected, modifier = modifier)
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
                text = stringResource(R.string.nav_trips),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.trips_sub),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (uiState.trips.isEmpty()) {
                Text(
                    text = stringResource(R.string.trips_empty),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            uiState.trips.forEach { item ->
                val toggleLabel = stringResource(R.string.trips_public_toggle, item.tripNm.orEmpty())
                TripSummaryCard(item = item, onClick = { viewModel.onOpenTrip(item.tripNo) }) {
                    Switch(
                        checked = item.public,
                        onCheckedChange = { viewModel.onTogglePublic(item.tripNo, it) },
                        modifier = Modifier.semantics { contentDescription = toggleLabel },
                    )
                }
            }
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    if (uiState.consentAskTripNo != null) {
        RecoConsentDialog(onAgree = viewModel::onConsentAndPublish, onDismiss = viewModel::onDismissConsent)
    }
}
