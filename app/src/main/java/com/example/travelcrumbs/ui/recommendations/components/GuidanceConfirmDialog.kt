package com.example.travelcrumbs.ui.recommendations.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppDialog
import com.example.travelcrumbs.ui.components.AppDialogBody
import com.example.travelcrumbs.ui.recommendations.GuidanceConfirmItem

/**
 * B-05 안내 확인 팝업 — 「○○까지 안내할까요?」 · 주소 · 누구의 어느 로드맵 몇 번째 장소 / 「취소」(Secondary) · 「안내 시작」(Primary).
 * 바깥 누름 · 시스템 뒤로 = 취소. 창 애니메이션 끔(팝업 틀 [AppDialog]). 요청 중엔 「안내 시작」을 다시 받지 않음.
 */
@Composable
fun GuidanceConfirmDialog(
    item: GuidanceConfirmItem,
    requesting: Boolean,
    onCancel: () -> Unit,
    onStart: () -> Unit,
) {
    AppDialog(
        title = stringResource(R.string.guidance_confirm_title, item.placeNm),
        dismissLabel = stringResource(R.string.guidance_cancel),
        confirmLabel = stringResource(R.string.guidance_start),
        onDismiss = onCancel,
        onConfirm = onStart,
        confirmEnabled = !requesting,
    ) {
        AppDialogBody(
            text = if (item.placeOrd == 1) {
                stringResource(R.string.guidance_confirm_body_first, item.addr, item.nicknameNm, item.tripNm)
            } else {
                stringResource(R.string.guidance_confirm_body_nth, item.addr, item.nicknameNm, item.tripNm, item.placeOrd)
            },
            maxLines = 2,
        )
    }
}
