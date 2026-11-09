package com.example.travelcrumbs.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.theme.Dimens

/**
 * 좌측 레일 — 목적지 목록 4칸 그대로(스펙 app-shell-v5 2-1 · D33). 정차 · 주행 · 모름 모두 보이고 눌림(D29 · D37).
 * 칸마다 한 번 터치 · 길게 누르기 없음 · 터치 영역은 [Dimens.MinTouchTarget] 이상 · 글자 5 mm 이상 · 한 줄(줄바꿈 · 잘림 없음).
 * 시각 규칙(디자인 시스템 10절 · D38): 아이콘 + 글자 · 아이콘 [Dimens.RailIconSize](32dp 이상) ·
 * 기본 칸 투명 배경 + 흰 아이콘 · 글자 · 선택 칸은 칸 배경 · 아이콘 · 글자가 함께 바뀜(N29).
 * 색은 테마 — 바탕 background · 선택 칸 secondaryContainer(Background/Selected) + secondary(Brand/Secondary) · 기본 칸 onBackground(#34).
 */
@Composable
fun AppNavigationRail(
    highlighted: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight().width(Dimens.RailWidth),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        AppDestination.railItems.forEach { destination ->
            val selected = destination == highlighted
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(Dimens.RailIconSize)
                            .testTag(railIconTag(destination)),
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag(railLabelTag(destination)),
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    // 선택 배경은 알약 모양이 아니라 칸 전체(아래 background) — 알약은 투명
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier
                    .testTag(railItemTag(destination))
                    .padding(horizontal = Dimens.RailItemInset)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .widthIn(min = Dimens.MinTouchTarget)
                    .heightIn(min = Dimens.RailItemHeight),
            )
        }
    }
}

/** 레일 칸 · 글자 이름표 — 테스트가 칸 · 글자 크기를 잴 때 씀. */
fun railItemTag(destination: AppDestination): String = "rail_${destination.id}"

fun railLabelTag(destination: AppDestination): String = "rail_label_${destination.id}"

fun railIconTag(destination: AppDestination): String = "rail_icon_${destination.id}"
