package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.recordingRepositoryOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** #69 스펙 3.2 · T3 ~ T7 — 화면 버튼과 같은 RecordingRepository(실제 구현 + 기기 안 대역). */
class GleoTripControlTest {
    private val time = FakeTimeSource(now = 1_700_000_000_000L)

    private fun recordingWith(trip: CurrentTrip?) = recordingRepositoryOf(time, FakeRecordingLocalDataSource(trip))
    private fun recordingTrip() = CurrentTrip.start(time.now).copy(tripNo = 1L)

    @Test
    fun `T3 기록 중 PAUSE 는 일시정지`() = runTest {
        val recording = recordingWith(recordingTrip())
        val outcome = GleoTripControl(recording).control("PAUSE")

        assertEquals(GleoCommandPhase.TRIP_PAUSED, outcome.phase)
        assertEquals(GleoStatus.SUCCESS, outcome.status)
        assertEquals(TripStatus.PAUSED, recording.progress.first().status)
    }

    @Test
    fun `T4 PAUSE 를 두 번 말해도 다시 기록 중으로 뒤집히지 않음`() = runTest {
        val recording = recordingWith(recordingTrip())
        val control = GleoTripControl(recording)
        control.control("PAUSE")
        val second = control.control("PAUSE")

        assertEquals(GleoCommandPhase.ALREADY_PAUSED, second.phase)
        assertEquals(AppConfig.GLEO_ALREADY_PAUSED_MESSAGE, second.message)
        assertEquals(TripStatus.PAUSED, recording.progress.first().status)
    }

    @Test
    fun `T5 일시정지 중 RESUME 은 다시 기록 · 기록 중 RESUME 은 그대로`() = runTest {
        val recording = recordingWith(recordingTrip())
        val control = GleoTripControl(recording)
        assertEquals(GleoCommandPhase.ALREADY_RUNNING, control.control("RESUME").phase)
        control.control("PAUSE")

        val resumed = control.control("RESUME")
        assertEquals(GleoCommandPhase.TRIP_RESUMED, resumed.phase)
        assertEquals(TripStatus.RECORDING, recording.progress.first().status)
    }

    @Test
    fun `T6 END 는 종료 확인 요청만 · 여행 상태는 그대로`() = runTest {
        val recording = recordingWith(recordingTrip())
        val outcome = GleoTripControl(recording).control("END")

        assertEquals(GleoCommandPhase.END_CONFIRM, outcome.phase)
        assertEquals(GleoScreenRequest.ConfirmEndTrip, outcome.screen)
        assertEquals(TripStatus.RECORDING, recording.progress.first().status)
    }

    @Test
    fun `T7 진행 중 여행이 없으면 세 동작 모두 실패 · 아무것도 안 바뀜`() = runTest {
        for (trip in listOf(null, recordingTrip().copy(status = TripStatus.COMPLETED))) {
            val recording = recordingWith(trip)
            for (op in listOf("PAUSE", "RESUME", "END")) {
                val outcome = GleoTripControl(recording).control(op)
                assertEquals(GleoCommandPhase.NO_TRIP, outcome.phase)
                assertEquals(GleoStatus.FAIL, outcome.status)
                assertEquals(null, outcome.screen)
            }
        }
    }
}
