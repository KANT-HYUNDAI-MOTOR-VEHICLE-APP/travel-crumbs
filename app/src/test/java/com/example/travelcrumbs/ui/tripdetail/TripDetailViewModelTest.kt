package com.example.travelcrumbs.ui.tripdetail

import com.example.travelcrumbs.testing.FakeTripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest {

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
    fun `제목 수정 — 추천 자리에서 받은 이름으로 입력칸이 열리고 저장하면 바뀜`() = runTest(dispatcher) {
        val trips = FakeTripRepository()
        val vm = TripDetailViewModel(trips) { "강릉 시장 투어" }
        vm.load("uuid-1")
        runCurrent()
        assertEquals("2026.11.07 여행", vm.uiState.value.detail?.tripName)

        vm.onEditTitle()
        runCurrent()
        assertTrue(vm.uiState.value.editingTitle)
        assertEquals("강릉 시장 투어", vm.uiState.value.titleInput)

        vm.onSaveTitle()
        runCurrent()

        assertEquals(listOf("강릉 시장 투어"), trips.renamed)
        assertEquals("강릉 시장 투어", vm.uiState.value.detail?.tripName)
        assertFalse(vm.uiState.value.editingTitle)
    }
}
