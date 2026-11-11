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
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.ui.driving.DrivingScreen
import com.example.travelcrumbs.ui.components.GleoNoticeLine
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.ui.gleo.GleoCommandViewModel
import com.example.travelcrumbs.ui.home.HomeScreen
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.recommendations.RecommendationsScreen
import com.example.travelcrumbs.ui.settings.SettingsScreen
import com.example.travelcrumbs.ui.shell.components.AppNavigationRail
import com.example.travelcrumbs.ui.shell.components.DrivingRestrictedContent
import com.example.travelcrumbs.ui.shell.components.UnfinishedInputBanner
import com.example.travelcrumbs.ui.trips.TripsScreen

/**
 * 앱 틀 — 레일 4칸은 늘 보이고 눌림 · 내용 영역만 판정대로(스펙 app-shell-v5 2 · 2-1 · 2-7 · D29 · D33).
 * 정차: 고른 목적지의 정차 화면 + 「이어서 쓰기」 띠. 주행 · 모름: 「기록」 칸 = 주행 전용 화면 · 그 밖 = 주행 제한 안내.
 * 전환은 애니메이션 없이 바로. 주행 · 모름에선 BackHandler 를 두지 않음 → 시스템 뒤로가 앱을 뒤로 보냄(#6 D7).
 *
 * [destinationContent] 는 정차 목적지 → 화면 연결. 테스트가 시험 전용 입력 화면을 끼울 때만 바꿈.
 */
@Composable
fun AppShellScreen(
    modifier: Modifier = Modifier,
    viewModel: AppShellViewModel = viewModel(),
    gleoViewModel: GleoCommandViewModel = viewModel(),
    destinationContent: @Composable (AppDestination) -> Unit = { destination ->
        val shell = viewModel.uiState.collectAsStateWithLifecycle().value
        ParkedDestinationContent(
            destination = destination,
            onOpenRecommendations = { viewModel.openRecommendations() },
            onOpenTrip = viewModel::openTrip,
            recommendationsEntry = shell.recommendationsEntry,
            recommendationsRegion = shell.recommendationsRegion,
            tripsEntry = shell.tripsEntry,
            tripsOpenTripNo = shell.tripsOpenTripNo,
        )
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gleoState by gleoViewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
      // Gleo 명령 결과 한 줄을 화면 맨 아래에 겹쳐 띄우려고 Box 로 감쌈(#25 U3).
      Box(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isDrivingMode) {
            BackHandler(enabled = uiState.selectedDestination != AppDestination.START) {
                viewModel.onBack()
            }
        }
        Row(modifier = Modifier.fillMaxSize()) {
            AppNavigationRail(
                highlighted = uiState.highlightedRailItem,
                onSelect = viewModel::selectDestination,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                if (uiState.isDrivingMode) {
                    DrivingContent(uiState.drivingDestination)
                } else {
                    if (uiState.unfinishedInputAvailable) {
                        UnfinishedInputBanner(onResume = viewModel::resumeUnfinishedInput)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        destinationContent(uiState.selectedDestination)
                    }
                }
            }
        }
        if (gleoState.command.phase != GleoCommandPhase.IDLE) {
            GleoNoticeLine(
                state = gleoState.command,
                parked = !uiState.isDrivingMode,
                onShown = gleoViewModel::onNoticeShown,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
      }
    }
}

/** 주행 · 모름의 내용 영역 — 주행 중 허용 칸(「기록」)만 주행 전용 화면 · 그 밖은 안내(2-8). */
@Composable
private fun DrivingContent(destination: AppDestination) {
    if (destination.allowedWhileDriving) {
        DrivingScreen()
    } else {
        DrivingRestrictedContent(titleRes = destination.labelRes)
    }
}

/** 정차 목적지 → 화면. 목적지를 더하면 이 when 이 컴파일 오류로 알려 줌. */
@Composable
private fun ParkedDestinationContent(
    destination: AppDestination,
    onOpenRecommendations: () -> Unit,
    onOpenTrip: (Long?) -> Unit,
    recommendationsEntry: Int,
    recommendationsRegion: Region?,
    tripsEntry: Int,
    tripsOpenTripNo: Long?,
) {
    when (destination) {
        // 기록 칸 정차 화면 = 지금 ui/home 파일(N28 — 이웃 브랜치가 고치는 파일을 옮기지 않음)
        AppDestination.RECORDS -> HomeScreen(onOpenRecommendations = onOpenRecommendations, onOpenTrip = onOpenTrip)
        AppDestination.TRIPS -> TripsScreen(entry = tripsEntry, openTripNo = tripsOpenTripNo)
        AppDestination.RECOMMENDATIONS -> RecommendationsScreen(entryKey = recommendationsEntry, entryRegion = recommendationsRegion)
        AppDestination.SETTINGS -> SettingsScreen()
    }
}
