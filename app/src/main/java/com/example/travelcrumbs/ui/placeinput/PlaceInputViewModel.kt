package com.example.travelcrumbs.ui.placeinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.place.PlaceRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.PlaceCandidates
import com.example.travelcrumbs.domain.model.CodeItem
import com.example.travelcrumbs.domain.model.NaviPlace
import com.example.travelcrumbs.domain.model.PlaceInput
import com.example.travelcrumbs.ui.components.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 장소 경험 입력(A-06) — 임시 기록(draft_yn = Y) 하나에 장소명 · 유형 · 좋았어요/별로예요 · 태그 · 메모를 채움.
 * 장소 유형은 장소유형 테이블(카테고리 → 유형) · 장소명은 기록한 위치 근처 내비 장소를 먼저 후보로 보여 주고, 없으면 직접 입력(결정 로그 D19 · D20).
 * 「저장」 = 완성(draft_yn = N) · 「나중에 입력」 = 임시 기록 유지.
 */
@HiltViewModel
class PlaceInputViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val placeRepository: PlaceRepository,
    private val codeRepository: CodeRepository,
    private val naviRepository: NaviRepository,
) : ViewModel() {

    private val state = MutableStateFlow(PlaceInputUiState())
    val uiState: StateFlow<PlaceInputUiState> = state.asStateFlow()

    /** 화면이 열릴 때 — 가장 먼저 기록한 임시 기록부터. 지난 값은 지움. */
    fun prepare() {
        state.value = PlaceInputUiState()
        viewModelScope.launch {
            val trip = tripRepository.activeTrip.value
            if (trip == null) {
                state.update { it.copy(loading = false, closed = true) }
                return@launch
            }
            val drafts = async { placeRepository.refreshDrafts(trip.tripUuid) }
            val types = async { placeRepository.placeTypes() }
            val polarities = async { codeRepository.codes(REVIEW_POLARITY) }
            val tags = async { placeRepository.tags() }
            val candidates = async { runCatching { naviRepository.placeCandidates() }.getOrDefault(emptyList()) }

            val draftList = drafts.await().getOrNull()
            val typeList = types.await().getOrNull()
            val polarityList = polarities.await().getOrNull()
            val tagList = tags.await().getOrNull()
            if (draftList == null || typeList == null || polarityList == null || tagList == null) {
                state.update { it.copy(loading = false, loadFailed = true, message = UiMessage.SERVER_UNAVAILABLE) }
                return@launch
            }
            val draft = draftList.firstOrNull()
            state.update {
                it.copy(
                    loading = false,
                    draft = draft,
                    draftCount = draftList.size,
                    candidates = draft?.let { d -> PlaceCandidates.near(d.lat, d.lng, candidates.await()) }.orEmpty(),
                    // 카테고리는 장소유형 목록에서 순서대로 한 번씩
                    categories = typeList.distinctBy { t -> t.categoryCode }
                        .map { t -> CodeItem(t.categoryCode, t.categoryName) },
                    placeTypes = typeList.map { t -> CodeItem(t.code, t.name, t.categoryCode) },
                    polarities = polarityList,
                    tags = tagList,
                    closed = draft == null,
                )
            }
        }
    }

    fun onRetry() = prepare()

    /** 내비 후보를 누르면 이름을 채우고 공식 장소로 연결 · 다시 누르면 연결만 풀고 이름은 둠. */
    fun onSelectCandidate(place: NaviPlace) {
        state.update { s ->
            if (s.selectedCandidate == place) {
                s.copy(selectedCandidate = null)
            } else {
                s.copy(selectedCandidate = place, placeName = place.name.take(NAME_MAX))
            }
        }
    }

    fun onNameChange(text: String) {
        state.update { it.copy(placeName = text.take(NAME_MAX)) }
    }

    /** 카테고리를 바꾸면 고른 유형은 지움(다른 카테고리의 유형이 남지 않게). */
    fun onSelectCategory(code: String) {
        state.update { s ->
            if (s.selectedCategory == code) s else s.copy(selectedCategory = code, selectedType = null)
        }
    }

    fun onSelectType(code: String) {
        state.update { s -> s.copy(selectedType = if (s.selectedType == code) null else code) }
    }

    fun onSelectPolarity(code: String) {
        state.update { s -> s.copy(selectedPolarity = if (s.selectedPolarity == code) null else code) }
    }

    fun onToggleTag(tagNo: Long) {
        state.update { s ->
            s.copy(selectedTags = if (tagNo in s.selectedTags) s.selectedTags - tagNo else s.selectedTags + tagNo)
        }
    }

    fun onMemoChange(text: String) {
        state.update { it.copy(memo = text.take(MEMO_MAX)) }
    }

    fun onSave() {
        val s = state.value
        val draft = s.draft ?: return
        val trip = tripRepository.activeTrip.value ?: return
        if (!s.canSave) return
        state.update { it.copy(saving = true) }
        val input = PlaceInput(
            userPlaceName = s.placeName.trim(),
            placeTypeCode = s.selectedType,
            reviewPolarityCode = s.selectedPolarity,
            memo = s.memo.trim().ifEmpty { null },
            tagNos = s.tags.map { it.tagNo }.filter { it in s.selectedTags },
            naviPlace = s.selectedCandidate,
        )
        viewModelScope.launch {
            placeRepository.complete(trip.tripUuid, draft.tripPlaceNo, input).fold(
                onSuccess = { state.update { it.copy(saving = false, closed = true) } },
                onFailure = { state.update { it.copy(saving = false, message = UiMessage.SERVER_UNAVAILABLE) } },
            )
        }
    }

    /** 「나중에 입력」 → 확인 창. */
    fun onLater() {
        state.update { it.copy(showLeaveConfirm = true) }
    }

    /** 확인 창 「계속 입력」. */
    fun onContinueInput() {
        state.update { it.copy(showLeaveConfirm = false) }
    }

    /** 확인 창 「기록 남기기」 — 아무것도 저장하지 않고 임시 기록 그대로 닫음. */
    fun onKeepDraft() {
        state.update { it.copy(showLeaveConfirm = false, closed = true) }
    }

    /** 화면이 닫힌 뒤 — 다음에 열 때 지난 값이 남지 않게. */
    fun reset() {
        state.value = PlaceInputUiState()
    }

    fun onMessageShown() {
        state.update { it.copy(message = null) }
    }

    private companion object {
        const val REVIEW_POLARITY = "REVIEW_POLARITY"
        const val NAME_MAX = 100
        const val MEMO_MAX = 500
    }
}
