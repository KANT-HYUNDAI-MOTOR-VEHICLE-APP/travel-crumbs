package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.recommendations.GuidanceConfirmItem
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * B-05 안내 확인 팝업 — 「○○까지 안내할까요?」 · 주소 · 누구의 어느 로드맵 몇 번째 장소 / 「취소」(Secondary) · 「안내 시작」(Primary).
 * 바깥 누름 · 시스템 뒤로 = 취소. 창 애니메이션 끔. 요청 중엔 「안내 시작」을 다시 받지 않음.
 */
@Composable
fun GuidanceConfirmDialog(
    item: GuidanceConfirmItem,
    requesting: Boolean,
    onCancel: () -> Unit,
    onStart: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
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
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                Text(
                    text = stringResource(R.string.guidance_confirm_title, item.placeNm),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.placeOrd == 1) {
                        stringResource(R.string.guidance_confirm_body_first, item.addr, item.nicknameNm, item.tripNm)
                    } else {
                        stringResource(R.string.guidance_confirm_body_nth, item.addr, item.nicknameNm, item.tripNm, item.placeOrd)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    SecondaryButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = Dimens.MinTouchTarget)
                            .height(Dimens.HomeButtonHeight),
                    ) {
                        Text(text = stringResource(R.string.guidance_cancel), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                    PrimaryButton(
                        onClick = onStart,
                        enabled = !requesting,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = Dimens.MinTouchTarget)
                            .height(Dimens.HomeButtonHeight),
                    ) {
                        Text(text = stringResource(R.string.guidance_start), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
