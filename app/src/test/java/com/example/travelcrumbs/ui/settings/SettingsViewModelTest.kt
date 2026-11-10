package com.example.travelcrumbs.ui.settings

import androidx.lifecycle.SavedStateHandle
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.completedTrip
import com.example.travelcrumbs.ui.components.UiMessage
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** C-03 설정(#51 U2) — 닉네임 수정 · 동의 상태 · 철회. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val time = FakeTimeSource(now = 1_794_106_800_000L) // 2026-11-08 12:00 KST
    private val users = FakeCurrentUserRepository(nicknameNm = "여행자", time = time)
    private val trips = FakeTripRepository(
        listOf(completedTrip(tripNo = 1, visibility = Visibility.ANONYMOUS_PUBLIC), completedTrip(tripNo = 2, visibility = Visibility.ANONYMOUS_PUBLIC)),
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): SettingsViewModel {
        val zoned = object : com.example.travelcrumbs.domain.TimeSource {
            override fun nowMillis() = time.now
            override fun zoneId(): ZoneId = ZoneId.of("Asia/Seoul")
        }
        val vm = SettingsViewModel(users, trips, zoned, SavedStateHandle(), com.example.travelcrumbs.testing.TripOnlyCleanup(trips))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun `U2-3 닉네임 수정 — 팝업에 지금 이름 · 저장하면 바뀌고 닫힘 · 빈 값은 저장 안 됨`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals("여행자", vm.uiState.value.nicknameNm)

        vm.onEditNickname()
        runCurrent()
        assertEquals("여행자", vm.uiState.value.nicknameDraft)

        vm.onNicknameChange("   ")
        vm.onSaveNickname()
        runCurrent()
        assertEquals("   ", vm.uiState.value.nicknameDraft)

        vm.onNicknameChange("민지네")
        vm.onSaveNickname()
        runCurrent()
        assertEquals("민지네", vm.uiState.value.nicknameNm)
        assertNull(vm.uiState.value.nicknameDraft)
        assertEquals(UiMessage.NICKNAME_SAVED, vm.uiState.value.message)
    }

    @Test
    fun `U2-4 추천 공개 철회 — 철회 행 · 내 공개 여행 모두 비공개 · 동의 안 함 표시`() = runTest(dispatcher) {
        users.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
        val vm = viewModel()
        assertTrue(vm.uiState.value.recoAgreed)

        vm.onWithdrawReco()
        runCurrent()

        assertFalse(vm.uiState.value.recoAgreed)
        assertEquals(ConsentType.RECO_PUBLIC to false, users.history.last())
        assertTrue(trips.trips.first().all { it.visibility == Visibility.PRIVATE })
        assertEquals(UiMessage.RECO_WITHDRAWN, vm.uiState.value.message)
    }

    @Test
    fun `U2-5 위치정보 수집 — 동의한 날짜 표시 · 철회하면 동의 안 함`() = runTest(dispatcher) {
        users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        val vm = viewModel()
        assertEquals(ConsentDate(2026, 11, 8), vm.uiState.value.locationAgreedOn)

        vm.onWithdrawLocation()
        runCurrent()
        assertNull(vm.uiState.value.locationAgreedOn)
        // P1 — 철회하면 저장된 좌표 · 주소도 지움
        assertTrue(trips.trips.first().flatMap { it.places }.all { it.lat == null && it.lng == null && it.addr == null })
    }

    @Test
    fun `R1-2 내 기록 모두 지우기 — 확인 팝업 · 지우면 여행 없음 · 동의 이력은 남음`() = runTest(dispatcher) {
        users.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
        val vm = viewModel()
        vm.onAskClearAll()
        runCurrent()
        assertTrue(vm.uiState.value.clearAllAsk)
        vm.onDismissClearAll()
        runCurrent()
        assertFalse(vm.uiState.value.clearAllAsk)
        assertEquals(2, trips.trips.first().size)

        vm.onAskClearAll()
        vm.onConfirmClearAll()
        runCurrent()
        assertTrue(trips.trips.first().isEmpty())
        assertEquals(UiMessage.DATA_CLEARED, vm.uiState.value.message)
        assertTrue(vm.uiState.value.recoAgreed)
    }
}
