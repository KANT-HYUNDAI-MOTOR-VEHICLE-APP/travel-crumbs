package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.recordingRepositoryOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripPauseSyncTest {

    private val recording = Trip(1L, "uuid-1", "2026.11.07 여행", "RECORDING", listOf("SOLO"))

    @Test
    fun `일시정지 버튼 → 서버 일시정지, 다시 누르면 다시 기록`() = runTest {
        val trips = FakeTripRepository(current = recording)
        val local = recordingRepositoryOf(FakeTimeSource())
        TripPauseSync(local, trips).start(backgroundScope)
        runCurrent()

        local.togglePause()
        runCurrent()
        assertEquals("PAUSED", trips.activeTrip.value?.statusCode)

        local.togglePause()
        runCurrent()
        assertEquals("RECORDING", trips.activeTrip.value?.statusCode)
        assertEquals(listOf("PAUSED", "RECORDING"), trips.pauseCalls)
    }

    @Test
    fun `앱을 다시 켰을 때 서버가 일시정지면 앱도 일시정지로 맞춤`() = runTest {
        val trips = FakeTripRepository(current = recording.copy(statusCode = "PAUSED"))
        val local = recordingRepositoryOf(FakeTimeSource())
        TripPauseSync(local, trips).start(backgroundScope)
        runCurrent()

        assertEquals(RecordingStatus.PAUSED, local.progress.first().status)
        assertEquals(emptyList<String>(), trips.pauseCalls)
        assertEquals("PAUSED", trips.activeTrip.value?.statusCode)
    }

    @Test
    fun `T7 빠른 여행 전환은 이전 여행 초기 대기를 취소하고 현재 여행만 변경 감지`() = runTest {
        val trips = SwitchingTripRepository(recording.copy(statusCode = "PAUSED"))
        val local = PendingRecordingRepository()
        TripPauseSync(local, trips).start(backgroundScope)
        runCurrent()

        // 이전 여행은 로컬 복원을 기다리는 중. 실제 완료 사건으로만 진행하고 임의 delay 없음.
        trips.activeTrip.value = recording.copy(tripUuid = "uuid-2")
        runCurrent()
        local.completeRestore(RecordingStatus.RECORDING)
        runCurrent()

        assertEquals(0, local.toggleCalls)
        assertEquals(emptyList<Pair<String, String>>(), trips.changes)

        local.togglePause()
        runCurrent()
        assertEquals(listOf("uuid-2" to "PAUSED"), trips.changes)
        assertEquals(RecordingStatus.PAUSED, local.progress.first().status)
    }

    /** 로컬 복원 완료를 테스트가 사건으로 전달. 기존 저장소·fixture의 초기값 변경 없음. */
    private class PendingRecordingRepository : RecordingRepository {
        private val state = MutableStateFlow<RecordingProgress?>(null)
        override val progress = state.filterNotNull()
        var toggleCalls = 0
            private set

        fun completeRestore(status: RecordingStatus) {
            state.value = RecordingProgress(status, 0L)
        }

        override suspend fun togglePause() {
            val current = checkNotNull(state.value)
            toggleCalls++
            state.value = current.copy(
                status = if (current.status == RecordingStatus.RECORDING) RecordingStatus.PAUSED else RecordingStatus.RECORDING,
            )
        }
    }

    /** 여행 전환과 어느 여행에 변경 요청을 보냈는지 관측하는 최소 대역. */
    private class SwitchingTripRepository(initial: Trip) : TripRepository by FakeTripRepository() {
        override val activeTrip = MutableStateFlow<Trip?>(initial)
        val changes = mutableListOf<Pair<String, String>>()

        override suspend fun pause(tripUuid: String): Result<Trip> = change(tripUuid, "PAUSED")

        override suspend fun resume(tripUuid: String): Result<Trip> = change(tripUuid, "RECORDING")

        private fun change(tripUuid: String, status: String): Result<Trip> {
            val current = activeTrip.value?.takeIf { it.tripUuid == tripUuid }
                ?: return Result.failure(IllegalStateException("inactive trip"))
            changes += tripUuid to status
            val changed = current.copy(statusCode = status)
            activeTrip.value = changed
            return Result.success(changed)
        }
    }
}
