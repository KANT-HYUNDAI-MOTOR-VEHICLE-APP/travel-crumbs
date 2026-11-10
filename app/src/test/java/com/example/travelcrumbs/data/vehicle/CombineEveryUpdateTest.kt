package com.example.travelcrumbs.data.vehicle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 주행 판정 합성이 값을 버리지 않음(#66 실측 M9 — 얼림이 풀릴 때 한꺼번에 온 D → P → D). */
class CombineEveryUpdateTest {

    @Test
    fun `M9 한꺼번에 온 기어 D P D — 셋 다 합성됨 · 둘 다 첫 값 전엔 없음`() = runTest {
        val gear = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val restriction = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val out = mutableListOf<String>()
        val job = launch { combineEveryUpdate(gear, restriction) { g, r -> "$g$r" }.toList(out) }
        runCurrent()
        gear.tryEmit("P")
        runCurrent()
        assertEquals(emptyList<String>(), out)
        restriction.tryEmit("-")
        runCurrent()
        gear.tryEmit("D")
        gear.tryEmit("P")
        gear.tryEmit("D")
        runCurrent()
        assertEquals(listOf("P-", "D-", "P-", "D-"), out)
        job.cancel()
    }
}
