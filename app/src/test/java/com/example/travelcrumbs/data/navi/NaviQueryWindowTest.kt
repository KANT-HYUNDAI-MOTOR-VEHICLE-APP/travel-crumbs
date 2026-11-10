package com.example.travelcrumbs.data.navi

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 상태 조회 중 온 onError 를 요청 결과로 쓰지 않는 창(리뷰 P3). */
class NaviQueryWindowTest {

    @Test
    fun `P3 조회 중엔 열림 · 끝나면(예외로 끝나도) 닫힘 — 겹친 조회는 마지막이 끝날 때 닫힘`() = runBlocking {
        val window = NaviQueryWindow()
        assertFalse(window.open)

        window.around {
            assertTrue(window.open)
            window.around { assertTrue(window.open) }
            assertTrue("바깥 조회가 아직 진행 중", window.open)
        }
        assertFalse(window.open)

        runCatching { window.around<Unit> { error("CAUSE_SEARCH_ROUTE") } }
        assertFalse("예외로 끝나도 닫힘", window.open)
    }
}
