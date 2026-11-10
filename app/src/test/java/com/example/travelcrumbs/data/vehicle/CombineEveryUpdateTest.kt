package com.example.travelcrumbs.data.vehicle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
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

/** 화면용 흐름은 한꺼번에 온 값의 마지막만(#66 리뷰 심사 4) — 얼림 해제 때 D → P → D 의 중간 P 로 정차 화면이 깜빡 열리지 않음. */
class ScreenDrivingConflateTest {

    @Test
    fun `심사 4 공유 흐름에 D P D 가 한꺼번에 — 화면용 conflate 구독은 D 만 · 기록기용은 셋 다`() = runTest {
        val shared = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 64)
        shared.tryEmit("P")
        val screen = mutableListOf<String>()
        val recorder = mutableListOf<String>()
        val a = launch { shared.conflate().distinctUntilChanged().toList(screen) }
        val b = launch { shared.toList(recorder) }
        runCurrent()
        shared.tryEmit("D")
        shared.tryEmit("P")
        shared.tryEmit("D")
        runCurrent()
        assertEquals(listOf("P", "D"), screen)
        assertEquals(listOf("P", "D", "P", "D"), recorder)
        a.cancel()
        b.cancel()
    }
}
