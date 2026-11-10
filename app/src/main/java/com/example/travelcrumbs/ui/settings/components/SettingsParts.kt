package com.example.travelcrumbs.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.AppDialog
import com.example.travelcrumbs.ui.components.AppDialogBody
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.theme.Dimens

/** 설정 카드 한 줄 — 제목 · 설명 · 오른쪽 버튼(없으면 [actionLabel] null). */
@Composable
fun SettingsRow(
    title: String,
    sub: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (actionLabel != null) {
                SecondaryButton(
                    onClick = onAction,
                    modifier = Modifier.widthIn(min = Dimens.SettingsActionMinWidth).height(Dimens.HomeButtonHeight),
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/** 닉네임 수정 팝업 — 입력 칸 하나 · 「취소」 · 「저장」(정차 전용 화면 안에서만 뜸 · 팝업 틀 [AppDialog]). */
@Composable
fun NicknameDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppDialog(
        title = stringResource(R.string.nickname_dialog_title),
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

/**
 * 근처 로드맵 위치 이용 동의 팝업(#55 · 스펙 home-nearby 2-9 · A-07 추천 공개 동의 팝업과 같은 꼴) — 제목 K1 · 본문 K2 ·
 * 「동의」 · 「닫기」(팝업 틀 [AppDialog] · 시스템 뒤로 = 닫기).
 */
@Composable
fun NearbyConsentDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.nearby_consent_title),
        dismissLabel = stringResource(R.string.action_close),
        confirmLabel = stringResource(R.string.settings_consent_agree),
        onDismiss = onDismiss,
        onConfirm = onAgree,
    ) {
        AppDialogBody(text = stringResource(R.string.nearby_consent_body))
    }
}
