package com.example.travelcrumbs.testing

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.travelcrumbs.domain.model.DemoFeatures
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.RequestRouteGuidanceUseCase
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.driving.DrivingViewModel
import com.example.travelcrumbs.ui.home.HomeViewModel
import com.example.travelcrumbs.ui.gleo.GleoCommandViewModel
import com.example.travelcrumbs.ui.recommendations.RecommendationsViewModel
import com.example.travelcrumbs.ui.settings.SettingsViewModel
import com.example.travelcrumbs.ui.tripstart.TripStartViewModel
import com.example.travelcrumbs.ui.shell.AppShellViewModel

/**
 * Compose UI 테스트용 조립 — Hilt 없이 fake 저장소로 화면의 ViewModel 을 만듦.
 * 화면의 `viewModel()` 은 [owner] 의 기본 팩토리로 이 ViewModel 들을 받음.
 */
class ShellTestGraph(
    initialDriving: DrivingState = DrivingState.PARKED,
    pinned: List<String> = emptyList(),
    places: List<com.example.travelcrumbs.domain.model.Place> = TEST_PLACES,
    /** debug 빌드 흉내(참) · release 흉내(거짓 · T110). */
    private val dummyRoute: Boolean = true,
    /** 진행 중 여행 하나로 시작(옛 시험의 「첫 실행」 자리) · 거짓이면 v6 첫 실행(여행 없음 · #51). */
    withTrip: Boolean = true,
    /** 시작 여행의 저장 번호(Room 판 trip_no 흉내) — 장소 기록 시험이 씀. */
    tripNo: Long? = null,
    /** 「이 장소 기록」을 실제 구현(TripPlaceRecorder · #51 U4)으로 — 거짓이면 늘 「준비 중」인 대역. */
    realRecordPlace: Boolean = false,
) {
    val time = FakeTimeSource(now = 1_000L)
    val driving = FakeDrivingSignalRepository(initialDriving)
    val gleo = FakeGleoActionRepository()
    val recordingLocal = if (withTrip) FakeRecordingLocalDataSource(com.example.travelcrumbs.domain.model.CurrentTrip.start(time.now).copy(tripNo = tripNo)) else FakeRecordingLocalDataSource()
    val recording = recordingRepositoryOf(time, recordingLocal)
    val inputs = unfinishedInputRepositoryOf(time)
    val savedState = SavedStateHandle()
    val navi = FakeNaviRepository()
    val catalog = FakePlaceCatalogRepository(places)
    val pinnedStore = FakePinnedPlacesStore(pinned, catalog)
    val pins = FakePinRepository()
    val drivingSavedState = SavedStateHandle()
    val guidance = FakeRouteGuidanceRequester()
    val ratingLocal = FakeRatingLocalDataSource()
    val ratings = ratingRepositoryOf(time, ratingLocal)
    val users = FakeCurrentUserRepository(time = time)
    val trips = FakeTripRepository(time = time)
    val tags = FakeTagRepository()
    val guide = guideTrackerOf(navi, pins)

    val recordPlace: com.example.travelcrumbs.domain.RecordPlaceUseCase by lazy {
        if (realRecordPlace) {
            com.example.travelcrumbs.data.trip.TripPlaceRecorder(recording, trips, users, navi, driving, inputs)
        } else {
            FakeRecordPlaceUseCase()
        }
    }

    val owner: ViewModelStoreOwner = Owner()

    /** 시험이 화면 없이 ViewModel 을 바로 만들 때. */
    fun <T : ViewModel> viewModel(modelClass: Class<T>): T = modelClass.cast(create(modelClass))!!

    private inner class Owner : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = ViewModelStore()
        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                modelClass.cast(this@ShellTestGraph.create(modelClass))!!
        }
    }

    private fun create(modelClass: Class<*>): ViewModel = when (modelClass) {
        AppShellViewModel::class.java -> AppShellViewModel(driving, inputs, savedState, gleo)
        GleoCommandViewModel::class.java -> GleoCommandViewModel(gleo)
        DrivingViewModel::class.java -> DrivingViewModel(
            recordingRepository = recording,
            recordPlace = recordPlace,
            startVoiceInput = FakeStartVoiceInputUseCase(),
            drivingSignalRepository = driving,
            savedStateHandle = drivingSavedState,
            ratingRepository = ratings,
            naviRepository = navi,
            gleoActionRepository = gleo,
        )
        HomeViewModel::class.java -> HomeViewModel(
            recordingRepository = recording,
            recordPlace = recordPlace,
            drivingSignalRepository = driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = navi,
            placeCatalogRepository = catalog,
            pinnedPlacesRepository = pinnedStore.repository,
            guideRequests = guide,
            placeStatsRepository = FakePlaceStatsRepository(),
            demoFeatures = DemoFeatures(dummyRoute = dummyRoute),
            ratingRepository = ratings,
            recommendationRepository = SeedRecommendationRepository(),
            timeSource = time,
            savedStateHandle = SavedStateHandle(),
            userRepository = users,
            tripRepository = trips,
            tagRepository = tags,
            unfinishedInputRepository = inputs,
        )
        TripStartViewModel::class.java -> TripStartViewModel(recording, users, catalog, guide, navi, time, SavedStateHandle())
        com.example.travelcrumbs.ui.roadmap.RoadmapReviewViewModel::class.java ->
            com.example.travelcrumbs.ui.roadmap.RoadmapReviewViewModel(trips, tags, users, time, SavedStateHandle(), catalog)
        com.example.travelcrumbs.ui.trips.TripsViewModel::class.java ->
            com.example.travelcrumbs.ui.trips.TripsViewModel(trips, users, ratings, time, SavedStateHandle())
        com.example.travelcrumbs.ui.tripdetail.TripDetailViewModel::class.java ->
            com.example.travelcrumbs.ui.tripdetail.TripDetailViewModel(trips, tags, users, ratings, time, SavedStateHandle())
        SettingsViewModel::class.java -> SettingsViewModel(users, trips, time, SavedStateHandle())
        RecommendationsViewModel::class.java -> RecommendationsViewModel(
            repository = SeedRecommendationRepository(),
            requestRouteGuidance = RequestRouteGuidanceUseCase(guidance),
            savedStateHandle = SavedStateHandle(),
            ratingRepository = ratings,
            timeSource = time,
            offlineMap = FakeOfflineMapRepository(),
        )
        else -> error("테스트 그래프에 없는 ViewModel: ${modelClass.simpleName}")
    }
}
