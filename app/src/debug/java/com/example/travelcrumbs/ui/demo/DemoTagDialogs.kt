package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExperienceTagChips(stop: DemoStop, selected: List<String>, onSelect: (List<String>) -> Unit) {
    val resource = when {
        stop.navigationTarget?.id == "seoul-forest" || stop.category == "산책" -> R.array.demo_tags_park
        stop.navigationTarget?.id == "gyeongbokgung" || stop.category == "문화" -> R.array.demo_tags_culture
        stop.navigationTarget?.id == "haeundae-beach" || stop.category == "바다" -> R.array.demo_tags_beach
        stop.navigationTarget?.id == "yongduam" || stop.category == "전망" -> R.array.demo_tags_scenic
        stop.category == "카페" -> R.array.demo_tags_cafe
        stop.category == "야경" -> R.array.demo_tags_night
        else -> R.array.demo_tags_general
    }
    val options = (stringArrayResource(resource).asList() + selected).distinct()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { tag ->
            FilterChip(
                selected = tag in selected,
                onClick = { onSelect(if (tag in selected) selected - tag else selected + tag) },
                label = { Text(tag) },
            )
        }
    }
}

@Composable
internal fun GuidedDestinationSaveDialog(
    stop: DemoStop,
    recording: Boolean,
    onSave: (List<String>?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(stop) { mutableStateOf(stop.tags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DemoTitle(stringResource(R.string.demo_save_destination_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        if (recording) R.string.demo_save_destination_in_trip else R.string.demo_save_destination_for_later,
                        stop.name,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(stringResource(R.string.demo_tag_optional))
                ExperienceTagChips(stop, selected) { selected = it }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoAction(stringResource(R.string.demo_save_with_tags), { onSave(selected) }, enabled = selected.isNotEmpty())
                DemoAction(stringResource(R.string.demo_save_without_tags), { onSave(null) }, primary = false)
                DemoAction(stringResource(R.string.demo_do_not_save), onDismiss, primary = false)
            }
        },
    )
}

@Composable
internal fun RecordTagDialog(stop: DemoStop, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(stop) { mutableStateOf(stop.tags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DemoTitle(stringResource(R.string.demo_tag_place_title, stop.name)) },
        text = { ExperienceTagChips(stop, selected) { selected = it } },
        confirmButton = { DemoAction(stringResource(R.string.demo_save_tags), { onSave(selected) }) },
        dismissButton = { DemoAction(stringResource(android.R.string.cancel), onDismiss, primary = false) },
    )
}

@Composable
internal fun DemoPlaceTagRow(stop: DemoStop, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stop.name, style = MaterialTheme.typography.titleMedium)
            if (stop.tags.isNotEmpty()) Text(stop.tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
        }
        val action = if (stop.tags.isEmpty()) R.string.demo_add_tags else R.string.demo_edit_tags
        Text(stringResource(action), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun TripExperienceDialog(stop: DemoStop, onSave: (List<String>, String) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(stop) { mutableStateOf(stop.tags) }
    var memo by remember(stop) { mutableStateOf(stop.memo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DemoTitle(stringResource(R.string.demo_tag_place_title, stop.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExperienceTagChips(stop, selected) { selected = it }
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(120) },
                    label = { Text(stringResource(R.string.demo_place_memo)) },
                    supportingText = { Text("${memo.length} / 120") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { DemoAction(stringResource(R.string.demo_apply_place_experience), { onSave(selected, memo) }) },
        dismissButton = { DemoAction(stringResource(android.R.string.cancel), onDismiss, primary = false) },
    )
}
