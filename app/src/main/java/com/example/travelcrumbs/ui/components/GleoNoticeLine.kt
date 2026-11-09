package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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

/** 주행·UNKNOWN은 정적 한 줄, 정차 중 여행 복원에는 진행 표시를 함께 사용. */
@Composable
fun GleoNoticeLine(
    state: GleoCommandState,
    parked: Boolean,
    onShown: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.noticeId, state.phase) {
        if (!state.phase.inProgress) {
            delay(AppConfig.NOT_READY_NOTICE_MILLIS)
            onShown(state.noticeId)
        }
    }
    val showProgress = parked && state.phase == GleoCommandPhase.LOADING_TRIP
    val text = when (state.phase) {
        GleoCommandPhase.IDLE -> return
        GleoCommandPhase.PREPARING -> R.string.gleo_preparing
        GleoCommandPhase.LOADING_TRIP -> if (showProgress) R.string.gleo_loading_trip_parked else R.string.gleo_loading_trip
        GleoCommandPhase.PROCESSING -> R.string.gleo_processing
        GleoCommandPhase.SAVED -> R.string.gleo_saved
        GleoCommandPhase.INVALID_REQUEST -> R.string.gleo_invalid_request
        GleoCommandPhase.NOT_READY -> R.string.gleo_not_ready
        GleoCommandPhase.CONSENT_REQUIRED -> R.string.gleo_consent_required
        GleoCommandPhase.FAILED -> R.string.gleo_failed
        GleoCommandPhase.UNCONFIRMED -> R.string.gleo_unconfirmed
    }
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().height(Dimens.NoticeLineHeight),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showProgress) CircularProgressIndicator(modifier = Modifier.size(Dimens.IconSize))
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
