package com.example.travelcrumbs.ui.shell.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.components.NoticePill
import com.example.travelcrumbs.ui.components.NoticeTone
import com.example.travelcrumbs.ui.components.ScreenHeader

/**
 * 주행 제한 안내(스펙 app-shell-v5 2-8) — 주행 · 모름 중 「기록」 밖 칸의 내용 대신. 제목 + 「정차 후 볼 수 있어요」 한 줄.
 * 그 칸의 정차 내용(목록 · 입력 칸 · 스크롤 · 버튼 · 기능 구역 · debug 카드)은 하나도 그리지 않음 · 앱 틀 상태만으로 그림.
 */
@Composable
fun DrivingRestrictedContent(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Dimens.ScreenContentStart, end = Dimens.ScreenContentEnd, top = Dimens.ScreenTop),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        ScreenHeader(title = stringResource(titleRes), parked = false, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            NoticePill(text = stringResource(R.string.driving_restricted), tone = NoticeTone.Neutral)
        }
    }
}
