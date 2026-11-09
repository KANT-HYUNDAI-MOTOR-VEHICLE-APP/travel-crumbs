package com.example.travelcrumbs.ui.roadmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RoadmapRules
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.ui.components.roadmapPlaces
import com.example.travelcrumbs.ui.components.sketchPins
import com.example.travelcrumbs.ui.tagsheet.PlaceTagSheetHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A-05 로드맵 검토 · A-06 저장 · 공개(와이어프레임 v6 · #51 U5) — 여행이 끝난 뒤 정차한 기록 칸에서(별점 팝업이 있으면 그 뒤).
 * A-05: 간소화 경로 · 장소 순서 · 장소별 태그 · 미완성 표시 · 「태그 달기」(A-03 시트) · 「로드맵 저장」 → A-06.
 * A-06: 비공개 / 공개(닉네임 표시) · 처음 공개면 추천 공개 동의(RECO_PUBLIC) · 미리보기 · 「저장」 → 검토 끝 · 내 여행 보관함.
 * 공개 = 표준 결정값 ANONYMOUS_PUBLIC 으로 저장 · 화면 · 추천은 닉네임 표시(v6 · 「닉네임 공개」 코드는 남은 결정).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoadmapReviewViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    tagRepository: TagRepository,
    private val userRepository: CurrentUserRepository,
    timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
    private val placeCatalog: PlaceCatalogRepository,
) : ViewModel() {

    private val done = MutableStateFlow(false)

    /** 목적지 이름 → 주소(앱 장소 목록) — 장소 주소로 지역을 모를 때 쓰는 대체(#51 U7). */
    private val destAddrs = MutableStateFlow<Map<String, String>>(emptyMap())

    private val tagSheet = PlaceTagSheetHolder(
        scope = viewModelScope,
        trips = tripRepository,
        tags = tagRepository,
        savedStateHandle = savedStateHandle,
        timeSource = timeSource,
        onSaved = {},
    )

    private val trip = savedStateHandle.getStateFlow<Long?>(KEY_TRIP, null)
        .flatMapLatest { no -> if (no == null) flowOf(null) else tripRepository.trip(no) }

    private val choices = combine(
        savedStateHandle.getStateFlow(KEY_STEP, RoadmapStep.REVIEW.name),
        savedStateHandle.getStateFlow(KEY_PUBLIC, false),
        done,
    ) { step, public, finished -> Triple(RoadmapStep.valueOf(step), public, finished) }

    private val people = combine(userRepository.user, userRepository.consents, destAddrs) { user, consents, addrs ->
        Triple(user.nicknameNm, consents[ConsentType.RECO_PUBLIC]?.agreed != true, addrs)
    }

    init {
        viewModelScope.launch { destAddrs.value = placeCatalog.candidates().associate { it.name to it.address } }
    }

    val uiState: StateFlow<RoadmapReviewUiState> = combine(
        trip,
        tagRepository.tags,
        choices,
        people,
        tagSheet.item,
    ) { record, tags, (step, public, finished), (nickname, consentNeeded, addrs), sheet ->
        val names = tags.associate { it.tagNo to it.tagNm }
        val region = record?.let { RoadmapRules.regionOfTrip(it.places, addrs[it.destPlaceNm]) }
        RoadmapReviewUiState(
            step = step,
            tripNo = record?.tripNo,
            tripNm = record?.tripNm,
            durationMinutes = (record?.durationMillis ?: 0L) / AppConfig.ELAPSED_UNIT_MILLIS,
            places = record?.roadmapPlaces(names).orEmpty(),
            sketch = record?.sketchPins().orEmpty(),
            publicChosen = public,
            recoConsentNeeded = consentNeeded,
            nicknameNm = nickname,
            regionSidoNm = region?.sidoNm,
            regionSigunguNm = region?.sigunguNm,
            representativeTagNm = record?.let { RoadmapRules.representativeTagNo(it.places) }?.let { names[it] },
            tagSheet = sheet,
            done = finished,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = RoadmapReviewUiState(),
    )

    /** 검토할 여행 — 다른 여행이면 A-05 부터. */
    fun load(tripNo: Long) {
        if (savedStateHandle.get<Long>(KEY_TRIP) == tripNo) return
        savedStateHandle[KEY_TRIP] = tripNo
        savedStateHandle[KEY_STEP] = RoadmapStep.REVIEW.name
        savedStateHandle[KEY_PUBLIC] = false
        done.value = false
    }

    /** 「태그 달기」 · 장소 카드 — A-03 시트를 그 장소로. */
    fun onEditPlace(tripPlaceNo: Long) = tagSheet.open(tripPlaceNo)

    fun onSheetNameChange(value: String) = tagSheet.onNameChange(value)

    fun onSheetMemoChange(value: String) = tagSheet.onMemoChange(value)

    fun onSheetToggleTag(tagNo: Long) = tagSheet.toggleTag(tagNo)

    fun onSheetSkip() = tagSheet.skip()

    fun onSheetSave() = tagSheet.save()

    /** A-05 「로드맵 저장」 → A-06. */
    fun onSaveRoadmap() {
        savedStateHandle[KEY_STEP] = RoadmapStep.PUBLISH.name
    }

    /** A-06 비공개 · 공개 고르기. */
    fun onChoosePublic(public: Boolean) {
        savedStateHandle[KEY_PUBLIC] = public
    }

    /** A-06 「저장」(「동의하고 저장」) — 공개면 처음 한 번 동의 행 · 공개 범위 · 검토 끝. */
    fun onPublish() {
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        val public = savedStateHandle.get<Boolean>(KEY_PUBLIC) == true
        val consentNeeded = uiState.value.recoConsentNeeded
        viewModelScope.launch {
            if (public && consentNeeded) userRepository.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
            tripRepository.setVisibility(tripNo, if (public) Visibility.ANONYMOUS_PUBLIC else Visibility.PRIVATE)
            // 검토 끝 표시는 화면이 「끝」을 받은 뒤([onDoneHandled]) — 먼저 하면 기록 칸이 이 화면을 먼저 치워 끝을 못 받음
            done.value = true
        }
    }

    /** 시스템 뒤로 — A-06 이면 A-05 · A-05 면 「나중에」(검토 끝 · 비공개로 보관함에 남음). */
    fun onBack() {
        if (savedStateHandle.get<String>(KEY_STEP) == RoadmapStep.PUBLISH.name) {
            savedStateHandle[KEY_STEP] = RoadmapStep.REVIEW.name
            return
        }
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        viewModelScope.launch { tripRepository.markReviewed(tripNo) }
    }

    /** 화면이 저장 끝을 받음 — 이제 검토 끝(기록 칸은 첫 화면으로). */
    fun onDoneHandled() {
        done.value = false
        val tripNo = savedStateHandle.get<Long>(KEY_TRIP) ?: return
        viewModelScope.launch { tripRepository.markReviewed(tripNo) }
    }

    private companion object {
        const val KEY_TRIP = "roadmap_trip_no"
        const val KEY_STEP = "roadmap_step"
        const val KEY_PUBLIC = "roadmap_public"
    }
}
