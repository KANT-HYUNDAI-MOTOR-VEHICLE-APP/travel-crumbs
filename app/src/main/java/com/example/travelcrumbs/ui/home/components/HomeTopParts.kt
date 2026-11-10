package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.DangerButton
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.TripSummaryCard
import com.example.travelcrumbs.ui.components.TripSummaryItem
import com.example.travelcrumbs.ui.components.VisibilityBadge
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * C-02 진행 중 여행이 없을 때(와이어프레임 v6 · #51 U2) — 인사 · 「여행 시작」(목적지 있음 · 주요) · 「추천 보러 가기」(목적지 없음) 나란히 ·
 * 최근 여행 1개. 넓으면 두 카드 한 줄 · 좁은 칸(나눈 칸)이면 위아래로. 고르는 건 사용자(앱이 판단하지 않음).
 */
@Composable
fun ColumnScope.NoTripTop(
    nicknameNm: String,
    recentTrip: TripSummaryItem?,
    onStartTrip: () -> Unit,
    onOpenRecommendations: () -> Unit,
    onOpenTrip: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
        Text(
            text = stringResource(R.string.home_greeting, nicknameNm),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.home_greeting_sub),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < Dimens.HomeChoiceRowMinWidth
        val start: @Composable (Modifier) -> Unit = { m ->
            ChoiceCard(R.string.action_start_trip, R.string.home_start_trip_sub, primary = true, onClick = onStartTrip, modifier = m)
        }
        val reco: @Composable (Modifier) -> Unit = { m ->
            ChoiceCard(R.string.action_open_recommendations, R.string.home_open_reco_sub, primary = false, onClick = onOpenRecommendations, modifier = m)
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                start(Modifier.fillMaxWidth())
                reco(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                start(Modifier.weight(1f))
                reco(Modifier.weight(1f))
            }
        }
    }
    Text(
        text = stringResource(R.string.home_recent_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
    if (recentTrip == null) {
        Text(
            text = stringResource(R.string.recent_trip_empty),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    } else {
        TripSummaryCard(item = recentTrip, onClick = { onOpenTrip(recentTrip.tripNo) }) {
            if (recentTrip.public) VisibilityBadge(public = true)
        }
    }
}

/** 큰 선택 카드 하나 — 제목 + 한 줄 설명 · 카드 전체가 버튼(한 번 터치). */
@Composable
private fun ChoiceCard(
    titleRes: Int,
    subRes: Int,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val content = if (primary) colors.onPrimary else colors.onSurface
    Column(
        modifier = modifier
            .heightIn(min = Dimens.HomeChoiceCardHeight)
            .clip(shape)
            .background(if (primary) colors.primary else colors.surface)
            .border(Dimens.CardBorderWidth, if (primary) Color.Transparent else colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
        Text(
            text = stringResource(subRes),
            style = MaterialTheme.typography.titleMedium,
            color = content,
            maxLines = 2,
        )
    }
}

/**
 * C-02 진행 중 여행(RECORDING · PAUSED) — 여행명 · 기록 상태 · 경과 · 장소 수 · 「이 장소 기록」(주요) · 「여행 종료」(A-04 팝업).
 */
@Composable
fun ColumnScope.TripInProgressTop(
    tripName: String?,
    status: TripStatus,
    elapsedMinutes: Long,
    placeCount: Int,
    onRecordPlace: () -> Unit,
    onEndTrip: () -> Unit,
) {
    Text(
        text = tripNameText(tripName),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = tripStatusText(status),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
    Text(
        text = elapsedText(elapsedMinutes),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
    Text(
        text = stringResource(R.string.trip_place_count, placeCount),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        PrimaryButton(onClick = onRecordPlace, modifier = Modifier.weight(1f).height(Dimens.HomeButtonHeight)) {
            Text(text = stringResource(R.string.action_record_place), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        DangerButton(onClick = onEndTrip, modifier = Modifier.weight(1f).height(Dimens.HomeButtonHeight)) {
            Text(text = stringResource(R.string.action_end_trip), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
