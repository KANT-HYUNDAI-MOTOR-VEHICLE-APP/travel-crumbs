package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.DangerButton
import com.example.travelcrumbs.ui.components.ActionTile
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.elapsedText
import com.example.travelcrumbs.ui.components.tripNameText
import com.example.travelcrumbs.ui.components.tripStatusText
import com.example.travelcrumbs.ui.components.TripSummaryItem
import com.example.travelcrumbs.ui.components.tripMetaText
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsColors

/**
 * C-02 진행 중 여행이 없을 때(와이어프레임 v6 · #51 U2) — 인사 · 「여행 시작」(목적지 있음 · 주요) · 「추천 보러 가기」(목적지 없음) 나란히 ·
 * 최근 여행 1개. 넓으면 두 카드 한 줄 · 좁은 칸(나눈 칸)이면 위아래로. 고르는 건 사용자(앱이 판단하지 않음).
 */
@Composable
fun ColumnScope.NoTripTop(
    onStartTrip: () -> Unit,
    onOpenRecommendations: () -> Unit,
    recentTrip: TripSummaryItem?,
    onOpenTrip: (Long) -> Unit,
    nearbyContent: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < Dimens.HomeChoiceRowMinWidth
        val start: @Composable (Modifier) -> Unit = { m ->
            ActionTile(
                title = stringResource(R.string.action_start_trip),
                subtitle = stringResource(R.string.home_start_trip_sub_v18),
                iconRes = R.drawable.ic_place,
                emphasized = true,
                onClick = onStartTrip,
                modifier = m,
            )
        }
        val reco: @Composable (Modifier) -> Unit = { m ->
            ActionTile(
                title = stringResource(R.string.home_recommend_roadmap),
                subtitle = stringResource(R.string.home_open_reco_sub_v18),
                iconRes = R.drawable.ic_nav_recommendations,
                emphasized = false,
                onClick = onOpenRecommendations,
                modifier = m,
            )
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                start(Modifier.fillMaxWidth().heightIn(min = 280.dp))
                reco(Modifier.fillMaxWidth().heightIn(min = 280.dp))
                if (recentTrip != null) RecentTripSummaryCard(recentTrip, { onOpenTrip(recentTrip.tripNo) }) else nearbyContent()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(Dimens.HomeActionAreaHeight),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(modifier = Modifier.weight(824f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    start(Modifier.fillMaxWidth().height(466.dp))
                    Column(modifier = Modifier.fillMaxWidth().height(470.dp)) { nearbyContent() }
                }
                reco(Modifier.weight(482f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun RecentTripSummaryCard(item: TripSummaryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colors.surface, MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.ActionTilePadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
    ) {
        Text(
            text = tripNameText(item.tripNm),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = tripMetaText(item),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.placeNames.isNotEmpty()) RouteStrip(item.placeNames)
    }
}

@Composable
private fun RouteStrip(placeNames: List<String>) {
    val first = placeNames.first()
    val last = placeNames.last()
    val dotCount = if (placeNames.size >= 3) 3 else 2
    val accent = TravelCrumbsColors.BrandPrimary
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
            val y = size.height / 2
            val start = 8.dp.toPx()
            val end = size.width - start
            drawLine(TravelCrumbsColors.BorderSubtle, Offset(start, y), Offset(end, y), 3.dp.toPx(), StrokeCap.Round)
            repeat(dotCount) { index ->
                val x = start + (end - start) * index / (dotCount - 1)
                drawCircle(accent, 7.dp.toPx(), Offset(x, y))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(last, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(720.dp),
    ) {
        val status: @Composable (Modifier) -> Unit = { panelModifier ->
            AppCard(modifier = panelModifier) {
                Text(
                    text = tripNameText(tripName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tripStatusText(status),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
                    Text(text = elapsedText(elapsedMinutes), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    Text(text = stringResource(R.string.trip_place_count, placeCount), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                }
                DangerButton(onClick = onEndTrip, modifier = Modifier.fillMaxWidth().height(Dimens.HomeButtonHeight)) {
                    Text(text = stringResource(R.string.action_end_trip), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        val actions: @Composable (Modifier) -> Unit = { panelModifier ->
            ActionTile(
                title = stringResource(R.string.action_record_place),
                subtitle = null,
                iconRes = R.drawable.ic_place,
                emphasized = true,
                onClick = onRecordPlace,
                modifier = panelModifier,
            )
        }
        if (maxWidth < Dimens.RailWidth * 3) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                status(Modifier.fillMaxWidth())
                actions(Modifier.fillMaxWidth().heightIn(min = Dimens.HomeChoiceCardHeight))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                status(Modifier.weight(1.2f).fillMaxHeight())
                actions(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
