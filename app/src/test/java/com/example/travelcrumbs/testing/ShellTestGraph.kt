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
import com.example.travelcrumbs.ui.recommendations.RecommendationsViewModel
import com.example.travelcrumbs.ui.settings.SettingsViewModel
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
) {
    val time = FakeTimeSource(now = 1_000L)
    val driving = FakeDrivingSignalRepository(initialDriving)
    val recordingLocal = if (withTrip) FakeRecordingLocalDataSource(com.example.travelcrumbs.domain.model.CurrentTrip.start(time.now)) else FakeRecordingLocalDataSource()
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

    val owner: ViewModelStoreOwner = Owner()

    private inner class Owner : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = ViewModelStore()
        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                modelClass.cast(this@ShellTestGraph.create(modelClass))!!
        }
    }

    private fun create(modelClass: Class<*>): ViewModel = when (modelClass) {
        AppShellViewModel::class.java -> AppShellViewModel(driving, inputs, savedState)
        DrivingViewModel::class.java -> DrivingViewModel(
            recordingRepository = recording,
            recordPlace = FakeRecordPlaceUseCase(),
            startVoiceInput = FakeStartVoiceInputUseCase(),
            drivingSignalRepository = driving,
            savedStateHandle = drivingSavedState,
            ratingRepository = ratings,
            naviRepository = navi,
        )
        HomeViewModel::class.java -> HomeViewModel(
            recordingRepository = recording,
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = navi,
            placeCatalogRepository = catalog,
            pinnedPlacesRepository = pinnedStore.repository,
            guideRequests = guideTrackerOf(navi, pins),
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
        SettingsViewModel::class.java -> SettingsViewModel(users, trips, time, SavedStateHandle())
        RecommendationsViewModel::class.java -> RecommendationsViewModel(
            repository = SeedRecommendationRepository(),
            requestRouteGuidance = RequestRouteGuidanceUseCase(guidance),
            savedStateHandle = SavedStateHandle(),
            ratingRepository = ratings,
            timeSource = time,
        )
        else -> error("테스트 그래프에 없는 ViewModel: ${modelClass.simpleName}")
    }
}
