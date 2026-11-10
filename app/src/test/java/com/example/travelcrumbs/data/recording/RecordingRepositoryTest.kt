package com.example.travelcrumbs.data.recording

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.example.travelcrumbs.domain.model.NewTrip
import com.example.travelcrumbs.domain.model.CompanionType
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingRepositoryTest {

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS

    /** 가상 시간을 벽시계로 씀 — delay 와 시계가 같이 감. */
    private fun TestScope.clock() = TimeSource { testScheduler.currentTime }

    @Test
    fun `U1-1 v6 저장된 여행이 없으면 만들지 않음 — 여행 없음 · 일시정지 · 종료도 아무 일 없음 · 여행 시작이면 기록 중 0분`() = runTest {
        val local = FakeRecordingLocalDataSource()
        val repository = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress.NONE, repository.progress.first())
        repository.togglePause()
        assertEquals(null, repository.endTrip())
        assertEquals(null, local.current)

        assertTrue(repository.startTrip(NewTrip("속초 가족여행", setOf(CompanionType.CHILD), "속초 중앙시장")))
        assertEquals(
            RecordingProgress(TripStatus.RECORDING, 0, tripName = "속초 가족여행", placeCount = 0, destPlaceNm = "속초 중앙시장"),
            repository.progress.first(),
        )
        assertEquals(setOf(CompanionType.CHILD), local.current?.companions)
        assertFalse("진행 중이면 새로 시작 안 함", repository.startTrip(NewTrip(null, emptySet(), null)))
    }

    @Test
    fun `V20 기록 중이면 분이 바뀔 때마다 새 값 — 65분`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(CurrentTrip.start(0)), clock())
        val seen = mutableListOf<RecordingProgress>()
        backgroundScope.launch { repository.progress.collect { seen += it } }

        testScheduler.advanceTimeBy(65 * minute + 1)

        assertEquals(66, seen.size)
        assertEquals(65L, seen.last().elapsedMinutes)
    }

    @Test
    fun `V15 V19 종료하면 COMPLETED · 경과 멈춤 · 2시간 지나도 그대로`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        val repository = DefaultRecordingRepository(local, clock())
        var latest: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { latest = it } }

        testScheduler.advanceTimeBy(10 * minute + 1)
        repository.endTrip()
        testScheduler.advanceTimeBy(120 * minute)

        assertEquals(RecordingProgress(TripStatus.COMPLETED, 10), latest)
        assertEquals(10 * minute + 1, local.current?.endAtMillis)
    }

    @Test
    fun `V21 이미 COMPLETED 인 여행에 종료가 다시 와도 상태 · 종료 시각 그대로`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0).ended(5 * minute))
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(30 * minute)

        repository.endTrip()

        assertEquals(TripStatus.COMPLETED, local.current?.status)
        assertEquals(5 * minute, local.current?.endAtMillis)
    }

    @Test
    fun `R32 종료 시각 — 진행 중(기록 중 · 일시정지)이면 이번 종료 시각 · 이미 끝난 여행이면 null`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0).pauseToggled(2 * minute))
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(7 * minute)

        assertEquals(7 * minute, repository.endTrip())
        testScheduler.advanceTimeBy(3 * minute)
        assertNull(repository.endTrip())
        assertEquals(7 * minute, local.current?.endAtMillis)
    }

    @Test
    fun `V18 한쪽에서 종료하면 다른 구독자도 같은 상태 — 단일 출처`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(CurrentTrip.start(0)), clock())
        var home: RecordingProgress? = null
        var driving: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { home = it } }
        backgroundScope.launch { repository.progress.collect { driving = it } }
        testScheduler.runCurrent()

        repository.endTrip()
        testScheduler.runCurrent()

        assertEquals(TripStatus.COMPLETED, home?.status)
        assertEquals(home, driving)
    }

    @Test
    fun `V22 COMPLETED 뒤 앱을 다시 띄워도 완료 · 경과 고정`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        DefaultRecordingRepository(local, clock()).apply {
            progress.first()
            testScheduler.advanceTimeBy(3 * minute)
            endTrip()
        }
        testScheduler.advanceTimeBy(60 * minute)

        val relaunched = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(TripStatus.COMPLETED, 3), relaunched.progress.first())
    }

    @Test
    fun `V34 COMPLETED 뒤 새 여행 시작 — 기록 중 · 0분 · 종료 시각 없음`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0).ended(5 * minute))
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(30 * minute)

        repository.startNewTrip()

        assertEquals(RecordingProgress(TripStatus.RECORDING, 0), repository.progress.first())
        assertEquals(30 * minute, local.current?.startAtMillis)
        assertNull(local.current?.endAtMillis)
    }

    @Test
    fun `V35 기록 중에는 새 여행 시작이 아무것도 바꾸지 않음`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(7 * minute)

        repository.startNewTrip()

        assertEquals(CurrentTrip.start(0), local.current)
        assertEquals(RecordingProgress(TripStatus.RECORDING, 7), repository.progress.first())
    }

    @Test
    fun `V37 새 여행 시작 뒤 10분 · 앱 다시 실행 — 기록 중 10분(새 시작 시각 기준)`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0).ended(5 * minute))
        testScheduler.advanceTimeBy(30 * minute)
        DefaultRecordingRepository(local, clock()).startNewTrip()
        testScheduler.advanceTimeBy(10 * minute)

        val relaunched = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(TripStatus.RECORDING, 10), relaunched.progress.first())
    }

    @Test
    fun `V48 V49 V50 일시정지 동안 경과 멈춤 · 재개 뒤 멈춘 값에서 이어짐`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(CurrentTrip.start(0)), clock())
        var latest: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { latest = it } }

        testScheduler.advanceTimeBy(20 * minute + 1)
        repository.togglePause()
        testScheduler.advanceTimeBy(30 * minute)
        assertEquals(RecordingProgress(TripStatus.PAUSED, 20), latest)

        repository.togglePause()
        testScheduler.advanceTimeBy(10 * minute)
        assertEquals(RecordingProgress(TripStatus.RECORDING, 30), latest)
    }

    @Test
    fun `V56 주행 화면에서 일시정지하면 홈 구독자도 일시정지 — 단일 출처`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(CurrentTrip.start(0)), clock())
        var home: RecordingProgress? = null
        var driving: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { home = it } }
        backgroundScope.launch { repository.progress.collect { driving = it } }
        testScheduler.advanceTimeBy(12 * minute + 1)

        repository.togglePause()
        testScheduler.runCurrent()

        assertEquals(RecordingProgress(TripStatus.PAUSED, 12), home)
        assertEquals(home, driving)
    }

    @Test
    fun `V54 PAUSED 에서 종료 — COMPLETED · 일시정지한 순간 기록 시간`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        val repository = DefaultRecordingRepository(local, clock())
        repository.progress.first()
        testScheduler.advanceTimeBy(25 * minute)
        repository.togglePause()
        testScheduler.advanceTimeBy(5 * minute)

        repository.endTrip()

        assertEquals(RecordingProgress(TripStatus.COMPLETED, 25), repository.progress.first())
        assertEquals(30 * minute, local.current?.endAtMillis)
    }

    @Test
    fun `V64 COMPLETED 에 일시정지 · 재개 요청 — 상태 · 기록 시간 그대로`() = runTest {
        val completed = CurrentTrip.start(0).ended(5 * minute)
        val local = FakeRecordingLocalDataSource(completed)
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(30 * minute)

        repository.togglePause()

        assertEquals(completed, local.current)
    }

    @Test
    fun `V35 PAUSED 에도 새 여행 시작이 아무것도 바꾸지 않음`() = runTest {
        val paused = CurrentTrip.start(0).pauseToggled(7 * minute)
        val local = FakeRecordingLocalDataSource(paused)
        val repository = DefaultRecordingRepository(local, clock())
        testScheduler.advanceTimeBy(20 * minute)

        repository.startNewTrip()

        assertEquals(paused, local.current)
    }

    @Test
    fun `V63 PAUSED 15분 · 앱 종료 · 1시간 뒤 다시 실행 — 일시정지 · 15분(꺼진 동안 안 늘어남)`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        DefaultRecordingRepository(local, clock()).apply {
            progress.first()
            testScheduler.advanceTimeBy(15 * minute)
            togglePause()
        }
        testScheduler.advanceTimeBy(60 * minute)

        val relaunched = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(TripStatus.PAUSED, 15), relaunched.progress.first())
    }

    @Test
    fun `V66 일시정지를 빠르게 두 번 — RECORDING → PAUSED → RECORDING · 오류 없음`() = runTest {
        val local = FakeRecordingLocalDataSource(CurrentTrip.start(0))
        val repository = DefaultRecordingRepository(local, clock())
        repository.progress.first()
        testScheduler.advanceTimeBy(3 * minute)

        repository.togglePause()
        repository.togglePause()

        assertEquals(TripStatus.RECORDING, local.current?.status)
        assertEquals(RecordingProgress(TripStatus.RECORDING, 3), repository.progress.first())
    }
}
