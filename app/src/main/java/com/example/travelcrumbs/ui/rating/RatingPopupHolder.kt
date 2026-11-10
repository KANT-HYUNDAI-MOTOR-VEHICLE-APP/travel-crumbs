package com.example.travelcrumbs.ui.rating

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.domain.RecommendationRules
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.RatingRecords
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.ui.components.RatingDialogItem
import com.example.travelcrumbs.ui.components.RatingDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 별점 팝업(#41 B-06) — ViewModel 이 소유하는 상태 도우미. 기록 칸(자동 팝업) · 추천 B-04(버튼으로 엶) ViewModel 이 같은 로직을 씀.
 * ViewModel 이 자기 [scope] · [savedStateHandle] 로 하나씩 만듦(화면 단위 · Android 화면 클래스 없음).
 * 고르는 중 값(로드맵 · 별 · 태그)은 [savedStateHandle] 에 둬 프로세스 재생성 뒤에도 그대로.
 * 어떤 팝업을 보일지는 순수 계산 [shownDraft] · 결과(저장됨)는 [onSaved] 로 ViewModel 의 상태 칸에.
 *
 * [autoPrompt] — 참이면 연 팝업이 없을 때 「여행 종료 뒤 · 아직 안 물은」 로드맵을 자동으로 보임(기록 칸).
 */
class RatingPopupHolder(
    private val scope: CoroutineScope,
    private val repository: RatingRepository,
    private val savedStateHandle: SavedStateHandle,
    private val timeSource: TimeSource,
    private val autoPrompt: Boolean,
    loadTags: suspend () -> List<Tag>,
    private val onSaved: () -> Unit,
) {
    private val draft = MutableStateFlow(restore())
    private val saving = MutableStateFlow(false)
    private val tags = MutableStateFlow<List<Tag>>(emptyList())

    init {
        scope.launch { tags.value = loadTags() }
    }

    /** 지금 보일 팝업 — 없으면 null. */
    val item: Flow<RatingDialogItem?> = combine(repository.records, draft, saving, tags) { records, current, isSaving, tagList ->
        shownDraft(records, current, autoPrompt)?.let { shown ->
            records.guided(shown.tripNo)?.let { guided -> shown.toItem(guided, tagList, timeSource.zoneId(), isSaving) }
        }
    }

    /** B-04 「별점 주기」 · 「고치기」 — 안내받은 로드맵만 · 이미 준 별점이면 그 값으로 채움. */
    fun open(tripNo: Long) {
        scope.launch {
            val records = repository.records.first()
            if (RecommendationRules.canRate(records, tripNo)) setDraft(RatingDraft.of(tripNo, records.ratingOf(tripNo)))
        }
    }

    /** 별 고르기 — [shown] 은 화면에 보이는 팝업(ViewModel 의 uiState 값). */
    fun selectScore(shown: RatingDialogItem?, score: Int) {
        setDraft(base(shown ?: return).copy(ratingScore = score))
    }

    fun toggleTag(shown: RatingDialogItem?, tagNo: Long) {
        setDraft(base(shown ?: return).toggleTag(tagNo))
    }

    /** 「나중에」 · 뒤로 — 닫고 그 로드맵의 자동 팝업도 끝난 것으로(로드맵마다 한 번). */
    fun dismiss(shown: RatingDialogItem?) {
        shown ?: return
        setDraft(null)
        scope.launch { repository.markPrompted(shown.tripNo) }
    }

    /** 「별점 주기」 — 저장되면 닫고 [onSaved]. 저장 중엔 다시 받지 않음. */
    fun submit(shown: RatingDialogItem?) {
        shown ?: return
        if (saving.value) return
        val chosen = base(shown)
        val score = chosen.ratingScore ?: return
        saving.value = true
        scope.launch {
            if (repository.saveRating(chosen.tripNo, score, chosen.tagNos)) {
                setDraft(null)
                onSaved()
            }
            saving.value = false
        }
    }

    /** 다른 진입(추천 칸 처음부터) — 연 팝업만 닫음(자동 팝업 기록은 그대로). */
    fun clear() = setDraft(null)

    /** 고르는 중 값 — 같은 로드맵이면 바로 앞 행동이 남긴 값(화면 상태가 아직 안 바뀌었어도) · 아니면 보이는 팝업 값. */
    private fun base(shown: RatingDialogItem): RatingDraft =
        draft.value?.takeIf { it.tripNo == shown.tripNo } ?: shown.toDraft()

    private fun setDraft(next: RatingDraft?) {
        draft.value = next
        savedStateHandle[KEY_TRIP] = next?.tripNo
        savedStateHandle[KEY_SCORE] = next?.ratingScore
        savedStateHandle[KEY_TAGS] = next?.tagNos?.sorted()?.toLongArray()
    }

    private fun restore(): RatingDraft? {
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return null
        return RatingDraft(
            tripNo = tripNo,
            ratingScore = savedStateHandle.get<Int>(KEY_SCORE),
            tagNos = savedStateHandle.get<LongArray>(KEY_TAGS)?.toSet().orEmpty(),
        )
    }

    companion object {
        private const val KEY_TRIP = "rating_trip_no"
        private const val KEY_SCORE = "rating_score"
        private const val KEY_TAGS = "rating_tag_nos"

        /**
         * 보일 팝업의 고르는 중 값(순수) — 연 팝업이 있고 그 로드맵 안내 기록이 있으면 그것 ·
         * 아니고 [autoPrompt] 면 자동 팝업 대상(빈 값) · 아니면 null.
         */
        fun shownDraft(records: RatingRecords, draft: RatingDraft?, autoPrompt: Boolean): RatingDraft? {
            if (draft != null && records.guided(draft.tripNo) != null) return draft
            if (!autoPrompt) return null
            return RecommendationRules.pendingRatingPrompt(records)?.let { RatingDraft(it.tripNo) }
        }

        private fun RatingDialogItem.toDraft() = RatingDraft(
            tripNo = tripNo,
            ratingScore = ratingScore,
            tagNos = tags.filter { it.selected }.map { it.tagNo }.toSet(),
        )
    }
}
