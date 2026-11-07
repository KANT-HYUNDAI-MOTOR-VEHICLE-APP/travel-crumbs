package com.example.travelcrumbs.ui.shell.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 좌측 레일 — 목적지 목록의 레일 칸 5개 그대로(스펙 app-shell-v5 2-1). 정차 · 주행 · 모름 모두 보이고 눌림(D29).
 * 칸마다 한 번 터치 · 길게 누르기 없음 · 터치 영역은 [Dimens.MinTouchTarget] 이상.
 */
@Composable
fun AppNavigationRail(
    highlighted: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier.fillMaxHeight()) {
        AppDestination.railItems.forEach { destination ->
            NavigationRailItem(
                selected = destination == highlighted,
                onClick = { onSelect(destination) },
                icon = { Icon(painter = painterResource(destination.iconRes), contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                modifier = Modifier
                    .testTag(railItemTag(destination))
                    .widthIn(min = Dimens.MinTouchTarget)
                    .heightIn(min = Dimens.RailItemHeight),
            )
        }
    }
}

/** 레일 칸 이름표 — 테스트가 칸 크기를 잴 때 씀. */
fun railItemTag(destination: AppDestination): String = "rail_${destination.id}"
