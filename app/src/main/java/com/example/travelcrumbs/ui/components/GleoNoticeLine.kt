package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.ui.theme.Dimens
import kotlinx.coroutines.delay

/** 화면 이동·입력·애니메이션 없는 공통 결과 한 줄. 주행·UNKNOWN에서도 같은 문구만 표시. */
@Composable
fun GleoNoticeLine(
    state: GleoCommandState,
    onShown: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.noticeId, state.phase) {
        if (state.phase != GleoCommandPhase.PREPARING && state.phase != GleoCommandPhase.PROCESSING) {
            delay(AppConfig.NOT_READY_NOTICE_MILLIS)
            onShown(state.noticeId)
        }
    }
    val text = when (state.phase) {
        GleoCommandPhase.IDLE -> return
        GleoCommandPhase.PREPARING -> R.string.gleo_preparing
        GleoCommandPhase.PROCESSING -> R.string.gleo_processing
        GleoCommandPhase.SAVED -> R.string.gleo_saved
        GleoCommandPhase.INVALID_REQUEST -> R.string.gleo_invalid_request
        GleoCommandPhase.NOT_READY -> R.string.gleo_not_ready
        GleoCommandPhase.CONSENT_REQUIRED -> R.string.gleo_consent_required
        GleoCommandPhase.UNCONFIRMED -> R.string.gleo_unconfirmed
    }
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxWidth().height(Dimens.NoticeLineHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
