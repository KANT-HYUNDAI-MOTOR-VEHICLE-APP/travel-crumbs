package com.example.travelcrumbs.ui.home.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.R

/*
 * 길안내 연결 화면의 시연용 부품 — debug 소스 세트(스펙 navigation 2.3 G1-D · G3 · G5 · 결정 기록 U55 · U70).
 * release 소스 세트엔 같은 이름의 빈 함수만 있어 release APK 에 더미 경로 문구 · 버튼이 들어가지 않음(심사 A1).
 * 보이는지는 화면 상태(dummyRouteAvailable · dummyRouteVisible)가 따로 정함 — 시험이 release 를 흉내 낼 수 있게.
 */

/** G5 「더미 경로」 — 「다시 시도」 오른쪽 · 같은 크기. */
@Composable
fun DummyRouteButton(enabled: Boolean, onClick: () -> Unit) {
    GuideActionButton(text = stringResource(R.string.guide_dummy_route), enabled = enabled, onClick = onClick)
}

/** G1-D 시연용 표시 — 지도 박스 맨 위 한 줄. */
@Composable
fun DummyRouteNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.guide_dummy_notice),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** G3 왼쪽 글자 「연동 실패 시 더미 경로 표시」. */
@Composable
fun FallbackLabel() {
    Text(
        text = stringResource(R.string.guide_fallback_label),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
