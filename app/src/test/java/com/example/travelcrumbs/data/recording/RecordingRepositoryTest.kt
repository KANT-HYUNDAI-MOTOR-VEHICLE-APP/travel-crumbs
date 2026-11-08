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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingRepositoryTest {

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS

    /** 가상 시간을 벽시계로 씀 — delay 와 시계가 같이 감. */
    private fun TestScope.clock() = TimeSource { testScheduler.currentTime }

    @Test
    fun `D2 V11 저장된 상태가 없으면 기록 중 0분으로 시작하고 저장`() = runTest {
        val local = FakeRecordingLocalDataSource()
        val repository = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(TripStatus.RECORDING, 0, tripName = null, placeCount = 0), repository.progress.first())
        assertEquals(TripStatus.RECORDING, local.current?.status)
    }

    @Test
    fun `V20 기록 중이면 분이 바뀔 때마다 새 값 — 65분`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(), clock())
        val seen = mutableListOf<RecordingProgress>()
        backgroundScope.launch { repository.progress.collect { seen += it } }

        testScheduler.advanceTimeBy(65 * minute + 1)

        assertEquals(66, seen.size)
        assertEquals(65L, seen.last().elapsedMinutes)
    }

    @Test
    fun `V15 V19 종료하면 COMPLETED · 경과 멈춤 · 2시간 지나도 그대로`() = runTest {
        val local = FakeRecordingLocalDataSource()
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
    fun `V18 한쪽에서 종료하면 다른 구독자도 같은 상태 — 단일 출처`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(), clock())
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
        val local = FakeRecordingLocalDataSource()
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
}
