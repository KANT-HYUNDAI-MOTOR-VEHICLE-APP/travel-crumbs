package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.ElapsedTime
import com.example.travelcrumbs.domain.model.RecordingSession
import com.example.travelcrumbs.domain.model.RecordingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingSessionTest {

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS

    @Test
    fun `T20 경과 나눔 — 0분 59분 65분`() {
        assertEquals(ElapsedTime(hours = 0, minutes = 0), ElapsedTime.fromMinutes(0))
        assertEquals(ElapsedTime(hours = 0, minutes = 59), ElapsedTime.fromMinutes(59))
        assertEquals(ElapsedTime(hours = 1, minutes = 5), ElapsedTime.fromMinutes(65))
        assertEquals(true, ElapsedTime.fromMinutes(59).underAnHour)
        assertEquals(false, ElapsedTime.fromMinutes(60).underAnHour)
    }

    @Test
    fun `T17 일시정지하면 경과가 멈춤`() {
        val started = RecordingSession.start(nowMillis = 0)
        val paused = started.toggled(nowMillis = 10 * minute)

        assertEquals(RecordingStatus.PAUSED, paused.status)
        assertEquals(10 * minute, paused.elapsedMillis(nowMillis = 30 * minute))
    }

    @Test
    fun `T18 재개하면 경과가 이어서 늘어남`() {
        val paused = RecordingSession.start(0).toggled(10 * minute)
        val resumed = paused.toggled(nowMillis = 30 * minute)

        assertEquals(RecordingStatus.RECORDING, resumed.status)
        assertEquals(15 * minute, resumed.elapsedMillis(nowMillis = 35 * minute))
    }

    @Test
    fun `D2 첫 실행은 기록 중 0분`() {
        val started = RecordingSession.start(nowMillis = 1_000)

        assertEquals(RecordingStatus.RECORDING, started.status)
        assertEquals(0L, started.elapsedMillis(nowMillis = 1_000))
    }

    @Test
    fun `N1 시계가 거꾸로 가도 경과는 음수가 아님`() {
        assertEquals(0L, RecordingSession.start(nowMillis = 5 * minute).elapsedMillis(nowMillis = 0))
    }
}
