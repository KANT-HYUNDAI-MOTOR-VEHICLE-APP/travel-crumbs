package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

/** 되돌릴 수 없는 일(여행 삭제 · 내 기록 모두 지우기 · #51 리뷰 P2) 전에 한 번 묻는 팝업 — 「취소」(보조) · [confirmRes](위험). 정차 화면에서만. */
@Composable
fun ConfirmDialog(
    titleRes: Int,
    bodyRes: Int,
    confirmRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            DangerButton(onClick = onConfirm, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(confirmRes), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            SecondaryButton(onClick = onDismiss, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
