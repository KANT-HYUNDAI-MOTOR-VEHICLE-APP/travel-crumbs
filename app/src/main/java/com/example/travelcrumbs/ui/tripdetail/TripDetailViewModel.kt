package com.example.travelcrumbs.ui.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.components.roadmapPlaces
import com.example.travelcrumbs.ui.components.sketchPins
import com.example.travelcrumbs.ui.components.toSummary
import com.example.travelcrumbs.ui.tagsheet.PlaceTagSheetHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A-08 내 로드맵 상세(와이어프레임 v6 · #51 U6) — 제목 수정 · 간소화 경로 · 장소 순서 · 태그 · 메모 고침(A-03 시트 재사용) ·
 * 공개 토글 · 받은 별점 평균 · 참여 수 · 고치면 즉시 저장(공개 중이면 추천에 바로 반영 — 추천이 같은 저장소를 읽음).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    tagRepository: TagRepository,
    private val userRepository: CurrentUserRepository,
    ratingRepository: RatingRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    private val tagSheet = PlaceTagSheetHolder(
        scope = viewModelScope,
        trips = tripRepository,
        tags = tagRepository,
        savedStateHandle = savedStateHandle,
        timeSource = timeSource,
        onSaved = { message.value = it },
    )

    private val trip = savedStateHandle.getStateFlow<Long?>(KEY_TRIP, null)
        .flatMapLatest { no -> if (no == null) flowOf(null) else tripRepository.trip(no) }

    private val dialogs = combine(
        savedStateHandle.getStateFlow<String?>(KEY_RENAME, null),
        savedStateHandle.getStateFlow(KEY_CONSENT_ASK, false),
        message,
    ) { rename, ask, shown -> Triple(rename, ask, shown) }

    val uiState: StateFlow<TripDetailUiState> = combine(
        trip,
        tagRepository.tags,
        ratingRepository.records,
        dialogs,
        tagSheet.item,
    ) { record, tags, ratings, (rename, ask, shown), sheet ->
        val names = tags.associate { it.tagNo to it.tagNm }
        val mine = record?.let { ratings.ratingOf(AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + it.tripNo) }
        TripDetailUiState(
            summary = record?.toSummary(timeSource.zoneId()),
            durationMinutes = (record?.durationMillis ?: 0L) / AppConfig.ELAPSED_UNIT_MILLIS,
            places = record?.roadmapPlaces(names).orEmpty(),
            sketch = record?.sketchPins().orEmpty(),
            ratingAvg = mine?.ratingScore?.toDouble() ?: 0.0,
            ratingCnt = if (mine == null) 0 else 1,
            renameDraft = rename,
            consentAsk = ask,
            tagSheet = sheet,
            message = shown,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = TripDetailUiState(),
    )

    fun load(tripNo: Long) {
        if (savedStateHandle.get<Long>(KEY_TRIP) == tripNo) return
        savedStateHandle[KEY_TRIP] = tripNo
        savedStateHandle[KEY_RENAME] = null
        savedStateHandle[KEY_CONSENT_ASK] = false
    }

    fun onEditName() {
        savedStateHandle[KEY_RENAME] = uiState.value.summary?.tripNm.orEmpty()
    }

    fun onRenameChange(value: String) {
        savedStateHandle[KEY_RENAME] = value.take(AppConfig.TRIP_NAME_MAX_LENGTH)
    }

    fun onDismissRename() {
        savedStateHandle[KEY_RENAME] = null
    }

    fun onSaveRename() {
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        val draft = savedStateHandle.get<String>(KEY_RENAME)?.trim().orEmpty()
        if (draft.isEmpty()) return
        savedStateHandle[KEY_RENAME] = null
        viewModelScope.launch {
            tripRepository.rename(tripNo, draft)
            message.value = UiMessage.ROADMAP_SAVED
        }
    }

    /** 공개 토글 — 켤 때 동의가 없으면 동의 팝업. */
    fun onTogglePublic(public: Boolean) {
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        viewModelScope.launch {
            when {
                !public -> tripRepository.setVisibility(tripNo, Visibility.PRIVATE)
                userRepository.consents.first()[ConsentType.RECO_PUBLIC]?.agreed == true ->
                    tripRepository.setVisibility(tripNo, Visibility.ANONYMOUS_PUBLIC)
                else -> savedStateHandle[KEY_CONSENT_ASK] = true
            }
        }
    }

    fun onConsentAndPublish() {
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        savedStateHandle[KEY_CONSENT_ASK] = false
        viewModelScope.launch {
            userRepository.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
            tripRepository.setVisibility(tripNo, Visibility.ANONYMOUS_PUBLIC)
        }
    }

    fun onDismissConsent() {
        savedStateHandle[KEY_CONSENT_ASK] = false
    }

    fun onEditPlace(tripPlaceNo: Long) = tagSheet.open(tripPlaceNo)

    fun onSheetNameChange(value: String) = tagSheet.onNameChange(value)

    fun onSheetMemoChange(value: String) = tagSheet.onMemoChange(value)

    fun onSheetToggleTag(tagNo: Long) = tagSheet.toggleTag(tagNo)

    fun onSheetSkip() = tagSheet.skip()

    fun onSheetSave() = tagSheet.save()

    fun onMessageShown() {
        message.value = null
    }

    private companion object {
        const val KEY_TRIP = "trip_detail_trip_no"
        const val KEY_RENAME = "trip_detail_rename"
        const val KEY_CONSENT_ASK = "trip_detail_consent_ask"
    }
}
