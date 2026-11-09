package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.navigation.AppDestination

@Composable
internal fun DemoRecordsScreen(
    state: DemoUiState,
    demo: DemoViewModel,
    navigate: (AppDestination) -> Unit,
    guidedDestination: DemoGuidedDestination?,
    onSavePromptHandled: (DemoGuidedDestination) -> Unit,
    itineraryCard: @Composable () -> Unit,
) {
    var tagEdit by remember { mutableStateOf<RecordTagEdit?>(null) }
    var confirm by remember { mutableStateOf<RecordingPrompt?>(null) }
    DemoPage("기록", "${state.account} 님 · 시연 데이터로 여행의 경로와 장소를 남겨 보세요.") {
        itineraryCard()
        DemoCard {
            DemoTitle(
                when {
                    state.paused -> "일시정지"
                    state.recording -> "성수 한 바퀴 · 기록 중"
                    else -> "새 여행을 시작해 보세요"
                },
            )
            if (state.recording) {
                Text("경과 ${state.minutes}분 · 기록된 장소 ${state.stops.size}곳")
                DemoRoute(state.stops, showStops = false, simulatedProgress = state.steps)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoAction("시뮬레이션 이동", demo::advance, enabled = !state.paused && state.steps < 3)
                    DemoAction("이 장소 기록", demo::addStop, enabled = !state.paused && state.nextSimulatedStop != null, primary = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoAction(if (state.paused) "기록 재개" else "일시정지", demo::togglePause, enabled = state.locationConsent, primary = false)
                    DemoAction("여행 종료", { confirm = RecordingPrompt.END }, enabled = state.stops.isNotEmpty(), primary = false)
                }
            } else {
                Text("성수 한 바퀴 · 함께 가는 사람")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("혼자", "친구", "가족").forEach { companion ->
                        FilterChip(
                            selected = state.companion == companion,
                            onClick = { demo.selectCompanion(companion) },
                            label = { Text(companion) },
                        )
                    }
                }
                DemoAction("여행 시작", { if (state.locationConsent) demo.start() else confirm = RecordingPrompt.LOCATION })
            }
        }
        if (state.recording && state.stops.isNotEmpty()) {
            DemoCard {
                DemoTitle(stringResource(R.string.demo_recorded_places))
                state.stops.forEach { stop ->
                    DemoPlaceTagRow(stop) { tagEdit = RecordTagEdit(stop.name, savedPlace = false) }
                }
            }
        }
        state.ownTrip?.let { trip ->
            DemoCard {
                DemoTitle("최근 여행 · ${trip.title}")
                DemoAction("로드맵 보기", { navigate(AppDestination.TRIPS) }, primary = false)
            }
        }
        if (state.ownSavedPlaces.isNotEmpty()) {
            DemoCard {
                DemoTitle(stringResource(R.string.demo_saved_destinations))
                state.ownSavedPlaces.forEach { stop ->
                    DemoPlaceTagRow(stop) { tagEdit = RecordTagEdit(stop.name, savedPlace = true) }
                    if (state.recording && state.stops.none { it.name == stop.name }) {
                        DemoAction(
                            stringResource(R.string.demo_add_saved_destination),
                            { demo.addSavedPlace(stop) },
                            primary = false,
                        )
                    }
                }
            }
        }
        if (!state.recording) DemoAction("추천 보러 가기", { navigate(AppDestination.RECOMMENDATIONS) }, primary = false)
    }
    if (guidedDestination != null) {
        GuidedDestinationSaveDialog(
            stop = state.guidedStop(guidedDestination.place),
            recording = state.recording,
            onSave = { tags ->
                demo.saveGuidedDestination(guidedDestination, tags)
                onSavePromptHandled(guidedDestination)
            },
            onDismiss = { onSavePromptHandled(guidedDestination) },
        )
        return
    }
    val editing = tagEdit
    val editingStop = editing?.let { target ->
        val places = if (target.savedPlace) state.ownSavedPlaces else state.stops
        places.firstOrNull { it.name == target.name }
    }
    if (editing != null && editingStop != null) {
        RecordTagDialog(
            stop = editingStop,
            onSave = { tags ->
                demo.updateRecordTags(editing.name, tags, editing.savedPlace)
                tagEdit = null
            },
            onDismiss = { tagEdit = null },
        )
    }
    when (confirm) {
        RecordingPrompt.LOCATION -> DemoConfirm(
            "위치 이용 동의", "여행을 기록할 때만 위치를 이용한다는 상황을 시연합니다. 이번 시연은 샘플 위치만 사용해요.",
            "동의하고 시작", { confirm = null },
        ) {
            demo.setLocationConsent(true)
            demo.start()
            confirm = null
        }
        RecordingPrompt.END -> DemoConfirm(
            "여행을 종료할까요?", "기록한 장소를 로드맵에서 확인할 수 있어요.",
            "종료하고 로드맵 보기", { confirm = null },
        ) {
            demo.finish()
            confirm = null
            navigate(AppDestination.TRIPS)
        }
        null -> Unit
    }
}

private enum class RecordingPrompt { LOCATION, END }

private data class RecordTagEdit(val name: String, val savedPlace: Boolean)
