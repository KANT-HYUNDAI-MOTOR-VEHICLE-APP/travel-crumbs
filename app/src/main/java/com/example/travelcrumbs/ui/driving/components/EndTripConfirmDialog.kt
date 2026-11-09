package com.example.travelcrumbs.ui.driving.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 여행 종료 확인 팝업(와이어프레임 A-08 · 스펙 app-shell-v5 2-6).
 * 대화상자 창이라 바깥(레일 칸 포함) 누름 · 시스템 뒤로는 앱 화면에 닿지 않고 [onContinue] 로만 감 — 레일 이동 없음(V45).
 * 자동으로 닫히는 시간 없음 · 창 열림 애니메이션 끔(주행 중 애니메이션 없음).
 */
@Composable
fun EndTripConfirmDialog(
    onContinue: () -> Unit,
    onEnd: () -> Unit,
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setWindowAnimations(0) }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
            ) {
                Text(
                    text = stringResource(R.string.end_trip_confirm_body),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = Dimens.MinTouchTarget)
                            .height(Dimens.DrivingButtonHeight),
                    ) {
                        DialogButtonText(R.string.end_trip_confirm_continue)
                    }
                    Button(
                        onClick = onEnd,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = Dimens.MinTouchTarget)
                            .height(Dimens.DrivingButtonHeight),
                    ) {
                        DialogButtonText(R.string.end_trip_confirm_end)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButtonText(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
