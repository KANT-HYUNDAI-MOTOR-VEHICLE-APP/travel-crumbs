package com.example.travelcrumbs.ui.trips.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppDialog
import com.example.travelcrumbs.ui.components.AppDialogBody
import com.example.travelcrumbs.ui.components.AppTextField

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
    isError: Boolean,
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
        confirmEnabled = !isError,
    ) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            errorText = if (isError) stringResource(R.string.input_trip_name_error) else null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
