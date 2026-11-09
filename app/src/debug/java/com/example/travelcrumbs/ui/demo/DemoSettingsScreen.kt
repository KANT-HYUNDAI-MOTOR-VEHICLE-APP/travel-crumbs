package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
internal fun DemoSettingsScreen(state: DemoUiState, demo: DemoViewModel, onReturnToRecords: () -> Unit) {
    var confirm by remember { mutableStateOf<SettingsPrompt?>(null) }
    DemoPage("설정", "${state.account} 님 · 시연 계정과 동의를 관리해요.") {
        DemoCard {
            DemoTitle("위치 이용 동의")
            Text(if (state.locationConsent) "동의함" else "동의하지 않음")
            DemoAction(
                if (state.locationConsent) "위치 동의 철회" else "위치 이용에 동의",
                { confirm = SettingsPrompt.LOCATION },
                primary = false,
            )
        }
        DemoCard {
            DemoTitle("공개 동의")
            Text(if (state.ownTrip != null && state.published) "내 로드맵 공개 중" else "공개 중인 내 로드맵 없음")
            if (state.ownTrip != null && state.published) DemoAction("공개 동의 철회", demo::unpublish, primary = false)
        }
        DemoCard {
            DemoTitle("시연 계정")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoAction("로그아웃", { confirm = SettingsPrompt.LOGOUT }, primary = false)
                DemoAction("시연 초기화", { confirm = SettingsPrompt.RESET }, primary = false)
            }
        }
    }
    when (confirm) {
        SettingsPrompt.LOCATION -> DemoConfirm(
            "위치 이용 동의", "이 시연은 실제 위치를 수집하지 않아요. 동의를 철회하면 진행 중 기록이 일시정지돼요.",
            if (state.locationConsent) "동의 철회" else "동의하기", { confirm = null },
        ) {
            demo.setLocationConsent(!state.locationConsent)
            confirm = null
        }
        SettingsPrompt.LOGOUT -> DemoConfirm(
            "시연 계정에서 로그아웃할까요?", "진행 중 기록은 종료되고, 공개한 로드맵은 다른 시연 계정에서도 볼 수 있어요.",
            "로그아웃하기", { confirm = null },
        ) {
            onReturnToRecords()
            demo.logout()
        }
        SettingsPrompt.RESET -> DemoConfirm(
            "시연을 처음부터 시작할까요?", "이 세션에서 만든 기록, 공개한 로드맵과 별점이 초기화돼요.",
            "초기화하기", { confirm = null },
        ) {
            onReturnToRecords()
            demo.reset()
        }
        null -> Unit
    }
}

private enum class SettingsPrompt { LOCATION, LOGOUT, RESET }
