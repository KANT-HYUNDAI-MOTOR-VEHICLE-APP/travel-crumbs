package com.example.travelcrumbs.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TcColors

/** 버튼 종류 — 지시서 12장. Primary 는 한 화면의 주요 행동 1개에만. */
enum class TcButtonKind { PRIMARY, SECONDARY, DANGER, TEXT }

/**
 * 디자인 시스템 버튼 — 아이콘 + 글자 · 높이 72 이상 · 모서리 16.
 * 상태: Default · Pressed(물결) · Disabled · Loading([loading] 이면 돌아가는 표시 + 누를 수 없음).
 */
@Composable
fun TcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: TcButtonKind = TcButtonKind.SECONDARY,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    minHeight: Dp = Dimens.PrimaryButtonHeight,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val container = when (kind) {
        TcButtonKind.PRIMARY -> TcColors.BrandPrimary
        TcButtonKind.SECONDARY -> TcColors.BackgroundSurface
        TcButtonKind.DANGER, TcButtonKind.TEXT -> Color.Transparent
    }
    val content = when (kind) {
        TcButtonKind.PRIMARY -> TcColors.TextOnPrimary
        TcButtonKind.SECONDARY, TcButtonKind.TEXT -> TcColors.TextPrimary
        TcButtonKind.DANGER -> TcColors.Danger
    }
    val active = enabled && !loading
    val borderColor = when {
        kind == TcButtonKind.SECONDARY -> if (active) TcColors.BorderDefault else TcColors.BorderSubtle
        kind == TcButtonKind.DANGER -> if (active) TcColors.Danger else TcColors.BorderSubtle
        else -> null
    }
    Button(
        onClick = onClick,
        enabled = active,
        modifier = modifier.heightIn(min = minHeight),
        shape = RoundedCornerShape(Dimens.ButtonCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (kind == TcButtonKind.PRIMARY) TcColors.BackgroundElevated else container,
            disabledContentColor = TcColors.TextMuted,
        ),
        border = borderColor?.let { BorderStroke(Dimens.BorderWidth, it) },
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.IconSize),
                color = TcColors.TextSecondary,
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.width(Dimens.ItemSpacingSmall))
        } else if (iconRes != null) {
            Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(Dimens.IconSize))
            Spacer(modifier = Modifier.width(Dimens.ItemSpacingSmall))
        }
        Text(text = text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
