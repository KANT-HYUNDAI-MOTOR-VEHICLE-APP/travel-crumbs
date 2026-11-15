package com.example.travelcrumbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.domain.model.NearbySectionState
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.ui.components.ScreenHeader
import com.example.travelcrumbs.ui.home.NearbyRoadmapsSection
import com.example.travelcrumbs.ui.home.components.NearbyRoadmapsArea
import com.example.travelcrumbs.ui.home.components.NoTripTop
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.components.AppNavigationRail
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import com.example.travelcrumbs.ui.tripstart.TripStartScreen
import com.example.travelcrumbs.ui.tripstart.DestinationItem
import com.example.travelcrumbs.ui.tripstart.TripStartUiState
import dagger.hilt.android.AndroidEntryPoint

/** 실제 저장 데이터를 바꾸지 않고 v1.8 화면을 에뮬레이터에서 대조하는 debug 전용 진입점. */
@AndroidEntryPoint
class V18InspectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra(EXTRA_SCREEN).orEmpty()
        setContent {
            TravelCrumbsTheme {
                InspectionShell {
                    if (screen == SCREEN_TRIP_START) {
                        TripStartScreen(
                            onDone = {},
                            onCancel = {},
                            inspectionState = TripStartUiState(
                                defaultMonth = 11,
                                defaultDay = 15,
                                tripNm = "11월 15일 여행",
                                canStart = true,
                                companions = setOf(CompanionType.SPOUSE, CompanionType.CHILD),
                                destinations = listOf(
                                    DestinationItem("gyeongbokgung", "경복궁", "서울 종로구 사직로 161"),
                                    DestinationItem("namsan", "남산서울타워", "서울 용산구 남산공원길 105"),
                                ),
                            ),
                        )
                    } else {
                        HomeInspectionContent()
                    }
                }
            }
        }
    }

    private companion object {
        const val EXTRA_SCREEN = "screen"
        const val SCREEN_TRIP_START = "trip-start"
    }
}

@Composable
private fun InspectionShell(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            AppNavigationRail(highlighted = AppDestination.RECORDS, onSelect = {})
            Box(
                modifier = Modifier
                    .padding(top = Dimens.ScreenTop)
                    .width(2.dp)
                    .height(1112.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    }
}

@Composable
private fun HomeInspectionContent() {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = getStringResource(R.string.nav_records),
            parked = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd, top = Dimens.ScreenTop),
        )
        Spacer(Modifier.height(Dimens.ScreenHeaderToBody))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ScreenBodyHeight)
                .padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd),
        ) {
            NoTripTop(
                onStartTrip = {},
                onOpenRecommendations = {},
                recentTrip = null,
                onOpenTrip = {},
            ) {
                NearbyRoadmapsArea(
                    section = NearbyRoadmapsSection(state = NearbySectionState.CONSENT_NEEDED),
                    onOpen = { _, _ -> },
                    onAgree = {},
                    onDecline = {},
                )
            }
        }
    }
}

@Composable
private fun getStringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)
