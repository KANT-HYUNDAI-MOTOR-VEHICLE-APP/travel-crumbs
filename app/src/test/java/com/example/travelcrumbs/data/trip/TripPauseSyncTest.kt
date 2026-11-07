package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.recordingRepositoryOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    }
}
