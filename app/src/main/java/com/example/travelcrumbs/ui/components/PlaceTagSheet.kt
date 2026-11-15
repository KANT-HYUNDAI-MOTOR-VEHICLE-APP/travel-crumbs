package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

/** A-03 시트가 그릴 것(#51 U4) — 좌표 · 주소 없음. [editing] = 이미 저장한 장소를 고침(A-05 · A-08). */
data class PlaceTagSheetItem(
    val tripPlaceNo: Long,
    val editing: Boolean,
    val userPlaceNm: String,
    val memo: String,
    val memoTooLong: Boolean = false,
    val recordedHour: Int,
    val recordedMinute: Int,
    val tags: List<TagChipItem>,
)

data class TagChipItem(val tagNo: Long, val tagNm: String, val selected: Boolean)

/**
 * A-03 장소 기록 · 태그 시트(와이어프레임 v6) — 정차 화면에서만 뜸(주행 중엔 앱 틀이 이 화면을 그리지 않음).
 * 위에서 아래로 제목 · 기록 시각 · 「어땠나요?」 태그 칩 · 장소 이름 · 메모(선택) · 안내 한 줄 · 「건너뛰기 · 나중에」(보조) · 「저장」(주요).
 * 팝업 틀 [AppDialog] — 가운데 · 폭 제한 · 뒤 화면 어둡게 · 내용이 길면 세로 스크롤 · 키보드에 입력 칸이 가려지지 않음.
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
    AppDialog(
        title = stringResource(if (item.editing) R.string.tag_sheet_title_edit else R.string.tag_sheet_title),
        dismissLabel = stringResource(R.string.tag_sheet_skip),
        confirmLabel = stringResource(R.string.action_save),
        onDismiss = onSkip,
        onConfirm = onSave,
        confirmEnabled = !item.memoTooLong,
        titleMaxLines = 1,
    ) {
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
                ExperienceTagChip(
                    choice = ExperienceTagChoice(tag.tagNo, tag.tagNm, tag.selected),
                    onClick = { onToggleTag(tag.tagNo) },
                )
            }
        }
        AppTextField(
            value = item.userPlaceNm,
            onValueChange = onNameChange,
            label = stringResource(R.string.tag_sheet_name_label),
            singleLine = false,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = item.memo,
            onValueChange = onMemoChange,
            label = stringResource(R.string.tag_sheet_memo_label),
            singleLine = false,
            minLines = 2,
            maxLines = 5,
            isError = item.memoTooLong,
            errorText = if (item.memoTooLong) stringResource(R.string.input_memo_error) else null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
