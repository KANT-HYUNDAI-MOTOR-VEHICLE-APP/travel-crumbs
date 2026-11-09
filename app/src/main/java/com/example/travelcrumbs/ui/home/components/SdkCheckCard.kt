package com.example.travelcrumbs.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * SDK 확인 카드(기어 · 내비 · Gleo) — debug 빌드에서만 홈 맨 아래(스펙 D9 · D17).
 * 카드가 처음 보일 때 내비 경로 상태를 한 번 조회.
 */
@Composable
fun SdkCheckCard(
    drivingState: DrivingState,
    naviReady: Boolean,
    navi: SdkStatus<NaviRouteState>,
    openedByGleoAction: Boolean,
    onRefreshNavi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onRefreshNavi() }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Text(text = stringResource(R.string.sdk_card_title), style = MaterialTheme.typography.titleLarge)

            SdkRow(label = stringResource(R.string.home_vehicle_label)) {
                Text(text = stringResource(drivingState.labelRes()), style = MaterialTheme.typography.bodyLarge)
            }

            SdkRow(label = stringResource(R.string.home_navi_label)) {
                Text(
                    text = stringResource(if (naviReady) R.string.navi_ready else R.string.navi_not_ready),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = naviText(navi), style = MaterialTheme.typography.bodyLarge)
                SecondaryButton(onClick = onRefreshNavi) {
                    Text(text = stringResource(R.string.navi_refresh))
                }
            }

            SdkRow(label = stringResource(R.string.home_gleo_label)) {
                Text(text = stringResource(R.string.gleo_function), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(if (openedByGleoAction) R.string.gleo_opened else R.string.gleo_not_opened),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SdkRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing / 2)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun naviText(status: SdkStatus<NaviRouteState>): String = when (status) {
    SdkStatus.Idle -> stringResource(R.string.status_idle)
    SdkStatus.Loading -> stringResource(R.string.status_loading)
    is SdkStatus.Failed -> stringResource(R.string.status_failed, status.code)
    is SdkStatus.Ready -> stringResource(
        R.string.navi_route_state,
        stringResource(if (status.value.routing) R.string.yes else R.string.no),
        status.value.mode,
        status.value.routeOption,
    )
}

private fun DrivingState.labelRes(): Int = when (this) {
    DrivingState.PARKED -> R.string.driving_state_parked
    DrivingState.DRIVING -> R.string.driving_state_driving
    DrivingState.UNKNOWN -> R.string.driving_state_unknown
}
