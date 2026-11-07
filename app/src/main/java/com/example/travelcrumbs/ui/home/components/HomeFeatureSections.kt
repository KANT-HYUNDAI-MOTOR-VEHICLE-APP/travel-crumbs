package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.TripSummary
import com.example.travelcrumbs.ui.components.SectionTitle
import com.example.travelcrumbs.ui.components.TcButton
import com.example.travelcrumbs.ui.components.TcButtonKind
import com.example.travelcrumbs.ui.components.TripCard
import com.example.travelcrumbs.ui.components.tripSummaryText
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 홈 기능 구역 자리(스펙 app-shell 2-2 · D17) — 맨 위 영역(기록 상태 · 「이 장소 기록」) 아래 · debug 카드 위.
 * 다른 기능은 여기에 자기 구역 호출 한 줄씩을 차례로 더함(예: #4 「가 볼 장소」).
 * 구역은 맨 위 영역을 밀어내거나 가리지 않음 — 홈 Column 의 맨 위 영역 다음에만 놓임.
 *
 * 지금 구역: 「새 여행 시작」(A-03 · 기록 중인 여행이 없을 때 · 이 화면의 Primary) · 최근 여행 카드(지시서 23장).
 */
@Composable
fun ColumnScope.HomeFeatureSections(
    onStartNewTrip: () -> Unit = {},
    showStartTrip: Boolean = true,
    recentTrips: List<TripSummary> = emptyList(),
    onOpenTrip: (String) -> Unit = {},
) {
    if (showStartTrip) {
        TcButton(
            text = stringResource(R.string.home_start_trip),
            onClick = onStartNewTrip,
            kind = TcButtonKind.PRIMARY,
            iconRes = R.drawable.ic_record,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (recentTrips.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Dimens.ItemSpacingSmall))
        SectionTitle(text = stringResource(R.string.home_recent_title))
        recentTrips.forEach { trip ->
            TripCard(
                title = trip.tripName,
                summary = tripSummaryText(trip),
                onClick = { onOpenTrip(trip.tripUuid) },
            )
        }
    }
}
