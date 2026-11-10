package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.ElapsedTime
import com.example.travelcrumbs.domain.model.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CurrentTripTest {

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS

    @Test
    fun `V20 경과 나눔 — 0분 59분 65분`() {
        assertEquals(ElapsedTime(hours = 0, minutes = 0), ElapsedTime.fromMinutes(0))
        assertEquals(ElapsedTime(hours = 0, minutes = 59), ElapsedTime.fromMinutes(59))
        assertEquals(ElapsedTime(hours = 1, minutes = 5), ElapsedTime.fromMinutes(65))
        assertEquals(true, ElapsedTime.fromMinutes(59).underAnHour)
        assertEquals(false, ElapsedTime.fromMinutes(60).underAnHour)
    }

    @Test
    fun `V20 일시정지 없이 기록 중 경과 = 지금 − 시작 시각(앱이 꺼져 있던 시간 포함)`() {
        val trip = CurrentTrip.start(nowMillis = 0)

        assertEquals(65 * minute, trip.elapsedMillis(nowMillis = 65 * minute))
        assertEquals(59 * minute, trip.elapsedMillis(nowMillis = 59 * minute))
        assertEquals(0L, trip.elapsedMillis(nowMillis = 0))
    }

    @Test
    fun `V15 V19 종료하면 COMPLETED · 종료 시각 · 경과는 종료 시각 기준으로 멈춤`() {
        val ended = CurrentTrip.start(nowMillis = 0).ended(nowMillis = 10 * minute)

        assertEquals(TripStatus.COMPLETED, ended.status)
        assertEquals(10 * minute, ended.endAtMillis)
        assertEquals(10 * minute, ended.elapsedMillis(nowMillis = 10 * minute + 120 * minute))
    }

    @Test
    fun `V21 COMPLETED · CANCELED 에 종료가 다시 와도 바뀌지 않음`() {
        val completed = CurrentTrip.start(0).ended(10 * minute)
        assertSame(completed, completed.ended(nowMillis = 50 * minute))

        val canceled = CurrentTrip.start(0).copy(status = TripStatus.CANCELED, endAtMillis = 3 * minute)
        assertSame(canceled, canceled.ended(nowMillis = 50 * minute))
    }

    @Test
    fun `D2 V34 새 여행은 기록 중 · 0분 · 여행명 없음 · 장소 0 · 종료 시각 없음`() {
        val started = CurrentTrip.start(nowMillis = 1_000)

        assertEquals(TripStatus.RECORDING, started.status)
        assertEquals(0L, started.elapsedMillis(nowMillis = 1_000))
        assertNull(started.tripName)
        assertEquals(0, started.placeCount)
        assertNull(started.endAtMillis)
    }

    @Test
    fun `V60 여행 상태 값은 표준 v0_4 TRIP_STATUS 다섯 · 진행 중 여행은 RECORDING PAUSED`() {
        assertEquals(
            listOf("RECORDING", "PAUSED", "COMPLETED", "INTERRUPTED", "CANCELED"),
            TripStatus.entries.map { it.name },
        )
        assertEquals(
            listOf(TripStatus.RECORDING, TripStatus.PAUSED),
            TripStatus.entries.filter { it.inProgress },
        )
    }

    @Test
    fun `V48 V49 일시정지하면 기록 시간 멈춤 · 재개하면 멈춘 값에서 이어짐`() {
        val paused = CurrentTrip.start(nowMillis = 0).pauseToggled(nowMillis = 12 * minute)

        assertEquals(TripStatus.PAUSED, paused.status)
        assertEquals(12 * minute, paused.elapsedMillis(nowMillis = 40 * minute))

        val resumed = paused.pauseToggled(nowMillis = 40 * minute)
        assertEquals(TripStatus.RECORDING, resumed.status)
        assertEquals(12 * minute, resumed.elapsedMillis(nowMillis = 40 * minute))
        assertEquals(15 * minute, resumed.elapsedMillis(nowMillis = 43 * minute))
    }

    @Test
    fun `V50 기록 20분 · 일시정지 30분 · 재개 10분 — 일시정지 동안 20분 · 끝에 30분(전체 경과 60분 아님)`() {
        val paused = CurrentTrip.start(0).pauseToggled(20 * minute)
        assertEquals(20 * minute, paused.elapsedMillis(50 * minute))

        val resumed = paused.pauseToggled(50 * minute)
        assertEquals(30 * minute, resumed.elapsedMillis(60 * minute))
    }

    @Test
    fun `V54 PAUSED 에서 종료 — COMPLETED · 기록 시간은 일시정지한 순간 값 · 종료 시각은 누른 시각`() {
        val ended = CurrentTrip.start(0).pauseToggled(25 * minute).ended(30 * minute)

        assertEquals(TripStatus.COMPLETED, ended.status)
        assertEquals(30 * minute, ended.endAtMillis)
        assertEquals(25 * minute, ended.elapsedMillis(nowMillis = 200 * minute))
    }

    @Test
    fun `V64 COMPLETED · CANCELED 에 일시정지 · 재개가 와도 바뀌지 않음`() {
        val completed = CurrentTrip.start(0).ended(10 * minute)
        assertSame(completed, completed.pauseToggled(nowMillis = 50 * minute))

        val canceled = CurrentTrip.start(0).copy(status = TripStatus.CANCELED, endAtMillis = 3 * minute)
        assertSame(canceled, canceled.pauseToggled(nowMillis = 50 * minute))
    }

    @Test
    fun `V20 시계가 거꾸로 가도 경과는 음수가 아님`() {
        assertEquals(0L, CurrentTrip.start(nowMillis = 5 * minute).elapsedMillis(nowMillis = 0))
    }
}
