package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.recommendations.components.RecoChip
import com.example.travelcrumbs.ui.theme.Dimens

/** A-03 시트가 그릴 것(#51 U4) — 좌표 · 주소 없음. [editing] = 이미 저장한 장소를 고침(A-05 · A-08). */
data class PlaceTagSheetItem(
    val tripPlaceNo: Long,
    val editing: Boolean,
    val userPlaceNm: String,
    val memo: String,
    val recordedHour: Int,
    val recordedMinute: Int,
    val tags: List<TagChipItem>,
)

data class TagChipItem(val tagNo: Long, val tagNm: String, val selected: Boolean)

/**
 * A-03 장소 기록 · 태그 시트(와이어프레임 v6) — 정차 화면에서만 뜸(주행 중엔 앱 틀이 이 화면을 그리지 않음).
 * 위에서 아래로 제목 · 기록 시각 · 「어땠나요?」 태그 칩 · 장소 이름 · 메모(선택) · 안내 한 줄 · 「건너뛰기 · 나중에」(보조) · 「저장」(주요).
 * 좁은 칸에서도 들어가게 폭은 창에 맞추고 세로로 스크롤.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceTagSheet(
    item: PlaceTagSheetItem,
    onNameChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onSkip: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.widthIn(max = Dimens.RatingDialogMaxWidth).padding(Dimens.ScreenPadding),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                Text(
                    text = stringResource(if (item.editing) R.string.tag_sheet_title_edit else R.string.tag_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.tag_sheet_recorded_at, item.recordedHour, item.recordedMinute),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.tag_sheet_how),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    item.tags.forEach { tag ->
                        RecoChip(label = tag.tagNm, selected = tag.selected, onClick = { onToggleTag(tag.tagNo) })
                    }
                }
                OutlinedTextField(
                    value = item.userPlaceNm,
                    onValueChange = onNameChange,
                    label = { Text(text = stringResource(R.string.tag_sheet_name_label)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = item.memo,
                    onValueChange = onMemoChange,
                    label = { Text(text = stringResource(R.string.tag_sheet_memo_label)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.tag_sheet_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                    SecondaryButton(onClick = onSkip, modifier = Modifier.weight(1f).height(Dimens.HomeButtonHeight)) {
                        Text(text = stringResource(R.string.tag_sheet_skip), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                    PrimaryButton(onClick = onSave, modifier = Modifier.weight(1f).height(Dimens.HomeButtonHeight)) {
                        Text(text = stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
