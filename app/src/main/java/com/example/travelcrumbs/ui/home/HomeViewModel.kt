package com.example.travelcrumbs.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.place.PinnedPlacesRepository
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.place.PlaceStatsRepository
import com.example.travelcrumbs.data.rating.RatingRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuideRequestState
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.domain.model.DraftPlaceInput
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.ui.rating.RatingPopupHolder
import com.example.travelcrumbs.ui.components.PlaceTagSheetHolder
import com.example.travelcrumbs.ui.components.UiMessage
import com.example.travelcrumbs.ui.components.toSummary
import com.example.travelcrumbs.domain.model.TripStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 기록 칸 정차 화면(스펙 app-shell-v5 2-2 · D34 · 파일 자리는 N28 · navigation 2.1 · 2.2) — 여행 상태는 주행 화면과
 * 같은 출처(RecordingRepository · #6 D1).
 * 「여기로 안내」 · 「고정」 · 「고정 해제」 · 실패 폴백(「다시 시도」 · 「더미 경로」 · 닫기)은 정차일 때만
 * (navigation 3.1 · U35 · U18 · U51 ~ U56) — 결과는 상태 칸으로.
 * 별점 자동 팝업(#41 B-06): 추천 안내 뒤 진행한 여행을 「여행 종료」로 끝냈으면 그 로드맵을 한 번(규칙 [RecommendationRules.pendingRatingPrompt]).
 * 이 화면은 정차일 때만 그려지므로(앱 틀) 주행 · 모름엔 팝업이 없음.
 *
 * 길안내 요청 · 대기 · TIMEOUT 대조 · 늦은 시작 판정 · 핀 남기기는 화면과 무관한 입구([GuideRequestTracker] · 앱 수명)가 함 —
 * 이 ViewModel 은 입구 상태를 화면 상태로 바꾸고, 정차일 때만 입구를 부름.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordPlace: RecordPlaceUseCase,
    drivingSignalRepository: DrivingSignalRepository,
    gleoActionRepository: GleoActionRepository,
    private val naviRepository: NaviRepository,
    private val placeCatalogRepository: PlaceCatalogRepository,
    private val pinnedPlacesRepository: PinnedPlacesRepository,
    private val placeStatsRepository: PlaceStatsRepository,
    private val guideRequests: GuideRequestTracker,
    private val demoFeatures: DemoFeatures,
    private val ratingRepository: RatingRepository,
    recommendationRepository: RecommendationRepository,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
    userRepository: CurrentUserRepository,
    private val tripRepository: TripRepository,
    private val tagRepository: TagRepository,
    private val unfinishedInputRepository: UnfinishedInputRepository,
) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)
    private val naviState = MutableStateFlow<SdkStatus<NaviRouteState>>(SdkStatus.Idle)
    private val placeStats = MutableStateFlow<SdkStatus<List<PlaceStatItem>>>(SdkStatus.Idle)

    /**
     * 더미 경로 패널(E13)을 연 실패의 시도 번호 — 패널은 이 번호와 입구의 지금 실패가 같을 때만 보임(파생).
     * 새 요청 · 늦은 시작 판정 · 다른 장소의 실패면 입구 상태가 바뀌어 저절로 사라짐 — 쓰기 순서와 무관(R4-1).
     * 화면 상태뿐 · 기기 저장 · SavedStateHandle 복원 대상 아님(navigation 3.1 · T108).
     */
    private val dummyRouteAttempt = MutableStateFlow<Long?>(null)

    /**
     * 길안내 연결 화면(2.3)을 연 때의 입구 변경 번호 — 화면은 입구 상태의 [GuideRequestState.seq] 가 이 번호 이상일 때만 보임(파생).
     * 「여기로 안내」가 입구를 부르기 **전에** 다음 번호로 써 둠 → 입구 상태가 바뀌기 전엔 화면이 닫힌 채, 바뀐 뒤엔 열림 —
     * 「요청 중인데 화면 안 열림」 같은 사이값이 생기지 않음. 화면 상태뿐 · 복원 대상 아님(T129).
     */
    private val guideScreenFrom = MutableStateFlow<Long?>(null)

    /** 화면이 고른 두 보기의 열림 번호 — combine 인자 수를 줄이려고 하나로 묶음. */
    private val openMarks = combine(guideScreenFrom, dummyRouteAttempt) { from, dummy ->
        OpenMarks(guideScreenFrom = from, dummyRouteAttempt = dummy)
    }

    /**
     * [guideScreenFrom] = 길안내 연결 화면을 연 입구 변경 번호(null = 닫힘) ·
     * [dummyRouteAttempt] = 더미 경로를 켠 실패의 시도 번호(null = 꺼짐).
     */
    private data class OpenMarks(val guideScreenFrom: Long?, val dummyRouteAttempt: Long?)

    /** 후보 — 읽기 전 null. 조작 메서드가 바로 찾을 수 있게 값으로 들고 있음. */
    private val candidates = MutableStateFlow<List<Place>?>(null)

    /** 별점 자동 팝업(#41) — 고르는 중 값 · 저장은 도우미가 · 결과 한 줄은 이 화면의 [message]. */
    private val rating = RatingPopupHolder(
        scope = viewModelScope,
        repository = ratingRepository,
        savedStateHandle = savedStateHandle,
        timeSource = timeSource,
        autoPrompt = true,
        loadTags = recommendationRepository::tags,
        onSaved = { message.value = UiMessage.RATING_SAVED },
    )

    private val sdkCheck = combine(
        drivingSignalRepository.drivingState,
        naviRepository.ready,
        naviState,
        gleoActionRepository.openedByGleoAction,
    ) { driving, naviReady, navi, openedByGleo ->
        HomeUiState(
            drivingState = driving,
            naviReady = naviReady,
            navi = navi,
            openedByGleoAction = openedByGleo,
        )
    }

    private val places = combine(
        candidates,
        pinnedPlacesRepository.pinnedIds.map<List<String>, List<String>?> { it }.onStart { emit(null) },
        guideRequests.state,
        placeStats,
        openMarks,
    ) { all, pinnedIds, request, stats, marks ->
        PlacesPart.of(all, pinnedIds, request, stats, marks.guideScreenFrom, marks.dummyRouteAttempt)
    }

    /** v6 첫 화면(C-02 · #51 U2) — 닉네임 · 최근 끝난 여행 1개 · 여행 종료 확인 팝업. */
    private val v6Part = combine(
        userRepository.user,
        tripRepository.trips,
        savedStateHandle.getStateFlow(KEY_END_CONFIRM, false),
        savedStateHandle.getStateFlow(KEY_TRIP_START, false),
        tripRepository.pendingReviewTripNo,
    ) { user, trips, endConfirm, tripStart, reviewTripNo ->
        V6Part(
            tripStartOpen = tripStart,
            reviewTripNo = reviewTripNo,
            nicknameNm = user.nicknameNm,
            recentTrip = trips.firstOrNull { it.status == TripStatus.COMPLETED }?.toSummary(timeSource.zoneId()),
            endConfirmVisible = endConfirm,
        )
    }

    private data class V6Part(
        val tripStartOpen: Boolean,
        val reviewTripNo: Long?,
        val nicknameNm: String,
        val recentTrip: com.example.travelcrumbs.ui.components.TripSummaryItem?,
        val endConfirmVisible: Boolean,
    )

    /** A-03 장소 기록 · 태그 시트(#51 U4) — 정차 중 「이 장소 기록」 · 「이어서 쓰기」 띠로 엶. */
    private val tagSheet = PlaceTagSheetHolder(
        scope = viewModelScope,
        trips = tripRepository,
        tags = tagRepository,
        savedStateHandle = savedStateHandle,
        timeSource = timeSource,
        onSaved = { message.value = it },
    )

    private val extras = combine(rating.item, v6Part, tagSheet.item) { prompt, part, sheet -> Triple(prompt, part, sheet) }

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.progress,
        message,
        sdkCheck,
        places,
        extras,
    ) { progress, shown, sdk, part, (prompt, v6, sheet) ->
        val parked = sdk.drivingState == DrivingState.PARKED
        sdk.copy(
            hasTrip = progress.hasTrip,
            tripName = progress.tripName,
            nicknameNm = v6.nicknameNm,
            recentTrip = v6.recentTrip,
            endConfirmVisible = v6.endConfirmVisible && progress.status.inProgress,
            tripStartOpen = v6.tripStartOpen && !progress.status.inProgress,
            tagSheet = sheet.takeIf { parked },
            // A-05 검토는 별점 팝업(#41)이 닫힌 뒤(지시서 U5 순서)
            reviewTripNo = v6.reviewTripNo.takeIf { prompt == null },
            tripStatus = progress.status,
            elapsedMinutes = progress.elapsedMinutes,
            placeCount = progress.placeCount,
            message = shown,
            pinnedPlaces = part.pinnedPlaces,
            candidates = part.candidates,
            placesVisible = parked,
            canPinMore = part.canPinMore,
            routeRequest = part.routeRequest,
            placeStats = part.placeStats,
            // 주행 · 모름이면 전용 화면 · 더미 경로는 없음(U54 넓힘 · 닫힘은 아래 init 구독이 기록)
            guideScreen = part.guideScreen.takeIf { parked },
            dummyRoute = part.dummyRoute.takeIf { parked },
            dummyRouteAvailable = demoFeatures.dummyRoute,
            ratingPrompt = prompt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.UI_STATE_STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch { candidates.value = placeCatalogRepository.candidates() }
        // Gleo 「여행 끝낼게」(#69 U2) — 저장소는 바꾸지 않고 기존 확인 팝업만 엶 · 명령마다 한 번.
        viewModelScope.launch {
            gleoActionRepository.commandState.collect { command ->
                if (command.screen == GleoScreenRequest.ConfirmEndTrip && savedStateHandle.get<Long>(KEY_GLEO_END) != command.noticeId) {
                    savedStateHandle[KEY_GLEO_END] = command.noticeId
                    savedStateHandle[KEY_END_CONFIRM] = true
                }
            }
        }

        // 「이어서 쓰기」 띠(주행 중 기록한 임시저장 장소 · #51 U4) — 그 장소로 A-03 시트를 엶
        viewModelScope.launch {
            unfinishedInputRepository.pendingRestore.collect { restore ->
                if (restore?.destinationId != DraftPlaceInput.DESTINATION_ID) return@collect
                restore.fields[DraftPlaceInput.FIELD_TRIP_PLACE_NO]?.toLongOrNull()?.let(tagSheet::open)
                unfinishedInputRepository.consumeRestore(DraftPlaceInput.DESTINATION_ID)
            }
        }
        // 주행 · 모름이 되면 길안내 연결 화면 · 더미 경로를 닫음 — 정차로 돌아와도 홈 목록(U54 넓힘 · T124 · T125).
        // 주행 중엔 홈 화면이 그려지지 않아 uiState 구독이 멈추므로 따로 받음(주행 판정 출처는 하나 · 공유 흐름).
        viewModelScope.launch {
            drivingSignalRepository.drivingState.collect { driving ->
                if (driving != DrivingState.PARKED) closeGuideScreen()
            }
        }
    }

    /** 별점 팝업 — 별 고르기. */
    fun selectRatingScore(score: Int) = rating.selectScore(uiState.value.ratingPrompt, score)

    /** 별점 팝업 — 좋았던 점 칩 켜고 끄기. */
    fun toggleRatingTag(tagNo: Long) = rating.toggleTag(uiState.value.ratingPrompt, tagNo)

    /** 별점 팝업 「나중에」 · 바깥 · 뒤로 — 그 로드맵은 다시 자동으로 안 뜸(B-04 「별점 주기」로는 줄 수 있음). */
    fun dismissRatingPrompt() = rating.dismiss(uiState.value.ratingPrompt)

    /** 별점 팝업 「별점 주기」 — 저장되면 팝업이 사라지고 한 줄 안내. */
    fun submitRating() = rating.submit(uiState.value.ratingPrompt)

    /** 정차 「이 장소 기록」 — 장소 저장(위치 · 임시저장) 뒤 A-03 시트를 그 장소로 엶(#51 U4). */
    fun onRecordPlace() {
        viewModelScope.launch {
            when (recordPlace()) {
                ActionResult.DONE -> latestPlaceNo()?.let(tagSheet::open)
                ActionResult.NOT_READY, ActionResult.FAILED, ActionResult.UNCONFIRMED -> message.value = UiMessage.NOT_READY
            }
        }
    }

    private suspend fun latestPlaceNo(): Long? {
        val tripNo = recordingRepository.progress.first().tripNo ?: return null
        return tripRepository.trip(tripNo).first()?.places?.maxByOrNull { it.placeOrd }?.tripPlaceNo
    }

    fun onTagNameChange(value: String) = tagSheet.onNameChange(value)

    fun onTagMemoChange(value: String) = tagSheet.onMemoChange(value)

    fun onToggleSheetTag(tagNo: Long) = tagSheet.toggleTag(tagNo)

    fun onSkipTagSheet() = tagSheet.skip()

    fun onSaveTagSheet() = tagSheet.save()

    /** C-02 「여행 시작」 — A-01 여행 시작 화면을 기록 칸 안에 엶(#51 U3). */
    fun onStartTrip() {
        savedStateHandle[KEY_TRIP_START] = true
    }

    /** A-01 시작 끝 — 기록 화면으로 · 내비를 못 열었으면 그 한 줄. */
    fun onTripStartDone(notice: UiMessage?) {
        savedStateHandle[KEY_TRIP_START] = false
        message.value = notice
    }

    /** A-01 시스템 뒤로 — 시작하지 않고 닫음. */
    fun onTripStartCancel() {
        savedStateHandle[KEY_TRIP_START] = false
    }

    /** 정차 「여행 종료」(C-02 진행 중 · #51 U2) — A-04 확인 팝업만 엶. */
    fun onEndTrip() {
        savedStateHandle[KEY_END_CONFIRM] = true
    }

    /** A-04 「계속 기록」 · 바깥 · 뒤로 — 팝업만 닫음. */
    fun onContinueRecording() {
        savedStateHandle[KEY_END_CONFIRM] = false
    }

    /** A-04 「종료」 — COMPLETED · 안내받은 추천 로드맵이 있으면 별점 자동 팝업 대상(#41) · 이어서 A-05 검토(#51 U5). */
    fun onConfirmEndTrip() {
        savedStateHandle[KEY_END_CONFIRM] = false
        viewModelScope.launch {
            recordingRepository.endTrip()?.let { endedAt -> ratingRepository.markGuidedTripsEnded(endedAt) }
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    /**
     * 「여기로 안내」(navigation 3.2 「길안내 요청」) — 요청을 시작하면(또는 그 순간 미연결로 바로 실패하면) 길안내 연결 화면(2.3)이 열림(U68).
     * 주행 · 모름 · 요청 중이면 무시(T5 · 화면도 그대로).
     */
    fun onGuide(placeId: String) {
        if (!uiState.value.placesVisible) return
        val place = candidates.value?.firstOrNull { it.id == placeId } ?: return
        val current = guideRequests.state.value
        if (current is GuideRequestState.Requesting) return
        // 입구가 상태를 바꾸기 전에 「다음 변경부터 화면에 보임」을 써 둠 — 요청 중 · 즉시 실패 어느 쪽이든 그 상태와 함께 열림
        dummyRouteAttempt.value = null
        guideScreenFrom.value = current.seq + 1
        guideRequests.guide(place)
    }

    /** 「길안내 화면 닫기」(2.3 G6 · 시스템 뒤로) — 요청은 취소하지 않음 · 결과는 홈 E3 에(T122). 주행 · 모름이면 무시(T132). */
    fun onCloseGuideScreen() {
        if (!uiState.value.placesVisible) return
        closeGuideScreen()
    }

    private fun closeGuideScreen() {
        guideScreenFrom.value = null
        dummyRouteAttempt.value = null
    }

    /** 화면이 다시 앞으로 옴(시작 · 재개 · R1) — 바로 앞 요청이 아직 시작 알림을 못 받았으면 입구가 내비 상태로 대조. */
    fun onScreenResumed() {
        guideRequests.recheck()
    }

    /**
     * 「다시 시도」(navigation 3.2 「실패 폴백」 · E11) — 실패한 그 장소로 「여기로 안내」와 같은 길.
     * 그 장소를 고정 해제했어도 요청(T112) · 정차 · 실패가 아니면 무시(T104 · T105).
     */
    fun onRetry() {
        if (!uiState.value.placesVisible) return
        guideRequests.retry()
    }

    /**
     * 「더미 경로」(E12 · E13) — 실패한 장소의 이름 · 주소만 패널에 · 내비 요청 · 핀 · 서버 · 기기 저장 · 로그 없음(U56 · T101).
     * debug 빌드 · 정차 · 길안내 연결 화면 열림 · 실패일 때만(켜져 있으면 같은 번호를 다시 씀 — 그대로).
     */
    fun onShowDummyRoute() {
        if (!demoFeatures.dummyRoute || !uiState.value.placesVisible || uiState.value.guideScreen == null) return
        val failed = guideRequests.state.value as? GuideRequestState.Failed ?: return
        dummyRouteAttempt.value = failed.attempt
    }

    /** 「고정」 — 정차일 때만 · 최대면 저장소가 그대로 둠(T36). */
    fun onPin(placeId: String) {
        if (!uiState.value.placesVisible) return
        viewModelScope.launch { pinnedPlacesRepository.pin(placeId) }
    }

    /** 「고정 해제」 — 정차일 때만 · 핀(여행 기록)에는 영향 없음. */
    fun onUnpin(placeId: String) {
        if (!uiState.value.placesVisible) return
        viewModelScope.launch { pinnedPlacesRepository.unpin(placeId) }
    }

    /** debug SDK 확인 카드가 보일 때만 부름(release 엔 카드가 없음 · D9). */
    fun refreshNavi() {
        if (naviState.value is SdkStatus.Loading) return
        naviState.value = SdkStatus.Loading
        viewModelScope.launch {
            naviState.value = runCatching {
                withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.routeState() }
            }.fold(
                onSuccess = { SdkStatus.Ready(it) },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    /** debug 카드의 「안내 건수」 한 줄(navigation 4.6 · U41) — 카드가 보일 때만 부름. */
    fun refreshPlaceStats() {
        if (placeStats.value is SdkStatus.Loading) return
        placeStats.value = SdkStatus.Loading
        viewModelScope.launch {
            val names = (candidates.value ?: placeCatalogRepository.candidates()).associate { it.id to it.name }
            placeStats.value = placeStatsRepository.topStats(AppConfig.PLACE_STATS_CARD_LIMIT).fold(
                onSuccess = { stats ->
                    SdkStatus.Ready(stats.map { PlaceStatItem(names[it.placeId] ?: it.placeId, it.startCount) })
                },
                onFailure = { SdkStatus.Failed(it.toCode()) },
            )
        }
    }

    private companion object {
        const val KEY_END_CONFIRM = "home_end_confirm_visible"
        const val KEY_GLEO_END = "home_gleo_end_confirm_notice"
        const val KEY_TRIP_START = "home_trip_start_open"
    }

    private fun Throwable.toCode(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

    /** 「가 볼 장소」 · 「장소 고르기」 부분 — 후보에 없는 고정 id 는 보이지 않고 세지 않음(navigation 2.1). */
    private data class PlacesPart(
        val pinnedPlaces: List<PlaceItem>?,
        val candidates: List<CandidateItem>,
        val canPinMore: Boolean,
        val routeRequest: RouteRequestState,
        val placeStats: SdkStatus<List<PlaceStatItem>>,
        val guideScreen: PlaceItem?,
        val dummyRoute: PlaceItem?,
    ) {
        companion object {
            fun of(
                all: List<Place>?,
                pinnedIds: List<String>?,
                request: GuideRequestState,
                stats: SdkStatus<List<PlaceStatItem>>,
                screenFrom: Long?,
                dummyAttempt: Long?,
            ): PlacesPart {
                val screen = guideScreenOf(request, screenFrom)
                val byId = all.orEmpty().associateBy { it.id }
                val pinned = if (all == null || pinnedIds == null) {
                    null
                } else {
                    pinnedIds.mapNotNull { byId[it] }.map { PlaceItem(it.id, it.name, it.address) }
                }
                val pinnedSet = pinned.orEmpty().map { it.id }.toSet()
                return PlacesPart(
                    pinnedPlaces = pinned,
                    candidates = all.orEmpty().map { CandidateItem(it.id, it.name, it.address, it.id in pinnedSet) },
                    canPinMore = pinnedSet.size < AppConfig.PINNED_PLACES_MAX,
                    routeRequest = request.toUi(),
                    placeStats = stats,
                    guideScreen = screen,
                    dummyRoute = if (screen == null) null else dummyRouteOf(request, dummyAttempt),
                )
            }
        }
    }
}

/** 입구 상태 → 화면 상태(navigation 3.1) — 시작됨은 요청할 때의 장소 이름 · 실패는 이유 · 장소 id. */
internal fun GuideRequestState.toUi(): RouteRequestState = when (this) {
    GuideRequestState.Idle -> RouteRequestState.Idle
    is GuideRequestState.Requesting -> RouteRequestState.Requesting(place.id)
    is GuideRequestState.Started -> RouteRequestState.Started(place.name)
    is GuideRequestState.Failed -> RouteRequestState.Failed(reason, place.id)
}

/**
 * 길안내 연결 화면(2.3)에 그릴 장소 — 화면을 연 뒤의 입구 상태([GuideRequestState.seq] ≥ [screenFrom])일 때만 · 그 상태의 장소.
 * 입구 상태 하나에서 나오므로 화면의 장소와 요청 상태가 어긋날 수 없음.
 */
internal fun guideScreenOf(request: GuideRequestState, screenFrom: Long?): PlaceItem? {
    if (screenFrom == null || request.seq < screenFrom) return null
    val place = when (request) {
        GuideRequestState.Idle -> return null
        is GuideRequestState.Requesting -> request.place
        is GuideRequestState.Started -> request.place
        is GuideRequestState.Failed -> request.place
    }
    return PlaceItem(place.id, place.name, place.address)
}

/**
 * 더미 경로(2.3 G1-D · 옛 E13)에 그릴 장소 — 입구가 지금 실패이고 그 시도 번호가 패널을 연 번호와 같을 때만(R4-1 · 파생).
 * 요청 중 · 시작됨이면 [dummyAttempt] 가 무엇이든 없음 → 「요청 중 + 패널 열림」은 만들 수 없음.
 */
internal fun dummyRouteOf(request: GuideRequestState, dummyAttempt: Long?): PlaceItem? {
    val failed = request as? GuideRequestState.Failed ?: return null
    if (dummyAttempt == null || failed.attempt != dummyAttempt) return null
    return PlaceItem(failed.place.id, failed.place.name, failed.place.address)
}
