package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsColors

data class ExperienceTagChoice(val id: Long, val label: String, val selected: Boolean)

fun isNegativeExperience(label: String): Boolean =
    listOf("길어요", "어려워요", "불편해요", "아쉬워요").any(label::contains)

@Composable
fun ExperienceTagChip(choice: ExperienceTagChoice, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val negative = isNegativeExperience(choice.label)
    val colors = MaterialTheme.colorScheme
    val selectedBackground = if (negative) TravelCrumbsColors.NegativeSelected else TravelCrumbsColors.PositiveSelected
    val selectedContent = if (negative) TravelCrumbsColors.NegativeText else colors.primary
    val borderColor = if (negative) TravelCrumbsColors.NegativeBorder else colors.primary
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.ChipHeight).semantics { selected = choice.selected },
        shape = CircleShape,
        color = if (choice.selected) selectedBackground else colors.surface,
        contentColor = if (choice.selected) selectedContent else if (negative) TravelCrumbsColors.NegativeText else colors.primary,
        border = BorderStroke(if (choice.selected) Dimens.SelectedBorderWidth else Dimens.CardBorderWidth, borderColor),
    ) {
        Text(
            text = if (choice.selected) "✓ ${choice.label}" else choice.label,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ChipVerticalPadding),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExperienceTagPickerDialog(
    tags: List<ExperienceTagChoice>,
    onDismiss: () -> Unit,
    onComplete: (Set<Long>) -> Unit,
) {
    var selectedIds by remember(tags) { mutableStateOf(tags.filter(ExperienceTagChoice::selected).mapTo(mutableSetOf()) { it.id }.toSet()) }
    AppDialog(
        title = stringResource(R.string.experience_more_title),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(R.string.action_selection_complete),
        onDismiss = onDismiss,
        onConfirm = { onComplete(selectedIds) },
        titleMaxLines = 1,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            tags.forEach { tag ->
                ExperienceTagChip(
                    choice = tag.copy(selected = tag.id in selectedIds),
                    onClick = {
                        selectedIds = if (tag.id in selectedIds) selectedIds - tag.id else selectedIds + tag.id
                    },
                )
            }
        }
    }
}
