package com.example.travelcrumbs.ui.home

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.SdkStatus
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T4 기어가 P 면 uiState 는 정차`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            FakeDrivingSignalRepository(DrivingState.PARKED),
            FakeGleoActionRepository(),
            FakeNaviRepository(),
        )

        subscribe(viewModel)

        assertEquals(DrivingState.PARKED, viewModel.uiState.value.drivingState)
    }

    @Test
    fun `T5 내비 조회가 성공하면 Ready`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            FakeDrivingSignalRepository(DrivingState.PARKED),
            FakeGleoActionRepository(),
            FakeNaviRepository(
                result = Result.success(
                    NaviRouteState(
                        routing = true,
                        mode = "EV",
                        routeOption = "FAST",
                        termsAgreed = true,
                    ),
                ),
            ),
        )

        subscribe(viewModel)

        val state = viewModel.uiState.value.navi
        assertTrue(state is SdkStatus.Ready)
        assertEquals(true, (state as SdkStatus.Ready).value.routing)
    }

    @Test
    fun `T9 Gleo Action 으로 열리면 uiState 에 남음`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            FakeDrivingSignalRepository(DrivingState.PARKED),
            FakeGleoActionRepository(opened = true),
            FakeNaviRepository(),
        )

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.openedByGleoAction)
    }

    /** uiState 는 WhileSubscribed 라 구독자가 있어야 흐름. */
    private fun TestScope.subscribe(viewModel: HomeViewModel) {
        backgroundScope.launch { viewModel.uiState.collect { } }
        testScheduler.advanceUntilIdle()
    }
}
