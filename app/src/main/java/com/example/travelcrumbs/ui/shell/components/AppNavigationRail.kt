package com.example.travelcrumbs.ui.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsColors

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
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val compact = maxHeight < 1000.dp
        val railWidth = if (compact) 120.dp else Dimens.RailWidth
        val itemHeight = if (compact) 96.dp else Dimens.RailItemHeight
        val itemSpacing = if (compact) 4.dp else Dimens.RailItemSpacing
        val topPadding = if (compact) 4.dp else Dimens.RailTopPadding
        val sidePadding = if (compact) 20.dp else Dimens.RailItemInset
        val iconSize = if (compact) 32.dp else Dimens.RailIconSize
        val labelSize = if (compact) 20.sp else 48.sp
        val labelLine = if (compact) 28.sp else 64.sp
        Column(
            modifier = Modifier.fillMaxHeight().width(railWidth).padding(start = sidePadding, end = sidePadding, top = topPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppDestination.railItems.forEach { destination ->
                val selected = destination == highlighted
                Surface(
                    onClick = { onSelect(destination) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .semantics { this.selected = selected }
                        .testTag(railItemTag(destination)),
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) TravelCrumbsColors.NavigationSelected else MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).testTag(railIconTag(destination)),
                        )
                        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = labelSize,
                                lineHeight = labelLine,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag(railLabelTag(destination)),
                        )
                    }
                }
            }
        }
    }
}

/** 레일 칸 · 글자 이름표 — 테스트가 칸 · 글자 크기를 잴 때 씀. */
fun railItemTag(destination: AppDestination): String = "rail_${destination.id}"

fun railLabelTag(destination: AppDestination): String = "rail_label_${destination.id}"

fun railIconTag(destination: AppDestination): String = "rail_icon_${destination.id}"
