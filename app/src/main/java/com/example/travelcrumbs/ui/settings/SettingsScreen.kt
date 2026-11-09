package com.example.travelcrumbs.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.NoticeLine
import com.example.travelcrumbs.ui.settings.components.NicknameDialog
import com.example.travelcrumbs.ui.settings.components.SettingsRow
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 설정(와이어프레임 v6 C-03 · #51 U2) — 들어오는 길은 레일 「설정」 칸 하나(D35). 위에서 아래로 닉네임 · 위치정보 수집 · 추천 공개 · 계정.
 * 주행 중엔 앱 틀이 이 화면 대신 「정차 후 볼 수 있어요」를 그림(입력 칸 없음).
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            SettingsRow(
                title = stringResource(R.string.settings_nickname),
                sub = stringResource(R.string.settings_nickname_sub, uiState.nicknameNm),
                actionLabel = stringResource(R.string.settings_edit),
                onAction = viewModel::onEditNickname,
            )
            val location = uiState.locationAgreedOn
            SettingsRow(
                title = stringResource(R.string.settings_location),
                sub = if (location != null) {
                    stringResource(R.string.settings_location_agreed, location.year, location.month, location.day)
                } else {
                    stringResource(R.string.settings_location_none)
                },
                actionLabel = if (location != null) stringResource(R.string.settings_withdraw) else null,
                onAction = viewModel::onWithdrawLocation,
            )
            SettingsRow(
                title = stringResource(R.string.settings_reco),
                sub = stringResource(if (uiState.recoAgreed) R.string.settings_reco_agreed else R.string.settings_reco_none),
                actionLabel = if (uiState.recoAgreed) stringResource(R.string.settings_withdraw) else null,
                onAction = viewModel::onWithdrawReco,
            )
            SettingsRow(
                title = stringResource(R.string.settings_account),
                sub = stringResource(R.string.settings_account_sub),
                actionLabel = null,
                onAction = {},
            )
        }
        NoticeLine(message = uiState.message, onShown = viewModel::onMessageShown)
    }

    uiState.nicknameDraft?.let { draft ->
        NicknameDialog(
            value = draft,
            onValueChange = viewModel::onNicknameChange,
            onDismiss = viewModel::onDismissNickname,
            onSave = viewModel::onSaveNickname,
        )
    }
}
