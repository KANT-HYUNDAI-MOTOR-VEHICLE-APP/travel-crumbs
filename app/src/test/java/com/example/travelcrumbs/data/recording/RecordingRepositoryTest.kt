package com.example.travelcrumbs.data.recording

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingRepositoryTest {

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS

    /** 가상 시간을 벽시계로 씀 — delay 와 시계가 같이 감. */
    private fun TestScope.clock() = TimeSource { testScheduler.currentTime }

    @Test
    fun `D2 저장된 상태가 없으면 기록 중 0분으로 시작하고 저장`() = runTest {
        val local = FakeRecordingLocalDataSource()
        val repository = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(RecordingStatus.RECORDING, 0), repository.progress.first())
        assertEquals(RecordingStatus.RECORDING, local.current?.status)
    }

    @Test
    fun `T20 기록 중이면 분이 바뀔 때마다 새 값 — 65분`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(), clock())
        val seen = mutableListOf<RecordingProgress>()
        backgroundScope.launch { repository.progress.collect { seen += it } }

        testScheduler.advanceTimeBy(65 * minute + 1)

        assertEquals(66, seen.size)
        assertEquals(65L, seen.last().elapsedMinutes)
    }

    @Test
    fun `T17 T18 일시정지 동안 경과가 멈추고 재개 뒤 이어짐`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(), clock())
        var latest: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { latest = it } }

        testScheduler.advanceTimeBy(10 * minute + 1)
        repository.togglePause()
        testScheduler.advanceTimeBy(20 * minute)
        assertEquals(RecordingProgress(RecordingStatus.PAUSED, 10), latest)

        repository.togglePause()
        testScheduler.advanceTimeBy(5 * minute)
        assertEquals(RecordingProgress(RecordingStatus.RECORDING, 15), latest)
    }

    @Test
    fun `T19 한쪽에서 일시정지하면 다른 구독자도 같은 상태`() = runTest {
        val repository = DefaultRecordingRepository(FakeRecordingLocalDataSource(), clock())
        var home: RecordingProgress? = null
        var driving: RecordingProgress? = null
        backgroundScope.launch { repository.progress.collect { home = it } }
        backgroundScope.launch { repository.progress.collect { driving = it } }
        testScheduler.runCurrent()

        repository.togglePause()
        testScheduler.runCurrent()

        assertEquals(RecordingStatus.PAUSED, home?.status)
        assertEquals(RecordingStatus.PAUSED, driving?.status)
    }

    @Test
    fun `T33 앱을 다시 띄워도 저장된 일시정지 상태 그대로`() = runTest {
        val local = FakeRecordingLocalDataSource()
        DefaultRecordingRepository(local, clock()).apply {
            progress.first()
            testScheduler.advanceTimeBy(3 * minute)
            togglePause()
        }

        val relaunched = DefaultRecordingRepository(local, clock())

        assertEquals(RecordingProgress(RecordingStatus.PAUSED, 3), relaunched.progress.first())
    }
}
