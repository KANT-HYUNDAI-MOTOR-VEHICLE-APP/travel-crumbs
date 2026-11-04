package com.example.travelcrumbs.ui.home

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme

/** 환경 확인용 화면 — Pleos SDK 3종이 실제로 불리는지 한눈에. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SdkCard(label = stringResource(R.string.home_vehicle_label)) {
                Text(
                    text = stringResource(uiState.drivingState.labelRes()),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            SdkCard(label = stringResource(R.string.home_navi_label)) {
                Text(
                    text = stringResource(
                        if (uiState.naviReady) R.string.navi_ready else R.string.navi_not_ready,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = naviText(uiState.navi),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = viewModel::refreshNavi) {
                    Text(text = stringResource(R.string.navi_refresh))
                }
            }

            SdkCard(label = stringResource(R.string.home_gleo_label)) {
                Text(
                    text = stringResource(R.string.gleo_function),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        if (uiState.openedByGleoAction) {
                            R.string.gleo_opened
                        } else {
                            R.string.gleo_not_opened
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SdkCard(
    label: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            content()
        }
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

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    TravelCrumbsTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Text(text = "Travel Crumbs")
        }
    }
}
