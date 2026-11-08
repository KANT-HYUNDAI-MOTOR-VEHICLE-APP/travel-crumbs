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
import com.example.travelcrumbs.ui.shell.AppShellViewModel

/**
 * Compose UI 테스트용 조립 — Hilt 없이 fake 저장소로 화면의 ViewModel 을 만듦.
 * 화면의 `viewModel()` 은 [owner] 의 기본 팩토리로 이 ViewModel 들을 받음.
 */
class ShellTestGraph(initialDriving: DrivingState = DrivingState.PARKED) {
    val time = FakeTimeSource(now = 1_000L)
    val driving = FakeDrivingSignalRepository(initialDriving)
    val recording = recordingRepositoryOf(time)
    val inputs = unfinishedInputRepositoryOf(time)
    val savedState = SavedStateHandle()
    val drivingSavedState = SavedStateHandle()

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
        )
        HomeViewModel::class.java -> HomeViewModel(
            recordingRepository = recording,
            recordPlace = FakeRecordPlaceUseCase(),
            drivingSignalRepository = driving,
            gleoActionRepository = FakeGleoActionRepository(),
            naviRepository = FakeNaviRepository(),
        )
        else -> error("테스트 그래프에 없는 ViewModel: ${modelClass.simpleName}")
    }
}
