package com.example.travelcrumbs.ui.gleo

import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GleoCommandViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `T24 앱 틀은 Repository가 관측한 준비 처리 결과 상태만 받음`() = runTest(dispatcher) {
        val repository = FakeGleoActionRepository()
        val vm = GleoCommandViewModel(repository)
        backgroundScope.launch { vm.uiState.collect { } }
        advanceUntilIdle()
        listOf(GleoCommandPhase.LOADING_TRIP, GleoCommandPhase.PREPARING, GleoCommandPhase.PROCESSING, GleoCommandPhase.SAVED).forEach { phase ->
            repository.commandState.value = GleoCommandState(1L, phase)
            advanceUntilIdle()
            assertEquals(phase, vm.uiState.value.command.phase)
        }
    }

    @Test
    fun `T25 결과 표시 소비는 Intent를 다시 처리하지 않음`() = runTest(dispatcher) {
        val repository = FakeGleoActionRepository()
        val vm = GleoCommandViewModel(repository)
        backgroundScope.launch { vm.uiState.collect { } }
        advanceUntilIdle()
        repository.commandState.value = GleoCommandState(7L, GleoCommandPhase.SAVED)
        advanceUntilIdle()
        vm.onNoticeShown(7L)
        advanceUntilIdle()
        assertEquals(GleoCommandPhase.IDLE, vm.uiState.value.command.phase)
        assertEquals(0, repository.handledCount)
    }
}
