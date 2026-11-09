package com.example.travelcrumbs.ui.trips.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.PrimaryButton
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.theme.Dimens

/** 처음 공개할 때 추천 공개 동의(RECO_PUBLIC)를 묻는 팝업(A-07 · A-08 토글 · #51 U6). */
@Composable
fun RecoConsentDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.publish_consent_title), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = stringResource(R.string.publish_consent_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            PrimaryButton(onClick = onAgree, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_consent_and_publish), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            SecondaryButton(onClick = onDismiss, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** A-08 여행명 고치기 팝업 — 입력 칸 하나 · 「취소」 · 「저장」. */
@Composable
fun RenameTripDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.trip_rename_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(onClick = onSave, enabled = value.isNotBlank(), modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            SecondaryButton(onClick = onDismiss, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
