package com.example.travelcrumbs.testing

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.ui.driving.DrivingViewModel
import com.example.travelcrumbs.ui.home.HomeViewModel
import com.example.travelcrumbs.ui.gleo.GleoCommandViewModel
import com.example.travelcrumbs.ui.placeinput.PlaceInputViewModel
import com.example.travelcrumbs.ui.shell.AppShellViewModel
import com.example.travelcrumbs.ui.tripdetail.TripDetailViewModel
import com.example.travelcrumbs.ui.trips.TripsViewModel
import com.example.travelcrumbs.ui.tripsetup.TripSetupViewModel

/**
 * Compose UI 테스트용 조립 — Hilt 없이 fake 저장소로 화면의 ViewModel 을 만듦.
 * 화면의 `viewModel()` 은 [owner] 의 기본 팩토리로 이 ViewModel 들을 받음.
 */
class ShellTestGraph(initialDriving: DrivingState = DrivingState.PARKED) {
    val time = FakeTimeSource(now = 1_000L)
    val driving = FakeDrivingSignalRepository(initialDriving)
    val recording = recordingRepositoryOf(time)
    val drafts = draftRepositoryOf(time)
    val savedState = SavedStateHandle()
    val trips = FakeTripRepository()
    val places = FakePlaceRepository()

    val owner: ViewModelStoreOwner = Owner()

    private inner class Owner : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = ViewModelStore()
        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                modelClass.cast(this@ShellTestGraph.create(modelClass))!!
        }
    }

    private fun create(modelClass: Class<*>): ViewModel = when (modelClass) {
        AppShellViewModel::class.java -> AppShellViewModel(driving, drafts, savedState)
        GleoCommandViewModel::class.java -> GleoCommandViewModel(FakeGleoActionRepository())
        DrivingViewModel::class.java -> DrivingViewModel(
            recording,
            FakeRecordPlaceUseCase(),
            FakeStartVoiceInputUseCase(),
        )
        HomeViewModel::class.java -> HomeViewModel(
            recordingRepository = recording,
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = FakeNaviRepository(),
            userRepository = FakeUserRepository(),
            consentRepository = FakeConsentRepository(),
            tripRepository = trips,
            placeRepository = places,
            endTrip = { uuid -> trips.endTrip(uuid) },
        )
        TripsViewModel::class.java -> TripsViewModel(tripRepository = trips)
        TripDetailViewModel::class.java -> TripDetailViewModel(
            tripRepository = trips,
            nameSuggester = { it },
        )
        PlaceInputViewModel::class.java -> PlaceInputViewModel(
            tripRepository = trips,
            placeRepository = places,
            codeRepository = FakeCodeRepository(),
            naviRepository = FakeNaviRepository(),
        )
        TripSetupViewModel::class.java -> TripSetupViewModel(
            tripRepository = trips,
            codeRepository = FakeCodeRepository(),
            nameSuggester = { it },
            timeSource = time,
        )
        else -> error("테스트 그래프에 없는 ViewModel: ${modelClass.simpleName}")
    }
}
