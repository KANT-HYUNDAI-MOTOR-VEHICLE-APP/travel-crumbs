package com.example.travelcrumbs.ui.components

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A-03 장소 기록 · 태그 시트(와이어프레임 v6 · #51 U4) — ViewModel 이 소유하는 상태 도우미(기록 칸 · A-05 검토 · A-08 상세가 같은 로직).
 * 고르는 중 값(장소 · 이름 · 메모 · 태그)은 [savedStateHandle] 에 둬 프로세스 재생성 뒤에도 그대로.
 * 「저장」 → 이름 · 메모 · 태그 저장 + 임시저장 해제 · 「건너뛰기 · 나중에」 → 닫기만(임시저장 그대로 · A-05 에서 채움).
 */
class PlaceTagSheetHolder(
    private val scope: CoroutineScope,
    private val trips: TripRepository,
    tags: TagRepository,
    private val savedStateHandle: SavedStateHandle,
    private val timeSource: TimeSource,
    private val onSaved: (UiMessage) -> Unit,
) {
    private val draft = MutableStateFlow(restore())

    /** 지금 보일 시트 — 없으면 null. 장소가 지워졌으면(다른 화면) 보이지 않음. */
    val item: Flow<PlaceTagSheetItem?> = combine(draft, trips.trips, tags.tags) { current, list, tagList ->
        current ?: return@combine null
        val place = list.asSequence().flatMap { it.places.asSequence() }.firstOrNull { it.tripPlaceNo == current.tripPlaceNo }
            ?: return@combine null
        val time = Instant.ofEpochMilli(place.recordedAt).atZone(timeSource.zoneId()).toLocalTime()
        PlaceTagSheetItem(
            tripPlaceNo = current.tripPlaceNo,
            editing = !place.draft,
            userPlaceNm = current.userPlaceNm,
            memo = current.memo,
            recordedHour = time.hour,
            recordedMinute = time.minute,
            tags = tagList.map { TagChipItem(it.tagNo, it.tagNm, it.tagNo in current.tagNos) },
        )
    }

    /** 그 장소로 시트를 엶 — 지금 저장된 이름 · 메모 · 태그로 채움. */
    fun open(tripPlaceNo: Long) {
        scope.launch {
            val place = trips.trips.first().asSequence().flatMap { it.places.asSequence() }
                .firstOrNull { it.tripPlaceNo == tripPlaceNo } ?: return@launch
            setDraft(Draft(tripPlaceNo, place.userPlaceNm.orEmpty(), place.memo.orEmpty(), place.tagNos.toSet()))
        }
    }

    fun onNameChange(value: String) = edit { it.copy(userPlaceNm = value.take(AppConfig.PLACE_NAME_MAX_LENGTH)) }

    fun onMemoChange(value: String) = edit { it.copy(memo = value.take(AppConfig.MEMO_MAX_LENGTH)) }

    fun toggleTag(tagNo: Long) = edit { it.copy(tagNos = if (tagNo in it.tagNos) it.tagNos - tagNo else it.tagNos + tagNo) }

    /** 「건너뛰기 · 나중에」 · 바깥 · 뒤로 — 닫기만. */
    fun skip() = setDraft(null)

    /** 「저장」 — 저장하고 닫음 · 결과 한 줄. */
    fun save() {
        val current = draft.value ?: return
        setDraft(null)
        scope.launch {
            trips.savePlace(current.tripPlaceNo, current.userPlaceNm, current.memo, current.tagNos)
            onSaved(UiMessage.PLACE_SAVED)
        }
    }

    private fun edit(change: (Draft) -> Draft) {
        draft.value?.let { setDraft(change(it)) }
    }

    private fun setDraft(next: Draft?) {
        draft.value = next
        savedStateHandle[KEY_PLACE] = next?.tripPlaceNo
        savedStateHandle[KEY_NAME] = next?.userPlaceNm
        savedStateHandle[KEY_MEMO] = next?.memo
        savedStateHandle[KEY_TAGS] = next?.tagNos?.sorted()?.toLongArray()
    }

    private fun restore(): Draft? {
        val no = savedStateHandle.get<Long>(KEY_PLACE) ?: return null
        return Draft(
            tripPlaceNo = no,
            userPlaceNm = savedStateHandle.get<String>(KEY_NAME).orEmpty(),
            memo = savedStateHandle.get<String>(KEY_MEMO).orEmpty(),
            tagNos = savedStateHandle.get<LongArray>(KEY_TAGS)?.toSet().orEmpty(),
        )
    }

    private data class Draft(val tripPlaceNo: Long, val userPlaceNm: String, val memo: String, val tagNos: Set<Long>)

    private companion object {
        const val KEY_PLACE = "tag_sheet_trip_place_no"
        const val KEY_NAME = "tag_sheet_user_place_nm"
        const val KEY_MEMO = "tag_sheet_memo"
        const val KEY_TAGS = "tag_sheet_tag_nos"
    }
}
