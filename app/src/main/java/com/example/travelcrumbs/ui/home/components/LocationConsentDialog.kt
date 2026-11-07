package com.example.travelcrumbs.ui.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R

/** 위치정보 수집 동의(A-03-1) — 첫 여행 시작 시 한 번. 바깥을 눌러 닫으면 「동의 안 함」과 같음. */
@Composable
fun LocationConsentDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Text(
                text = stringResource(R.string.consent_location_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.consent_location_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text(text = stringResource(R.string.consent_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(text = stringResource(R.string.consent_decline))
            }
        },
    )
}
