package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DemoDrivingScreen(state: DemoUiState, demo: DemoViewModel) {
    var confirmEnd by remember { mutableStateOf(false) }
    var voiceNotice by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        DemoTitle(if (state.recording) "성수 한 바퀴 · ${if (state.paused) "일시정지" else "기록 중"}" else "주행 중")
        if (!state.recording) {
            Text("정차한 뒤 여행을 시작할 수 있어요.")
        } else {
            Text("경과 ${state.minutes}분 · 기록된 장소 ${state.stops.size}곳")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoAction("이 장소 기록", demo::addStop, enabled = !state.paused && state.nextSimulatedStop != null)
                DemoAction("음성 입력", { voiceNotice = true }, enabled = !state.paused, primary = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoAction(if (state.paused) "기록 재개" else "일시정지", demo::togglePause, enabled = state.locationConsent, primary = false)
                DemoAction("여행 종료", { confirmEnd = true }, enabled = state.stops.isNotEmpty(), primary = false)
            }
            if (voiceNotice) Text("시연에서는 실제 음성 입력이 연결되지 않아요.")
        }
    }
    if (confirmEnd) DemoConfirm(
        "여행을 종료할까요?", "정차 후 내 여행에서 로드맵을 확인할 수 있어요.", "여행 종료", { confirmEnd = false },
    ) {
        demo.finish()
        confirmEnd = false
    }
}
