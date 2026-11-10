package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 하위 화면(레일 칸 첫 화면 아래 · 정차 전용) 머리글 맨 왼쪽의 「← 뒤로」(#51 사람 요청 — 차량에선 시스템 뒤로 키가 잘 안 보임).
 * [onBack] 은 그 화면의 시스템 뒤로(BackHandler)와 같은 함수 하나 — 뒤로 규칙이 두 군데로 갈리지 않게.
 * 레일 칸 첫 화면 · 주행 화면 · 주행 제한 안내엔 두지 않음(주행 중 조작 줄임).
 * 모양은 디자인 시스템 12절 Secondary · 높이는 주요 버튼 72 이상(7절 · [Dimens.HomeButtonHeight]).
 */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SecondaryButton(
        onClick = onBack,
        modifier = modifier
            .heightIn(min = Dimens.HomeButtonHeight)
            .testTag(BACK_BUTTON_TAG),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            modifier = Modifier.size(Dimens.RailIconSize),
        )
        Text(
            text = stringResource(R.string.action_back),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(start = Dimens.CardContentSpacing),
        )
    }
}

/** 테스트가 「뒤로」 버튼을 찾는 이름표. */
const val BACK_BUTTON_TAG = "back_button"
