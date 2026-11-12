package com.example.travelcrumbs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R

/** 화면 맨 위 한 줄 「시연용 데이터」 — 시드를 쓰는 동안 모든 빌드에서(공모전 제출 APK 가 release · 사람 결정). 추천 칸 머리 · 홈 근처 로드맵 구역(#55)이 같이 씀. */
@Composable
fun DemoDataLine(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.reco_demo_data),
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
