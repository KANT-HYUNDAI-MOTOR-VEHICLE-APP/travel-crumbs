package com.example.travelcrumbs.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.ui.driving.DrivingScreen
import com.example.travelcrumbs.ui.home.HomeScreen
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.settings.SettingsScreen
import com.example.travelcrumbs.ui.shell.components.AppNavigationRail
import com.example.travelcrumbs.ui.shell.components.DraftBanner
import com.example.travelcrumbs.ui.trips.TripsScreen

/**
 * 앱 틀 — 판정이 정차면 레일 + 목적지 화면, 주행 · 모름이면 주행 전용 화면 하나만(스펙 app-shell 2 · 2-6).
 * 전환은 애니메이션 없이 바로. 주행 화면에선 BackHandler 를 두지 않음 → 시스템 뒤로가 앱을 뒤로 보냄(D7).
 *
 * [destinationContent] 는 목적지 → 화면 연결. 테스트가 시험 전용 입력 화면을 끼울 때만 바꿈.
 */
@Composable
fun AppShellScreen(
    modifier: Modifier = Modifier,
    viewModel: AppShellViewModel = viewModel(),
    destinationContent: @Composable (AppDestination) -> Unit = { DestinationContent(it) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (uiState.showDrivingScreen) {
            DrivingScreen()
        } else {
            BackHandler(enabled = uiState.selectedDestination != AppDestination.START) {
                viewModel.onBack()
            }
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavigationRail(
                    selected = uiState.selectedDestination,
                    onSelect = viewModel::selectDestination,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (uiState.draftAvailable) {
                        DraftBanner(onResume = viewModel::resumeDraft)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        destinationContent(uiState.selectedDestination)
                    }
                }
            }
        }
    }
}

/** 목적지 → 화면. 목적지를 더하면 이 when 이 컴파일 오류로 알려 줌. */
@Composable
private fun DestinationContent(destination: AppDestination) {
    when (destination) {
        AppDestination.HOME -> HomeScreen()
        AppDestination.TRIPS -> TripsScreen()
        AppDestination.SETTINGS -> SettingsScreen()
    }
}
