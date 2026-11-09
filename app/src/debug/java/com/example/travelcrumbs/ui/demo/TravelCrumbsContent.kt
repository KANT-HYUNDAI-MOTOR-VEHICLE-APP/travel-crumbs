package com.example.travelcrumbs.ui.demo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.MainActivity
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.shell.AppShellViewModel
import com.example.travelcrumbs.ui.theme.TcColors
import com.example.travelcrumbs.ui.theme.TcTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun TravelCrumbsContent() {
    val shell: AppShellViewModel = viewModel()
    val demo: DemoViewModel = viewModel()
    val state by demo.uiState.collectAsStateWithLifecycle()
    val guidance: DemoGuidanceViewModel = viewModel()
    val guidanceState by guidance.uiState.collectAsStateWithLifecycle()
    val pendingSave by guidance.pendingSave.collectAsStateWithLifecycle()
    val arrivalToSave by guidance.arrivalToSave.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(guidance, shell, demo, context) {
        guidance.parkedReturn.filterNotNull().distinctUntilChanged().collect { destination ->
            if (destination.account == demo.uiState.value.account) {
                shell.onBack()
                bringTravelCrumbsForward(context)
            }
        }
    }
    LifecycleResumeEffect(guidance) {
        guidance.recheck()
        onPauseOrDispose { }
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TcColors.BrandPrimary,
            onPrimary = TcColors.TextOnPrimary,
            secondary = TcColors.BrandSecondary,
            onSecondary = TcColors.TextOnPrimary,
            background = TcColors.BackgroundBase,
            onBackground = TcColors.TextPrimary,
            surface = TcColors.BackgroundSurface,
            onSurface = TcColors.TextPrimary,
            surfaceVariant = TcColors.BackgroundElevated,
            onSurfaceVariant = TcColors.TextSecondary,
            outline = TcColors.BorderDefault,
            outlineVariant = TcColors.BorderSubtle,
        ),
        typography = TcTypography,
    ) {
        AppShellScreen(
            viewModel = shell,
            destinationContent = { destination ->
                if (state.account == null) {
                    DemoLogin(demo::login)
                } else {
                    val account = state.account ?: return@AppShellScreen
                    val itineraryCard: @Composable () -> Unit = {
                        guidanceState?.takeIf { it.account == account }?.let { itinerary ->
                            DemoItineraryCard(
                                itinerary = itinerary,
                                arrivedPlaceToSave = arrivalToSave?.takeIf { it.account == account },
                                onContinue = {
                                    if (guidance.prepareNext(account)) guidance.dispatch(openNativeNavigation(context))
                                },
                                onSimulateArrival = { guidance.simulateArrival(account) },
                                onSaveArrival = { arrival ->
                                    shell.onBack()
                                    guidance.saveArrival(arrival)
                                },
                                onEnd = { guidance.end(account) },
                            )
                        }
                    }
                    when (destination) {
                        AppDestination.RECORDS -> DemoRecordsScreen(
                            state,
                            demo,
                            shell::selectDestination,
                            pendingSave?.takeIf { it.account == state.account },
                            guidance::dismissSave,
                            itineraryCard,
                        )
                        AppDestination.TRIPS -> DemoTripsScreen(state, demo)
                        AppDestination.RECOMMENDATIONS -> DemoRecommendationsScreen(
                            state = state,
                            demo = demo,
                            canStartItinerary = guidanceState == null,
                            onStart = { roadmap ->
                                if (guidance.prepareStart(roadmap, account)) guidance.dispatch(openNativeNavigation(context))
                            },
                            itineraryCard = itineraryCard,
                        )
                        AppDestination.SETTINGS -> DemoSettingsScreen(state, demo) {
                            guidance.resetSession()
                            shell.selectDestination(AppDestination.RECORDS)
                        }
                    }
                }
            },
            drivingContent = { DemoDrivingScreen(state, demo) },
        )
    }
}

/** 기존 앱 태스크를 앞으로 요청. 시스템이 전환을 막으면 팝업은 대기 상태로 유지. */
private fun bringTravelCrumbsForward(context: Context) {
    try {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    } catch (_: ActivityNotFoundException) {
        Log.w("DemoNavigation", "기록 화면 전환 대기")
    } catch (_: SecurityException) {
        Log.w("DemoNavigation", "기록 화면 전환 대기")
    }
}

/** 시스템 지도/내비 앱 실행과 SDK의 경로 전달은 각각 확인함. */
private fun openNativeNavigation(context: Context): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_MAPS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
