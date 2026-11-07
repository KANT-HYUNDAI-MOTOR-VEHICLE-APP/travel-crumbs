package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.ui.theme.Dimens
import kotlinx.coroutines.delay

/**
 * 화면 아래 한 줄 안내 — 자리를 늘 차지해 떴다 사라져도 화면이 움직이지 않음 · 애니메이션 없음.
 * 정해진 시간 뒤 [onShown] 으로 상태를 지움(스펙 app-shell 2-5).
 */
@Composable
fun NoticeLine(
    message: UiMessage?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(AppConfig.NOT_READY_NOTICE_MILLIS)
            onShown()
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.NoticeLineHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (message != null) {
            Text(
                text = stringResource(message.textRes()),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun UiMessage.textRes(): Int = when (this) {
    UiMessage.NOT_READY -> R.string.not_ready
    UiMessage.SERVER_UNAVAILABLE -> R.string.server_unavailable
    UiMessage.TRIP_ALREADY_RECORDING -> R.string.trip_already_recording
    UiMessage.TRIP_STARTED -> R.string.trip_started
}
