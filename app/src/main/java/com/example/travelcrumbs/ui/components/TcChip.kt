package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TcColors

/**
 * 선택 칩 — 지시서 13장(동행자 · 경험 태그 · 장소 유형 등).
 * Default: 투명 + 기본 테두리 · Selected: Brand/Secondary 바탕 + 짙은 글자 + 체크 · Disabled: 낮은 명도.
 * 색만으로 구분하지 않도록 선택되면 체크 아이콘을 함께 보임.
 */
@Composable
fun TcChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(percent = 50)
    val textColor = when {
        !enabled -> TcColors.TextMuted
        selected -> TcColors.TextOnPrimary
        else -> TcColors.TextPrimary
    }
    Row(
        modifier = modifier
            .heightIn(min = Dimens.TouchMin)
            .clip(shape)
            .background(if (selected) TcColors.BrandSecondary else Color.Transparent)
            .border(
                width = Dimens.BorderWidth,
                color = when {
                    !enabled -> TcColors.BorderSubtle
                    selected -> TcColors.BrandSecondary
                    else -> TcColors.BorderDefault
                },
                shape = shape,
            )
            .selectable(selected = selected, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = textColor, maxLines = 1)
    }
}
