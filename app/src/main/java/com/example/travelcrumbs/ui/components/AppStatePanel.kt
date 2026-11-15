package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelcrumbs.ui.theme.Dimens

enum class AppStateKind { Empty, Loading, Error, Disabled }

/** v1.8 빈 상태·로딩·실패·비활성 상태가 공유하는 차량용 상태 카드. */
@Composable
fun AppStatePanel(
    title: String,
    kind: AppStateKind,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (kind == AppStateKind.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.RailIconSize), color = colors.primary)
            } else {
                Text(
                    text = when (kind) {
                        AppStateKind.Empty -> "—"
                        AppStateKind.Error -> "!"
                        AppStateKind.Disabled -> "○"
                        AppStateKind.Loading -> ""
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (kind == AppStateKind.Error) colors.error else colors.primary,
                    maxLines = 1,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
