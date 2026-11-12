package com.example.travelcrumbs.data.navi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 상시 리스너 칸(#66 3.8) — 연결 · 재연결 때 1개 · 중복 없음 · 세션 끝에 뗌. */
class NaviListenerSlotTest {

    @Test
    fun `T50 붙음 · 또 붙음 신호 · 끊김 · 다시 붙음 — 등록은 늘 1개`() {
        var listeners = 0
        val slot = NaviListenerSlot(add = { listeners++ }, remove = { listeners-- })
        slot.attach()
        slot.attach()
        assertEquals(1, listeners)
        slot.detach()
        slot.detach()
        assertEquals(0, listeners)
        slot.attach()
        assertEquals(1, listeners)
        assertTrue(slot.isAttached)
    }

    @Test
    fun `T50 붙이기 실패 — 붙지 않은 것으로 · 다음 붙음에 다시 시도`() {
        var fail = true
        var listeners = 0
        val slot = NaviListenerSlot(add = { if (fail) error("x") else listeners++ }, remove = { listeners-- })
        slot.attach()
        assertFalse(slot.isAttached)
        fail = false
        slot.attach()
        assertEquals(1, listeners)
    }
}
