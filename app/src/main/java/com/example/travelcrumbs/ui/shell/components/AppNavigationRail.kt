package com.example.travelcrumbs.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TcColors

/**
 * 좌측 레일 — 목적지 목록 그대로(스펙 app-shell 2-1 · 디자인 시스템 지시서 10장).
 * 기본: 투명 바탕 + 흰 아이콘 · 선택: Background/Selected 바탕 + Brand/Secondary 아이콘 · 글자(셋 다 바뀜).
 * 아이콘 + 메뉴명을 함께 보임.
 */
@Composable
fun AppNavigationRail(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(TcColors.BackgroundBase)
            .padding(horizontal = Dimens.ItemSpacingSmall, vertical = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            val tint = if (isSelected) TcColors.BrandSecondary else TcColors.TextPrimary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = RAIL_ITEM_MIN_HEIGHT)
                    .clip(RoundedCornerShape(Dimens.CardCorner))
                    .background(if (isSelected) TcColors.BackgroundSelected else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(destination) })
                    .padding(vertical = Dimens.ItemSpacingSmall, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(Dimens.RailIconSize),
                )
                Text(
                    text = stringResource(destination.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private val RAIL_ITEM_MIN_HEIGHT = 96.dp
