package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 자동 기록 판정 순수 계산(#66 3.4 · 3.6) — 기한 · 개수 · 반경 · 이 앱 요청 창. */
class AutoRecordRulesTest {

    private fun p(arrivedAt: Long, parked: Boolean = false, lat: Double = 37.5) =
        PendingArrival(1L, "n", "a", lat, 127.0, arrivedAt, parked)

    @Test
    fun `T5 PARKED 아직 대기 — 도착 뒤 10분이면 폐기 · 1밀리초 전이면 유지 · 앱 새로 시작은 10분을 안 봄`() {
        val list = listOf(p(0L))
        assertTrue(AutoRecordRules.purge(list, AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS, applyParkWait = true).isEmpty())
        assertEquals(1, AutoRecordRules.purge(list, AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS - 1, applyParkWait = true).size)
        assertEquals(1, AutoRecordRules.purge(list, AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS * 6, applyParkWait = false).size)
        assertEquals(1, AutoRecordRules.purge(listOf(p(0L, parked = true)), AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS * 6, applyParkWait = true).size)
    }

    @Test
    fun `T42 T43 24시간 절대 만료 — PARKED 봄이어도 · 앱 새로 시작이어도 · 경계 포함`() {
        val max = AppConfig.AUTO_RECORD_PENDING_MAX_AGE_MILLIS
        assertTrue(AutoRecordRules.purge(listOf(p(0L, parked = true)), max, applyParkWait = false).isEmpty())
        assertEquals(1, AutoRecordRules.purge(listOf(p(0L, parked = true)), max - 1, applyParkWait = false).size)
    }

    @Test
    fun `T26 대기 5건에 6번째 — 가장 오래된 도착 버림 · 5건 · 도착 순`() {
        val five = (1L..5L).map { p(it) }
        val after = AutoRecordRules.add(five, p(6L))
        assertEquals(AppConfig.AUTO_RECORD_PENDING_MAX_COUNT, after.size)
        assertEquals(listOf(2L, 3L, 4L, 5L, 6L), after.map { it.arrivedAt })
    }

    @Test
    fun `T11 T44 T45 새 시작 — PARKED 아직 · 반경 밖만 폐기 · 반경 안(재탐색) · PARKED 봄은 유지`() {
        val far = NaviDestination("c", null, 37.51, 127.0)
        val near = NaviDestination("a", null, 37.50027, 127.0)
        assertTrue(AutoRecordRules.dropPassed(listOf(p(0L)), far).isEmpty())
        assertEquals(1, AutoRecordRules.dropPassed(listOf(p(0L)), near).size)
        assertEquals(1, AutoRecordRules.dropPassed(listOf(p(0L, parked = true)), far).size)
    }

    @Test
    fun `T24 T25 확정 판정 — 기존 장소 · 앞선 확정과 100 m 안이면 빼고 먼저 도착한 것만`() {
        val a = p(1L)
        val aNear = p(2L, lat = 37.50072)
        val far = p(3L, lat = 37.51)
        assertEquals(listOf(a, far), AutoRecordRules.toRecord(listOf(far, aNear, a), emptyList()))
        assertEquals(listOf(far), AutoRecordRules.toRecord(listOf(a, far), listOf(37.5 to 127.0)))
    }

    @Test
    fun `T8 T48 T49 수동 직전 — 수동 위치 반경 안은 폐기 · 위치 없으면 전부 후보`() {
        val a = p(1L)
        val c = p(2L, lat = 37.51)
        assertEquals(listOf(c), AutoRecordRules.beforeManual(listOf(a, c), 37.50045, 127.0))
        assertEquals(listOf(a, c), AutoRecordRules.beforeManual(listOf(a, c), null, null))
    }

    @Test
    fun `T23 좌표 쓸 수 없음 — (0, 0) · 범위 밖`() {
        assertFalse(AutoRecordRules.usable(NaviDestination("x", null, 0.0, 0.0)))
        assertFalse(AutoRecordRules.usable(NaviDestination("x", null, 91.0, 127.0)))
        assertTrue(AutoRecordRules.usable(NaviDestination("x", null, 37.5, 127.0)))
    }

    @Test
    fun `T54 T55 이 앱 요청 창 — 시작됨 20초 · 정확히 60초는 앱 요청 · 60초 + 1밀리초는 직접`() {
        val x = Place("x", "X", "주소", 37.5, 127.0)
        val d = NaviDestination("X", null, 37.5, 127.0)
        val w = AppConfig.AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS
        assertTrue(AutoRecordRules.isAppRequest(d, null, x, 0L, 20_000L))
        assertTrue(AutoRecordRules.isAppRequest(d, null, x, 0L, w))
        assertFalse(AutoRecordRules.isAppRequest(d, null, x, 0L, w + 1))
        assertTrue("내부 칸은 시각과 무관", AutoRecordRules.isAppRequest(d, x, null, null, 10 * w))
        assertFalse("이름이 다르면 아님", AutoRecordRules.isAppRequest(d.copy(name = "Y"), x, x, 0L, 0L))
    }
}
