package com.example.travelcrumbs.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

/** 내용이 아직 없는 목적지 — 제목 + 「준비 중」. */
@Composable
fun PlaceholderContent(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.not_ready),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
