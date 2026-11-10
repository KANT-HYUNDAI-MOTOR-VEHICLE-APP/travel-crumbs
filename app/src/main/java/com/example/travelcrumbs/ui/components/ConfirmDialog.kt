package com.example.travelcrumbs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R

/** 되돌릴 수 없는 일(여행 삭제 · 내 기록 모두 지우기 · #51 리뷰 P2) 전에 한 번 묻는 팝업 — 「취소」(보조) · [confirmRes](위험). 정차 화면에서만. */
@Composable
fun ConfirmDialog(
    titleRes: Int,
    bodyRes: Int,
    confirmRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        title = stringResource(titleRes),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(confirmRes),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmKind = DialogConfirmKind.Danger,
    ) {
        AppDialogBody(text = stringResource(bodyRes))
    }
}
