package com.example.travelcrumbs.ui.trips.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppDialog
import com.example.travelcrumbs.ui.components.AppDialogBody

/** 처음 공개할 때 추천 공개 동의(RECO_PUBLIC)를 묻는 팝업(A-07 · A-08 토글 · #51 U6) — 팝업 틀 [AppDialog]. */
@Composable
fun RecoConsentDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.publish_consent_title),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(R.string.action_consent_and_publish),
        onDismiss = onDismiss,
        onConfirm = onAgree,
    ) {
        AppDialogBody(text = stringResource(R.string.publish_consent_body))
    }
}

/** A-08 여행명 고치기 팝업 — 입력 칸 하나 · 「취소」 · 「저장」(팝업 틀 [AppDialog]). */
@Composable
fun RenameTripDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppDialog(
        title = stringResource(R.string.trip_rename_title),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(R.string.action_save),
        onDismiss = onDismiss,
        onConfirm = onSave,
        confirmEnabled = value.isNotBlank(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
