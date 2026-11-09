package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R

@Composable
internal fun DemoTripsScreen(state: DemoUiState, demo: DemoViewModel) {
    val trip = state.ownTrip
    var selectedStop by rememberSaveable(trip?.id) { mutableStateOf<Int?>(null) }
    var preview by rememberSaveable { mutableStateOf(false) }
    DemoPage("내 여행", "여행이 끝나면 기록한 장소가 로드맵으로 모여요.") {
        if (trip == null) {
            DemoCard { DemoTitle("아직 저장할 여행이 없어요.") }
            return@DemoPage
        }
        DemoCard {
            DemoTitle(trip.title)
            Text("${trip.city} ${trip.district} · 장소 ${trip.stops.size}곳 · ${trip.companion}")
            DemoRoute(trip.stops, showStops = false)
        }
        DemoCard {
            DemoTitle("장소별 경험")
            val untagged = state.editedStops.count { it.tags.isEmpty() }
            Text(stringResource(R.string.demo_untagged_places, untagged))
            state.editedStops.forEachIndexed { index, stop ->
                DemoPlaceTagRow(stop) { selectedStop = index }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val unsaved = !state.saved || state.editedStops.map { it.copy(memo = it.memo.trim()) } != trip.stops
                DemoAction("로드맵 저장", demo::save, enabled = unsaved)
                if (state.published) {
                    DemoAction("공개 취소", demo::unpublish, primary = false)
                } else {
                    DemoAction("공개하기", { preview = true }, enabled = !unsaved, primary = false)
                }
            }
            state.message?.let { Text(it) }
        }
    }
    selectedStop?.let { index ->
        state.editedStops.getOrNull(index)?.let { stop ->
            TripExperienceDialog(
                stop = stop,
                onSave = { tags, memo ->
                    demo.editStop(index) { it.copy(tags = tags, memo = memo) }
                    selectedStop = null
                },
                onDismiss = { selectedStop = null },
            )
        }
    }
    if (preview && trip != null) {
        val sharedStops = trip.stops.joinToString("\n") { stop ->
            (listOf(stop.name) + stop.tags + stop.memo).filter(String::isNotBlank).joinToString(" · ")
        }
        DemoConfirm(
            "공개 범위 확인",
            "${trip.title}\n$sharedStops\n\n이 내용을 시연용 추천에 익명으로 공개해요.",
            "동의하고 공개", { preview = false },
        ) {
            demo.publish()
            preview = false
        }
    }
}
