package com.example.travelcrumbs.ui.driving.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.components.AppCard
import com.example.travelcrumbs.ui.components.SecondaryButton
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 주행 화면 넓은 칸의 「내비 안내」 카드(#51 U0) — 가짜 지도 대신. 내비가 안내 중이면 목적지 이름 · 아니면 길안내를 켜 보라는 한 줄 ·
 * 「내비 보기」(내비 앱이 있을 때만 · 보조 버튼 · 주행 버튼 4개 규칙 D31 과 별개) · 나란히 보기 안내 한 줄. 움직임 없음.
 */
@Composable
fun NaviGuideCard(
    destinationName: String?,
    naviAppAvailable: Boolean,
    onOpenNavi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.driving_navi_card_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = destinationName?.let { stringResource(R.string.driving_navi_guiding, it) }
                ?: stringResource(R.string.driving_navi_idle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (naviAppAvailable) {
            SecondaryButton(onClick = onOpenNavi, modifier = Modifier.height(Dimens.HomeButtonHeight)) {
                Text(text = stringResource(R.string.action_open_navi), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        Text(
            text = stringResource(R.string.navi_split_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}
