package com.example.travelcrumbs.ui.driving.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppDialog
import com.example.travelcrumbs.ui.components.DialogConfirmKind
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 여행 종료 확인 팝업(와이어프레임 A-08 · 스펙 app-shell-v5 2-6) — 팝업 틀 [AppDialog](가운데 · 폭 제한 · 뒤 화면 어둡게).
 * 대화상자 창이 화면 전체를 덮어 바깥(레일 칸 포함) 누름은 앱 화면에 닿지 않고 아무 일 없음 · 시스템 뒤로는 [onContinue] — 레일 이동 없음(V45).
 * 「계속 기록」 = Secondary · 「종료」 = Danger(디자인 시스템 12절) · 주행 중에도 뜨므로 버튼 높이 [Dimens.DrivingButtonHeight].
 * 자동으로 닫히는 시간 없음 · 창 열림 애니메이션 끔(주행 중 애니메이션 없음).
 */
@Composable
fun EndTripConfirmDialog(
    onContinue: () -> Unit,
    onEnd: () -> Unit,
) {
    AppDialog(
        title = stringResource(R.string.end_trip_confirm_body),
        dismissLabel = stringResource(R.string.end_trip_confirm_continue),
        confirmLabel = stringResource(R.string.end_trip_confirm_end),
        onDismiss = onContinue,
        onConfirm = onEnd,
        confirmKind = DialogConfirmKind.Danger,
        buttonHeight = Dimens.DrivingButtonHeight,
    )
}
